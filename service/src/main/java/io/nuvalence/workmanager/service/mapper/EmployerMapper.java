package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileCreateModel;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileResponseModel;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileUpdateModel;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(
        componentModel = "spring",
        uses = {AddressMapper.class})
public interface EmployerMapper {
    EmployerMapper INSTANCE = Mappers.getMapper(EmployerMapper.class);

    EmployerProfileResponseModel employerToResponseModel(Employer employer);

    Employer updateModelToEmployer(EmployerProfileUpdateModel updateModel);

    Employer createModelToEmployer(EmployerProfileCreateModel createModel);
}
