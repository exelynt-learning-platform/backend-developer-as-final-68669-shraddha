package com.spironet.booking.dto.resource;

import com.spironet.booking.entity.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class ResourceResponse {
    private Long id;
    private String name;
    private ResourceType type;
    private String description;
    private String location;
    private Integer capacity;
    private BigDecimal pricePerHour;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
