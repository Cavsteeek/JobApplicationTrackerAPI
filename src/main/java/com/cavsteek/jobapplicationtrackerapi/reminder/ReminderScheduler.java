package com.cavsteek.jobapplicationtrackerapi.reminder;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderService reminderService;

    @Scheduled(cron = "${reminder.cron}", zone = "${reminder.zone}")
    public void run() {
        reminderService.sendDueReminders();
    }
}