package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.transaction.TransactionLinkType;
import io.nuvalence.workmanager.service.repository.TransactionLinkTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TransactionLinkTypeServiceTest {

    private TransactionLinkTypeRepository repository;
    private TransactionLinkTypeService service;

    @BeforeEach
    void setUp() {
        repository = mock(TransactionLinkTypeRepository.class);

        service = new TransactionLinkTypeService(repository);
    }

    @Test
    void getTransactionLinkTypeById() {
        UUID id = UUID.randomUUID();

        TransactionLinkType tlt = TransactionLinkType.builder().id(id).build();

        when(repository.findById(id)).thenReturn(Optional.of(tlt));

        var response = service.getTransactionLinkTypeById(id);

        assert response.isPresent();
        assert response.get().getId().equals(id);

        verifyNoMoreInteractions(repository);
    }

    @Test
    void getTransactionLinkTypeById_NotFound() {
        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        var response = service.getTransactionLinkTypeById(id);

        assert response.isEmpty();

        verifyNoMoreInteractions(repository);
    }

    @Test
    void directRepositoryWrappers() {
        UUID id = UUID.randomUUID();
        TransactionLinkType tlt = TransactionLinkType.builder().id(id).build();

        // test 1
        when(repository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        var response = service.saveTransactionLinkType(tlt);

        assertNotNull(response);
        assert response.getId().equals(id);

        // test 2
        when(repository.findAll()).thenReturn(List.of(tlt));

        var responseList = service.getTransactionLinkTypes();

        assert responseList.size() == 1;
        assert responseList.get(0).getId().equals(id);

        // test 3
        service.deleteTransactionLinkType(id);

        verify(repository, times(1)).deleteTransactionLinkTypeById(id);
    }
}
