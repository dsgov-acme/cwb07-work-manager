package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileLinkRequestModel;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileLinkResponse;
import io.nuvalence.workmanager.service.generated.models.GeneralProfileModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface EmployerUserLinkMapper {
    EmployerUserLinkMapper INSTANCE = Mappers.getMapper(EmployerUserLinkMapper.class);

    @Mapping(source = "profile.id", target = "profileId")
    EmployerProfileLinkResponse employerUserLinkToResponseModel(EmployerUserLink employer);

    EmployerUserLink updateModelToEmployerlLink(EmployerProfileLinkRequestModel updateModel);

    default GeneralProfileModel employerUserLinkToGeneralProfileModel(
            EmployerUserLink employerUserLink) {
        GeneralProfileModel profileModel = new GeneralProfileModel();
        profileModel.setId(employerUserLink.getProfile().getId());
        profileModel.setType(ProfileType.EMPLOYER.getValue());
        profileModel.setLevel(employerUserLink.getProfileAccessLevel().getValue());
        profileModel.setDisplayName(employerUserLink.getProfile().getLegalName());

        return profileModel;
    }
}
