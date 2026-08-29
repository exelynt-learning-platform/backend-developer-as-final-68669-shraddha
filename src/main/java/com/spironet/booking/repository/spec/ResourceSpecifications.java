package com.spironet.booking.repository.spec;

import com.spironet.booking.entity.Resource;
import com.spironet.booking.entity.ResourceType;
import org.springframework.data.jpa.domain.Specification;

public final class ResourceSpecifications {

    private ResourceSpecifications() {
    }

    public static Specification<Resource> hasType(ResourceType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Resource> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }
}
