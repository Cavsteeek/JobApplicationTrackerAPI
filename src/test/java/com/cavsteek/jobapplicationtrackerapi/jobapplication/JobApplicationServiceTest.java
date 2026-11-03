package com.cavsteek.jobapplicationtrackerapi.jobapplication;

import com.cavsteek.jobapplicationtrackerapi.common.ResourceNotFoundException;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.dto.CreateJobApplicationRequest;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.dto.JobApplicationResponse;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.dto.StatusHistoryResponse;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.dto.UpdateStatusRequest;
import com.cavsteek.jobapplicationtrackerapi.user.User;
import com.cavsteek.jobapplicationtrackerapi.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository applicationRepository;
    @Mock
    private StatusHistoryRepository historyRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JobApplicationService service;

    private User user() {
        return User.builder().id(1L).email("a@example.com").passwordHash("hash").build();
    }

    private JobApplication application(Long id, ApplicationStatus status) {
        return JobApplication.builder()
                .id(id).user(user()).company("Acme").role("Backend Developer")
                .status(status).appliedDate(LocalDate.of(2026, 10, 1))
                .build();
    }

    // ---------- create ----------

    @Test
    void create_savesApplicationAndRecordsInitialHistory() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> {
            JobApplication saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        JobApplicationResponse response = service.create(1L, new CreateJobApplicationRequest(
                "Acme", "Backend Developer", "https://acme.com/jobs/1",
                LocalDate.of(2026, 10, 1), "via portal"));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.company()).isEqualTo("Acme");
        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);

        ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getOldStatus()).isNull();
        assertThat(captor.getValue().getNewStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void create_whenUserMissing_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, new CreateJobApplicationRequest(
                "Acme", "Dev", null, LocalDate.of(2026, 10, 1), null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(applicationRepository);
    }

    // ---------- get / ownership ----------

    @Test
    void get_whenNotOwnedOrMissing_throwsNotFound() {
        when(applicationRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- list ----------

    @Test
    void list_withoutStatus_usesUserQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        when(applicationRepository.findByUserId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(application(1L, ApplicationStatus.APPLIED))));

        Page<JobApplicationResponse> result = service.list(1L, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(applicationRepository, never()).findByUserIdAndStatus(any(), any(), any());
    }

    @Test
    void list_withStatus_usesStatusQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        when(applicationRepository.findByUserIdAndStatus(1L, ApplicationStatus.INTERVIEW, pageable))
                .thenReturn(new PageImpl<>(List.of(application(2L, ApplicationStatus.INTERVIEW))));

        Page<JobApplicationResponse> result = service.list(1L, ApplicationStatus.INTERVIEW, pageable);

        assertThat(result.getContent()).extracting(JobApplicationResponse::status)
                .containsExactly(ApplicationStatus.INTERVIEW);
        verify(applicationRepository, never()).findByUserId(any(), any());
    }

    // ---------- updateStatus ----------

    @Test
    void updateStatus_whenChanged_savesAndRecordsHistory() {
        JobApplication app = application(5L, ApplicationStatus.APPLIED);
        when(applicationRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(app));
        when(applicationRepository.saveAndFlush(app)).thenReturn(app);

        JobApplicationResponse response =
                service.updateStatus(1L, 5L, new UpdateStatusRequest(ApplicationStatus.INTERVIEW));

        assertThat(response.status()).isEqualTo(ApplicationStatus.INTERVIEW);

        ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getOldStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(captor.getValue().getNewStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    void updateStatus_whenSameStatus_doesNothing() {
        JobApplication app = application(5L, ApplicationStatus.APPLIED);
        when(applicationRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(app));

        service.updateStatus(1L, 5L, new UpdateStatusRequest(ApplicationStatus.APPLIED));

        verify(historyRepository, never()).save(any());
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    // ---------- history ----------

    @Test
    void history_checksOwnershipThenReturnsRows() {
        JobApplication app = application(5L, ApplicationStatus.APPLIED);
        StatusHistory row = StatusHistory.builder()
                .id(1L).application(app).oldStatus(null).newStatus(ApplicationStatus.APPLIED).build();
        when(applicationRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(app));
        when(historyRepository.findByApplicationIdOrderByChangedAtDesc(5L)).thenReturn(List.of(row));

        List<StatusHistoryResponse> result = service.history(1L, 5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).newStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void history_whenNotOwned_neverReadsHistory() {
        when(applicationRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.history(1L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(historyRepository);
    }

    // ---------- delete ----------

    @Test
    void delete_removesOwnedApplication() {
        JobApplication app = application(5L, ApplicationStatus.APPLIED);
        when(applicationRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(app));

        service.delete(1L, 5L);

        verify(applicationRepository).delete(app);
    }

    @Test
    void delete_whenMissing_throwsAndDeletesNothing() {
        when(applicationRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(applicationRepository, never()).delete(any());
    }
}