package com.spironet.booking.dto.resource;

import com.spironet.booking.entity.ResourceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must be at most 150 characters")
    private String name;

    @NotNull(message = "Type is required")
    private ResourceType type;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @Size(max = 200, message = "Location must be at most 200 characters")
    private String location;

    @Positive(message = "Capacity must be a positive number")
    private Integer capacity;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price per hour must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price per hour must have at most 2 decimal places")
    private BigDecimal pricePerHour;

    private Boolean active;
}
