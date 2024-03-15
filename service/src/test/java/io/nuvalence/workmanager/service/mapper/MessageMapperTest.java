package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import io.nuvalence.workmanager.service.generated.models.CreateMessageModel;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class MessageMapperTest {

    private final MessageMapper mapper = Mappers.getMapper(MessageMapper.class);

    @Test
    void testCreateModelToMessage() {
        // Create a sample CreateMessageModel instance
        CreateMessageModel createMessageModel = new CreateMessageModel();
        // Set necessary properties on createMessageModel

        // Call the mapper method
        Message message = mapper.createModelToMessage(createMessageModel);

        // Assertions
        assertNotNull(message);
        // Add more assertions based on the expected behavior of your mapper method and the mock
        // data
    }
}
