package com.cavsteek.jobapplicationtrackerapi.reminder;

import com.cavsteek.jobapplicationtrackerapi.email.EmailService;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.ApplicationStatus;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.JobApplication;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.JobApplicationRepository;
import com.cavsteek.jobapplicationtrackerapi.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final List<ApplicationStatus> PENDING =
            List.of(ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEW);

    private final JobApplicationRepository repository;
    private final EmailService emailService;

    @Value("${reminder.stale-after-days}")
    private int staleAfterDays;

    @Value("${reminder.repeat-after-days}")
    private int repeatAfterDays;

    public int sendDueReminders() {
        Instant now = Instant.now();

        List<JobApplication> due = repository.findDueForReminder(
                PENDING,
                now.minus(staleAfterDays, ChronoUnit.DAYS),
                now.minus(repeatAfterDays, ChronoUnit.DAYS));

        Map<Long, List<JobApplication>> byUser = due.stream()
                .collect(Collectors.groupingBy(a -> a.getUser().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        int sent = 0;
        for (List<JobApplication> apps : byUser.values()) {
            User user = apps.get(0).getUser();
            try {
                emailService.send(user.getEmail(), subject(apps.size()), buildHtml(apps, now));
                repository.markReminded(apps.stream().map(JobApplication::getId).toList(), now);
                sent++;
            } catch (Exception e) {
                log.error("Reminder email failed for user {}", user.getId(), e);
            }
        }

        log.info("Reminder run finished: {} application(s) due, {} email(s) sent", due.size(), sent);
        return sent;
    }

    private String subject(int count) {
        return count == 1
                ? "Job Tracker: 1 application needs a follow-up"
                : "Job Tracker: " + count + " applications need a follow-up";
    }

    private String buildHtml(List<JobApplication> apps, Instant now) {
        StringBuilder html = new StringBuilder(
                "<p>These applications have had no update for a while. Time to follow up?</p><ul>");
        for (JobApplication a : apps) {
            long days = ChronoUnit.DAYS.between(a.getLastUpdated(), now);
            html.append("<li><b>").append(HtmlUtils.htmlEscape(a.getCompany())).append("</b> - ")
                    .append(HtmlUtils.htmlEscape(a.getRole()))
                    .append(" (").append(a.getStatus()).append(", ")
                    .append(days).append(" days since last update)</li>");
        }
        return html.append("</ul>").toString();
    }
}