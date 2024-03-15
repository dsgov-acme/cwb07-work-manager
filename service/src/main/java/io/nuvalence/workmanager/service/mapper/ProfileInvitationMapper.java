package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.profile.ProfileInvitation;
import io.nuvalence.workmanager.service.generated.models.ProfileInvitationRequestModel;
import io.nuvalence.workmanager.service.generated.models.ProfileInvitationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

@Mapper(
        componentModel = "spring",
        uses = {AddressMapper.class})
public interface ProfileInvitationMapper {
    ProfileInvitationMapper INSTANCE = Mappers.getMapper(ProfileInvitationMapper.class);

    ProfileInvitation createModelToProfileInvitation(
            UUID profileId, ProfileInvitationRequestModel createModel);

    @Mapping(source = "type", target = "profileType")
    ProfileInvitationResponse profileInvitationToResponseModel(ProfileInvitation profileInvitation);
}
