package com.spironet.booking.controller;

import com.spironet.booking.dto.common.PageResponse;
import com.spironet.booking.dto.resource.ResourceRequest;
import com.spironet.booking.dto.resource.ResourceResponse;
import com.spironet.booking.entity.ResourceType;
import com.spironet.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Manage bookable resources (rooms, vehicles, equipment)")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "List resources (paginated, filterable by type/active)")
    public ResponseEntity<PageResponse<ResourceResponse>> list(
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(resourceService.list(type, active, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Get a resource by id")
    public ResponseEntity<ResourceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new resource (ADMIN only)")
    public ResponseEntity<ResourceResponse> create(@Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a resource (ADMIN only)")
    public ResponseEntity<ResourceResponse> update(@PathVariable Long id, @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.ok(resourceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a resource (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
