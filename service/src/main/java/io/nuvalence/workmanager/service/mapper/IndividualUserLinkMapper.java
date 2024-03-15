package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.profile.IndividualUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.generated.models.GeneralProfileModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileLinkResponseModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileLinkUpdateModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface IndividualUserLinkMapper {
    IndividualUserLinkMapper INSTANCE = Mappers.getMapper(IndividualUserLinkMapper.class);

    IndividualUserLink updateModelToIndividualLink(IndividualProfileLinkUpdateModel updateModel);

    @Mapping(target = "profileId", source = "individualUserLink.profile.id")
    IndividualProfileLinkResponseModel individualLUserlinkToResponseModel(
            IndividualUserLink individualUserLink);

    default GeneralProfileModel individualUserLinkToGeneralProfileModel(
            IndividualUserLink individualUserLink, String displayName) {
        GeneralProfileModel profileModel = new GeneralProfileModel();
        profileModel.setId(individualUserLink.getProfile().getId());
        profileModel.setType(ProfileType.INDIVIDUAL.getValue());
        profileModel.setLevel(individualUserLink.getAccessLevel().getValue());
        profileModel.setDisplayName(displayName);

        return profileModel;
    }
}
