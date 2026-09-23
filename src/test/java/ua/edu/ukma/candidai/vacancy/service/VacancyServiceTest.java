package ua.edu.ukma.candidai.vacancy.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ua.edu.ukma.candidai.common.exception.DuplicateResourceException;
import ua.edu.ukma.candidai.common.exception.InvalidStateTransitionException;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.util.CommonGenerator;
import ua.edu.ukma.candidai.vacancy.VacancyStatusChangedEvent;
import ua.edu.ukma.candidai.vacancy.dto.request.CreateVacancyRequest;
import ua.edu.ukma.candidai.vacancy.dto.request.UpdateVacancyStatusRequest;
import ua.edu.ukma.candidai.vacancy.dto.response.VacancyResponse;
import ua.edu.ukma.candidai.vacancy.model.JobCategory;
import ua.edu.ukma.candidai.vacancy.model.Vacancy;
import ua.edu.ukma.candidai.vacancy.model.VacancyStatus;
import ua.edu.ukma.candidai.vacancy.repository.VacancyRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ua.edu.ukma.candidai.vacancy.TestResources.DEFAULT_AUTHOR_ID;
import static ua.edu.ukma.candidai.vacancy.TestResources.DEFAULT_ID;
import static ua.edu.ukma.candidai.vacancy.TestResources.DEFAULT_NOW;
import static ua.edu.ukma.candidai.vacancy.TestResources.NON_EXISTENT_ID;
import static ua.edu.ukma.candidai.vacancy.TestResources.aDeletedVacancy;
import static ua.edu.ukma.candidai.vacancy.TestResources.aVacancy;
import static ua.edu.ukma.candidai.vacancy.TestResources.aVacancyPage;
import static ua.edu.ukma.candidai.vacancy.TestResources.aVacancyResponse;
import static ua.edu.ukma.candidai.vacancy.TestResources.aVacancyResponsePage;
import static ua.edu.ukma.candidai.vacancy.TestResources.validCreateVacancyRequest;

@ExtendWith(MockitoExtension.class)
class VacancyServiceTest {

    @Mock
    private VacancyRepository vacancyRepository;

    @Mock
    private VacancyMapper vacancyMapper;

    @Mock
    private CommonGenerator generator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private VacancyServiceImpl vacancyService;

    @Test
    @DisplayName("createVacancy with unique title should save vacancy and return response")
    void givenUniqueTitle_createVacancy_shouldSaveAndReturnResponse() {
        CreateVacancyRequest request = validCreateVacancyRequest();
        Vacancy vacancy = aVacancy();
        VacancyResponse expectedResponse = aVacancyResponse();

        when(vacancyRepository.existsActiveByAuthorIdAndTitle(DEFAULT_AUTHOR_ID, request.title())).thenReturn(false);
        when(generator.uuid()).thenReturn(DEFAULT_ID);
        when(generator.now()).thenReturn(DEFAULT_NOW);
        when(vacancyRepository.save(vacancy)).thenReturn(vacancy);
        when(vacancyMapper.toResponse(vacancy)).thenReturn(expectedResponse);

        VacancyResponse actual = vacancyService.createVacancy(request);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
        verify(vacancyRepository).save(vacancy);
    }

