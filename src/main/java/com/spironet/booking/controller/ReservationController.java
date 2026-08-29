package com.spironet.booking.controller;

import com.spironet.booking.dto.common.PageResponse;
import com.spironet.booking.dto.reservation.ReservationCreateRequest;
import com.spironet.booking.dto.reservation.ReservationResponse;
import com.spironet.booking.dto.reservation.ReservationUpdateRequest;
import com.spironet.booking.entity.ReservationStatus;
import com.spironet.booking.security.UserPrincipal;
import com.spironet.booking.service.ReservationService;
import com.spironet.booking.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Create and manage resource reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "List reservations (ADMIN sees all, USER sees only their own); filter by status/minPrice/maxPrice")
    public ResponseEntity<PageResponse<ReservationResponse>> list(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        UserPrincipal current = SecurityUtils.getCurrentUser();
        boolean isAdmin = SecurityUtils.isAdmin();
        return ResponseEntity.ok(PageResponse.from(
                reservationService.list(isAdmin, current.getId(), status, minPrice, maxPrice, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Get a reservation by id (USER may only access their own)")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id) {
        UserPrincipal current = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(reservationService.getById(id, SecurityUtils.isAdmin(), current.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Create a reservation for the authenticated user")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest request) {
        UserPrincipal current = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request, current.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fully update a reservation, including status (ADMIN only)")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id, @Valid @RequestBody ReservationUpdateRequest request) {
        return ResponseEntity.ok(reservationService.updateFull(id, request));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Cancel a reservation (USER may only cancel their own)")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable Long id) {
        UserPrincipal current = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(reservationService.cancel(id, SecurityUtils.isAdmin(), current.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a reservation (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
