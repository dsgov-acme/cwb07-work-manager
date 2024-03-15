package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import io.nuvalence.workmanager.service.models.EmployerUserLinkFilters;
import io.nuvalence.workmanager.service.repository.EmployerUserLinkRepository;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class EmployerUserLinkServiceTest {

    @Mock private EmployerUserLinkRepository repository;
    @Mock private UserManagementClient userManagementClient;

    private EmployerUserLinkService service;

    @BeforeEach
    public void setUp() {
        service = new EmployerUserLinkService(repository);
    }

    @Test
    void saveEmployerUserLink() {
        EmployerUserLink link = new EmployerUserLink();
        service.saveEmployerUserLink(link);
        verify(repository, times(1)).save(link);
    }

    @Test
    void getEmployerUserLink() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployerUserLink link = new EmployerUserLink();

        when(repository.findByProfileIdAndUserId(profileId, userId)).thenReturn(Optional.of(link));

        Optional<EmployerUserLink> result = service.getEmployerUserLink(profileId, userId);

        assertTrue(result.isPresent());
        assertEquals(link, result.get());
    }

    @Test
    void getEmployerUserLinkByUserId() {
        UUID userId = UUID.randomUUID();
        EmployerUserLink link = new EmployerUserLink();

        when(repository.findByUserId(userId)).thenReturn(List.of(link));

        List<EmployerUserLink> result = service.getEmployerByUserId(userId);

        assertFalse(result.isEmpty());
        assertEquals(link, result.get(0));
    }

    @Test
    void deleteEmployerUserLink() {
        UUID id = UUID.randomUUID();
        service.deleteEmployerUserLink(id);
        verify(repository, times(1)).deleteById(id);
    }

    @Test
    void getEmployerUserLinks() {
        UUID profileId = UUID.randomUUID();
        List<UUID> userIds = Collections.singletonList(UUID.randomUUID());
        String sortBy = "createdTimestamp";
        String sortOrder = "ASC";
        Integer pageNumber = 0;
        Integer pageSize = 10;

        EmployerUserLinkFilters filters =
                EmployerUserLinkFilters.builder()
                        .profileId(profileId)
                        .userIds(userIds)
                        .sortBy(sortBy)
                        .sortOrder(sortOrder)
                        .pageNumber(pageNumber)
                        .pageSize(pageSize)
                        .build();

        EmployerUserLink link = new EmployerUserLink();
        Page<EmployerUserLink> expectedPage = new PageImpl<>(Collections.singletonList(link));

        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(expectedPage);

        Page<EmployerUserLink> result = service.getEmployerUserLinks(filters);

        assertEquals(expectedPage, result);
    }

    @Test
    void getEmployerLinksByUserId() {
        String userId = UUID.randomUUID().toString();

        List<EmployerUserLink> expected =
                List.of(EmployerUserLink.builder().id(UUID.randomUUID()).build());

        when(repository.findByUserId(UUID.fromString(userId))).thenReturn(expected);

        List<EmployerUserLink> result = service.getEmployerLinksByUserId(userId);

        assertEquals(expected, result);
    }
}
