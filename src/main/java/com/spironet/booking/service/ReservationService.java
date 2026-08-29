package com.spironet.booking.service;

import com.spironet.booking.dto.reservation.ReservationCreateRequest;
import com.spironet.booking.dto.reservation.ReservationResponse;
import com.spironet.booking.dto.reservation.ReservationUpdateRequest;
import com.spironet.booking.entity.Reservation;
import com.spironet.booking.entity.ReservationStatus;
import com.spironet.booking.entity.Resource;
import com.spironet.booking.entity.User;
import com.spironet.booking.exception.BadRequestException;
import com.spironet.booking.exception.ResourceNotFoundException;
import com.spironet.booking.repository.ReservationRepository;
import com.spironet.booking.repository.UserRepository;
import com.spironet.booking.repository.spec.ReservationSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ResourceService resourceService;

    public Page<ReservationResponse> list(boolean isAdmin, Long currentUserId, ReservationStatus status,
                                           BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Long userFilter = isAdmin ? null : currentUserId;
        Specification<Reservation> spec = Specification.allOf(
                ReservationSpecifications.belongsToUser(userFilter),
                ReservationSpecifications.hasStatus(status),
                ReservationSpecifications.priceGreaterThanOrEqual(minPrice),
                ReservationSpecifications.priceLessThanOrEqual(maxPrice));

        return reservationRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public ReservationResponse getById(Long id, boolean isAdmin, Long currentUserId) {
        Reservation reservation = findEntity(id);
        assertOwnershipOrAdmin(reservation, isAdmin, currentUserId);
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse create(ReservationCreateRequest request, Long currentUserId) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        Resource resource = resourceService.findEntity(request.getResourceId());
        resourceService.ensureBookable(resource);

        assertNoOverlap(resource.getId(), request.getStartTime(), request.getEndTime(), null);

        User user = userRepository.getReferenceById(currentUserId);

        Reservation reservation = Reservation.builder()
                .user(user)
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(ReservationStatus.PENDING)
                .price(calculatePrice(resource.getPricePerHour(), request.getStartTime(), request.getEndTime()))
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse updateFull(Long id, ReservationUpdateRequest request) {
        Reservation reservation = findEntity(id);
        validateTimeRange(request.getStartTime(), request.getEndTime());

        Resource resource = resourceService.findEntity(request.getResourceId());
        assertNoOverlap(resource.getId(), request.getStartTime(), request.getEndTime(), reservation.getId());

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setStatus(request.getStatus());
        reservation.setPrice(calculatePrice(resource.getPricePerHour(), request.getStartTime(), request.getEndTime()));

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse cancel(Long id, boolean isAdmin, Long currentUserId) {
        Reservation reservation = findEntity(id);
        assertOwnershipOrAdmin(reservation, isAdmin, currentUserId);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Reservation is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public void delete(Long id) {
        Reservation reservation = findEntity(id);
        reservationRepository.delete(reservation);
    }

    private Reservation findEntity(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private void assertOwnershipOrAdmin(Reservation reservation, boolean isAdmin, Long currentUserId) {
        if (!isAdmin && !reservation.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this reservation");
        }
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new BadRequestException("Start time must be before end time");
        }
    }

    private void assertNoOverlap(Long resourceId, LocalDateTime start, LocalDateTime end, Long excludeReservationId) {
        List<Reservation> overlapping = reservationRepository.findOverlapping(
                resourceId, start, end, ReservationStatus.CANCELLED, excludeReservationId);
        if (!overlapping.isEmpty()) {
            throw new BadRequestException("Resource is already booked for the requested time range");
        }
    }

    private BigDecimal calculatePrice(BigDecimal pricePerHour, LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
        return pricePerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .resourceId(reservation.getResource().getId())
                .resourceName(reservation.getResource().getName())
                .userId(reservation.getUser().getId())
                .username(reservation.getUser().getUsername())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .status(reservation.getStatus())
                .price(reservation.getPrice())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}
