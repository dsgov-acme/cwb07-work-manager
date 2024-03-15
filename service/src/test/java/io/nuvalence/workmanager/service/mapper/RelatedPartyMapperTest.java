package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.profile.RelatedParty;
import io.nuvalence.workmanager.service.generated.models.RelatedPartyModel;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

class RelatedPartyMapperTest {
    private final RelatedPartyMapper relatedPartyMapper =
            Mappers.getMapper(RelatedPartyMapper.class);

    @Test
    void entityToModelTest() {
        UUID id = UUID.randomUUID();
        RelatedParty relatedParty = new RelatedParty();
        relatedParty.setType(ProfileType.INDIVIDUAL);
        relatedParty.setProfileId(id);

        RelatedPartyModel model = relatedPartyMapper.entityToModel(relatedParty);

        assertEquals(ProfileType.INDIVIDUAL.getValue(), model.getType());
        assertEquals(id, model.getId());
    }

    @Test
    void entityListToModelTest() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        RelatedParty relatedParty1 = new RelatedParty();
        relatedParty1.setType(ProfileType.EMPLOYER);
        relatedParty1.setProfileId(id1);

        RelatedParty relatedParty2 = new RelatedParty();
        relatedParty2.setType(ProfileType.INDIVIDUAL);
        relatedParty2.setProfileId(id2);

        List<RelatedParty> relatedParties = Arrays.asList(relatedParty1, relatedParty2);
        List<RelatedPartyModel> models = relatedPartyMapper.entityListToModel(relatedParties);

        assertEquals(2, models.size());
        assertEquals(ProfileType.EMPLOYER.getValue(), models.get(0).getType());
        assertEquals(id1, models.get(0).getId());
        assertEquals(ProfileType.INDIVIDUAL.getValue(), models.get(1).getType());
        assertEquals(id2, models.get(1).getId());
    }
}
