package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileCreateModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileResponseModel;
import io.nuvalence.workmanager.service.generated.models.IndividualProfileUpdateModel;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(
        componentModel = "spring",
        uses = {AddressMapper.class})
public interface IndividualMapper {
    IndividualMapper INSTANCE = Mappers.getMapper(IndividualMapper.class);

    Individual createModelToIndividual(IndividualProfileCreateModel createModel);

    IndividualProfileResponseModel individualToResponseModel(Individual individual);

    Individual updateModelToIndividual(IndividualProfileUpdateModel updateModel);
}
