package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileCreateModel;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileResponseModel;
import io.nuvalence.workmanager.service.generated.models.EmployerProfileUpdateModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.UUID;

class EmployerMapperTest {
    private final EmployerMapper mapper = Mappers.getMapper(EmployerMapper.class);
    private final AddressMapper addressMapper = Mappers.getMapper(AddressMapper.class);

    @BeforeEach
    void before() {
        ReflectionTestUtils.setField(mapper, "addressMapper", addressMapper);
    }

    @Test
    void employerToResponseModel() {
        UUID id = UUID.randomUUID();

        EmployerProfileResponseModel responseModelResult =
                mapper.employerToResponseModel(createEmployer(id));

        assertEquals(profileResponseModel(id), responseModelResult);
    }

    @Test
    void updateModelToEmployer() {
        Employer employerResult = mapper.updateModelToEmployer(profileUpdateModel());

        assertEquals(createEmployer(null), employerResult);
    }

    @Test
    void createModelToEmployer() {
        Employer employerResult = mapper.createModelToEmployer(profileCreateModel());

        assertEquals(createEmployer(null), employerResult);
    }

    private Employer createEmployer(UUID id) {
        return Employer.builder()
                .id(id)
                .fein("fein")
                .legalName("legalName")
                .otherNames(Collections.singletonList("otherNames"))
                .type("LLC")
                .industry("industry")
                .summaryOfBusiness("summaryOfBusiness")
                .businessPhone("businessPhone")
                .build();
    }

    private EmployerProfileUpdateModel profileUpdateModel() {
        EmployerProfileUpdateModel employerProfileUpdateModel = new EmployerProfileUpdateModel();
        employerProfileUpdateModel.setFein("fein");
        employerProfileUpdateModel.setLegalName("legalName");
        employerProfileUpdateModel.setOtherNames(Collections.singletonList("otherNames"));
        employerProfileUpdateModel.setType("LLC");
        employerProfileUpdateModel.setIndustry("industry");
        employerProfileUpdateModel.setSummaryOfBusiness("summaryOfBusiness");
        employerProfileUpdateModel.businessPhone("businessPhone");

        return employerProfileUpdateModel;
    }

    private EmployerProfileCreateModel profileCreateModel() {
        EmployerProfileCreateModel employerProfileCreateModel = new EmployerProfileCreateModel();
        employerProfileCreateModel.setFein("fein");
        employerProfileCreateModel.setLegalName("legalName");
        employerProfileCreateModel.setOtherNames(Collections.singletonList("otherNames"));
        employerProfileCreateModel.setType("LLC");
        employerProfileCreateModel.setIndustry("industry");
        employerProfileCreateModel.setSummaryOfBusiness("summaryOfBusiness");
        employerProfileCreateModel.businessPhone("businessPhone");

        return employerProfileCreateModel;
    }

    private EmployerProfileResponseModel profileResponseModel(UUID id) {
        EmployerProfileResponseModel employerProfileResponseModel =
                new EmployerProfileResponseModel();
        employerProfileResponseModel.setId(id);
        employerProfileResponseModel.setFein("fein");
        employerProfileResponseModel.setLegalName("legalName");
        employerProfileResponseModel.setOtherNames(Collections.singletonList("otherNames"));
        employerProfileResponseModel.setType("LLC");
        employerProfileResponseModel.setIndustry("industry");
        employerProfileResponseModel.setSummaryOfBusiness("summaryOfBusiness");
        employerProfileResponseModel.businessPhone("businessPhone");

        return employerProfileResponseModel;
    }
}
