package io.nuvalence.workmanager.service.domain.dynamicschema;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class DynamicEntityContainerEventListenerTest {
    @Test
    void testDynamicEntityPreSave() {
        DynamicEntityContainerEventListener listener = new DynamicEntityContainerEventListener();
        DynamicEntityContainer container = mock(DynamicEntityContainer.class);
        DynamicEntity data = mock(DynamicEntity.class);

        when(container.getData()).thenReturn(data);

        listener.dynamicEntityPreSave(container);

        verify(data, times(1)).preSave();
        verifyNoMoreInteractions(data);
    }

    @Test
    void testDynamicEntityPostLoad() {
        DynamicEntityContainerEventListener listener = new DynamicEntityContainerEventListener();
        DynamicEntityContainer container = mock(DynamicEntityContainer.class);
        DynamicEntity data = mock(DynamicEntity.class);

        when(container.getData()).thenReturn(data);

        listener.dynamicEntityPostLoad(container);

        verify(data, times(1)).postLoad();
        verifyNoMoreInteractions(data);
    }
}
