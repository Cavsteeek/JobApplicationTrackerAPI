package com.cavsteek.jobapplicationtrackerapi.reminder;

import com.cavsteek.jobapplicationtrackerapi.email.EmailService;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.ApplicationStatus;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.JobApplication;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.JobApplicationRepository;
import com.cavsteek.jobapplicationtrackerapi.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private JobApplicationRepository repository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReminderService reminderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reminderService, "staleAfterDays", 7);
        ReflectionTestUtils.setField(reminderService, "repeatAfterDays", 7);
    }

    private User user(Long id, String email) {
        return User.builder().id(id).email(email).passwordHash("hash").build();
    }

    private JobApplication app(Long id, User owner, String company, ApplicationStatus status, int daysAgo) {
        return JobApplication.builder()
                .id(id).user(owner).company(company).role("Developer").status(status)
                .appliedDate(LocalDate.of(2026, 10, 1))
                .lastUpdated(Instant.now().minus(daysAgo, ChronoUnit.DAYS))
                .build();
    }

    private void givenDue(List<JobApplication> due) {
        when(repository.findDueForReminder(anyCollection(), any(Instant.class), any(Instant.class)))
                .thenReturn(due);
    }

    @Test
    void sendsOneEmailPerUser_andMarksTheirApplicationsReminded() {
        User one = user(1L, "one@example.com");
        User two = user(2L, "two@example.com");
        givenDue(List.of(
                app(1L, one, "Acme", ApplicationStatus.APPLIED, 10),
                app(2L, one, "Globex", ApplicationStatus.INTERVIEW, 8),
                app(3L, two, "Initech", ApplicationStatus.APPLIED, 9)));

        int sent = reminderService.sendDueReminders();

        assertThat(sent).isEqualTo(2);
        verify(emailService).send(eq("one@example.com"), contains("2 applications"), contains("Acme"));
        verify(emailService).send(eq("two@example.com"), contains("1 application"), contains("Initech"));
        verify(repository).markReminded(eq(List.of(1L, 2L)), any(Instant.class));
        verify(repository).markReminded(eq(List.of(3L)), any(Instant.class));
    }

    @Test
    void whenOneEmailFails_othersStillSend_andFailedUserIsNotMarked() {
        User one = user(1L, "one@example.com");
        User two = user(2L, "two@example.com");
        givenDue(List.of(
                app(1L, one, "Acme", ApplicationStatus.APPLIED, 10),
                app(3L, two, "Initech", ApplicationStatus.APPLIED, 9)));
        doThrow(new RuntimeException("brevo is down"))
                .when(emailService).send(eq("one@example.com"), anyString(), anyString());

        int sent = reminderService.sendDueReminders();

        assertThat(sent).isEqualTo(1);
        verify(repository, never()).markReminded(eq(List.of(1L)), any(Instant.class));
        verify(repository).markReminded(eq(List.of(3L)), any(Instant.class));
    }

    @Test
    void whenNothingIsDue_sendsNothing() {
        givenDue(List.of());

        int sent = reminderService.sendDueReminders();

        assertThat(sent).isZero();
        verifyNoInteractions(emailService);
        verify(repository, never()).markReminded(any(), any());
    }

    @Test
    void escapesHtmlInCompanyNames() {
        User one = user(1L, "one@example.com");
        givenDue(List.of(app(1L, one, "<script>alert(1)</script>", ApplicationStatus.APPLIED, 10)));

        reminderService.sendDueReminders();

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailService).send(eq("one@example.com"), anyString(), body.capture());
        assertThat(body.getValue()).doesNotContain("<script>").contains("&lt;script&gt;");
    }
}