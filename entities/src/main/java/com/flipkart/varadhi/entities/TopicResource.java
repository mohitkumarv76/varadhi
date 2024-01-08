package com.flipkart.varadhi.entities;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
@ValidateResource(message = "Invalid Topic name. Check naming constraints.", max = 64)
//TODO: Topic Resource should be VersionedEntity
public class TopicResource extends MetaStoreEntity implements Validatable {
    private static final String RESOURCE_TYPE_NAME = "TopicResource";

    String project;
    boolean grouped;
    CapacityPolicy capacityPolicy;

    public TopicResource(
            String name,
            int version,
            String project,
            boolean grouped,
            CapacityPolicy capacityPolicy
    ) {
        super(name, version);
        this.project = project;
        this.grouped = grouped;
        this.capacityPolicy = capacityPolicy;
    }
}
