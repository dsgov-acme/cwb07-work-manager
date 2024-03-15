package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import io.nuvalence.workmanager.service.generated.models.CreateMessageModel;
import io.nuvalence.workmanager.service.generated.models.ResponseMessageModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface MessageMapper {
    MessageMapper INSTANCE = Mappers.getMapper(MessageMapper.class);

    Message createModelToMessage(CreateMessageModel messageModel);

    @Mapping(target = "isOriginalMessage", source = "originalMessage")
    ResponseMessageModel messageToResponseModel(Message message);
}