    @Test
    @DisplayName("createVacancy with duplicate title for author should throw DuplicateResourceException")
    void givenDuplicateTitleForAuthor_createVacancy_shouldThrowDuplicateResourceException() {
        CreateVacancyRequest request = validCreateVacancyRequest();
        when(vacancyRepository.existsActiveByAuthorIdAndTitle(DEFAULT_AUTHOR_ID, request.title())).thenReturn(true);

        assertThatThrownBy(() -> vacancyService.createVacancy(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Active vacancy with title 'Senior Java Engineer' already exists");

        verifyNoMoreInteractions(vacancyRepository);
    }

    @Test
    @DisplayName("getVacancyById with existing active vacancy should return response")
    void givenExistingId_getVacancyById_shouldReturnResponse() {
        Vacancy vacancy = aVacancy();
        VacancyResponse expectedResponse = aVacancyResponse();

        when(vacancyRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(vacancy));
        when(vacancyMapper.toResponse(vacancy)).thenReturn(expectedResponse);

        VacancyResponse actual = vacancyService.getVacancyById(DEFAULT_ID);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("getVacancyById with non-existent id should throw ResourceNotFoundException")
    void givenNonExistentId_getVacancyById_shouldThrowResourceNotFoundException() {
        when(vacancyRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vacancyService.getVacancyById(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vacancy not found with id: " + NON_EXISTENT_ID);
    }

    @Test
    @DisplayName("getVacancyById with soft-deleted vacancy should throw ResourceNotFoundException")
    void givenDeletedVacancy_getVacancyById_shouldThrowResourceNotFoundException() {
        Vacancy deletedVacancy = aDeletedVacancy();
        when(vacancyRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(deletedVacancy));

        assertThatThrownBy(() -> vacancyService.getVacancyById(DEFAULT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vacancy not found with id: " + DEFAULT_ID);
    }

    @Test
    @DisplayName("updateVacancyStatus with valid transition should update status and save")
    void givenValidStateTransition_updateVacancyStatus_shouldUpdateAndReturnResponse() {
        Vacancy vacancy = aVacancy();
        UpdateVacancyStatusRequest request = new UpdateVacancyStatusRequest(VacancyStatus.PAUSED);
        Instant updatedAt = DEFAULT_NOW.plusSeconds(3600);

        when(vacancyRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(vacancy));
        when(generator.now()).thenReturn(updatedAt);

        Vacancy expectedSavedVacancy = aVacancy(VacancyStatus.PAUSED, updatedAt);
        VacancyResponse expectedResponse = aVacancyResponse(VacancyStatus.PAUSED, updatedAt);

        when(vacancyRepository.save(expectedSavedVacancy)).thenReturn(expectedSavedVacancy);
        when(vacancyMapper.toResponse(expectedSavedVacancy)).thenReturn(expectedResponse);

        VacancyResponse actual = vacancyService.updateVacancyStatus(DEFAULT_ID, request);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedResponse);
        verify(vacancyRepository).save(expectedSavedVacancy);
        verify(eventPublisher).publishEvent(new VacancyStatusChangedEvent(
                DEFAULT_ID,
                expectedSavedVacancy.getTitle(),
                expectedSavedVacancy.getAuthorId(),
                VacancyStatus.OPEN,
                VacancyStatus.PAUSED,
                updatedAt
        ));
    }

    @Test
    @DisplayName("updateVacancyStatus with invalid transition should throw InvalidStateTransitionException")
    void givenInvalidStateTransition_updateVacancyStatus_shouldThrowInvalidStateTransitionException() {
        Vacancy closedVacancy = aVacancy(VacancyStatus.CLOSED);
        UpdateVacancyStatusRequest request = new UpdateVacancyStatusRequest(VacancyStatus.OPEN);

        when(vacancyRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(closedVacancy));

        assertThatThrownBy(() -> vacancyService.updateVacancyStatus(DEFAULT_ID, request))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition vacancy status from CLOSED to OPEN");

        verifyNoMoreInteractions(vacancyRepository);
    }

    @Test
    @DisplayName("deleteVacancy with existing id should mark as deleted and save")
    void givenExistingId_deleteVacancy_shouldSoftDelete() {
        Vacancy vacancy = aVacancy();
        Instant deletedAt = DEFAULT_NOW.plusSeconds(7200);

        when(vacancyRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(vacancy));
        when(generator.now()).thenReturn(deletedAt);

        Vacancy expectedDeletedVacancy = aDeletedVacancy(deletedAt);

        vacancyService.deleteVacancy(DEFAULT_ID);

        verify(vacancyRepository).save(expectedDeletedVacancy);
    }

    @Test
    @DisplayName("deleteVacancy with non-existent id should throw ResourceNotFoundException")
    void givenNonExistentId_deleteVacancy_shouldThrowResourceNotFoundException() {
        when(vacancyRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vacancyService.deleteVacancy(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vacancy not found with id: " + NON_EXISTENT_ID);

        verifyNoMoreInteractions(vacancyRepository);
    }

    @Test
    @DisplayName("getAllVacancies should return mapped page from repository")
    void givenFiltersAndPageable_getAllVacancies_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Vacancy vacancy = aVacancy();
        VacancyResponse expectedResponse = aVacancyResponse();
        Page<Vacancy> vacancyPage = aVacancyPage(List.of(vacancy), pageable);

        when(vacancyRepository.findAll(VacancyStatus.OPEN, JobCategory.ENGINEERING, pageable))
                .thenReturn(vacancyPage);
        when(vacancyMapper.toResponse(vacancy)).thenReturn(expectedResponse);

        Page<VacancyResponse> actual = vacancyService.getAllVacancies(
                VacancyStatus.OPEN,
                JobCategory.ENGINEERING,
                pageable
        );

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(aVacancyResponsePage(List.of(expectedResponse), pageable));
    }
}
