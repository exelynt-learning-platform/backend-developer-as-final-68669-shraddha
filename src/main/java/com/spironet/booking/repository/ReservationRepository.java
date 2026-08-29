package com.spironet.booking.repository;

import com.spironet.booking.entity.Reservation;
import com.spironet.booking.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    @Query("""
            select r from Reservation r
            where r.resource.id = :resourceId
              and r.status <> :excludedStatus
              and r.startTime < :endTime
              and r.endTime > :startTime
              and (:excludeReservationId is null or r.id <> :excludeReservationId)
            """)
    List<Reservation> findOverlapping(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedStatus") ReservationStatus excludedStatus,
            @Param("excludeReservationId") Long excludeReservationId);
}
