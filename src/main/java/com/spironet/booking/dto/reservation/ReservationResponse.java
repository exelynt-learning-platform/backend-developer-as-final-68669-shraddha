package com.spironet.booking.dto.reservation;

import com.spironet.booking.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ReservationResponse {
    private Long id;
    private Long resourceId;
    private String resourceName;
    private Long userId;
    private String username;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ReservationStatus status;
    private BigDecimal price;
    private Instant createdAt;
    private Instant updatedAt;
}
