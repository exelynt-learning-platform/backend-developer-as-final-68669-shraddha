package com.spironet.booking.service;

import com.spironet.booking.dto.resource.ResourceRequest;
import com.spironet.booking.dto.resource.ResourceResponse;
import com.spironet.booking.entity.Resource;
import com.spironet.booking.entity.ResourceType;
import com.spironet.booking.exception.BadRequestException;
import com.spironet.booking.exception.ResourceNotFoundException;
import com.spironet.booking.repository.ResourceRepository;
import com.spironet.booking.repository.spec.ResourceSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public Page<ResourceResponse> list(ResourceType type, Boolean active, Pageable pageable) {
        Specification<Resource> spec = Specification
                .allOf(ResourceSpecifications.hasType(type), ResourceSpecifications.isActive(active));
        return resourceRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public ResourceResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .type(request.getType())
                .description(request.getDescription())
                .location(request.getLocation())
                .capacity(request.getCapacity())
                .pricePerHour(request.getPricePerHour())
                .active(request.getActive() == null || request.getActive())
                .build();
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = findEntity(id);
        resource.setName(request.getName());
        resource.setType(request.getType());
        resource.setDescription(request.getDescription());
        resource.setLocation(request.getLocation());
        resource.setCapacity(request.getCapacity());
        resource.setPricePerHour(request.getPricePerHour());
        if (request.getActive() != null) {
            resource.setActive(request.getActive());
        }
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public void delete(Long id) {
        Resource resource = findEntity(id);
        resourceRepository.delete(resource);
    }

    Resource findEntity(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    void ensureBookable(Resource resource) {
        if (!resource.isActive()) {
            throw new BadRequestException("Resource is not available for booking: " + resource.getName());
        }
    }

    private ResourceResponse toResponse(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .type(resource.getType())
                .description(resource.getDescription())
                .location(resource.getLocation())
                .capacity(resource.getCapacity())
                .pricePerHour(resource.getPricePerHour())
                .active(resource.isActive())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }
}
