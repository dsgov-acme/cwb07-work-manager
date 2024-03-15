package io.nuvalence.workmanager.service.domain.dynamicschema;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * JPA Event Listener to handle pre-persists, pre-update and post-load operations on DynamicEntity.
 */
public class DynamicEntityContainerEventListener {
    @PrePersist
    @PreUpdate
    public void dynamicEntityPreSave(final DynamicEntityContainer container) {
        container.getData().preSave();
    }

    @PostLoad
    public void dynamicEntityPostLoad(final DynamicEntityContainer container) {
        container.getData().postLoad();
    }
}
