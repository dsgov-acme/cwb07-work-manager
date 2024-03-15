package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.profile.RelatedParty;
import io.nuvalence.workmanager.service.generated.models.RelatedPartyModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RelatedPartyMapper {
    @Mapping(target = "type", expression = "java(typeToString(relatedParty.getType()))")
    @Mapping(target = "id", expression = "java(relatedParty.getProfileId())")
    RelatedPartyModel entityToModel(RelatedParty relatedParty);

    default List<RelatedPartyModel> entityListToModel(List<RelatedParty> relatedPartyList) {
        return relatedPartyList.stream().map(this::entityToModel).toList();
    }

    default String typeToString(ProfileType type) {
        return type.getValue();
    }
}
