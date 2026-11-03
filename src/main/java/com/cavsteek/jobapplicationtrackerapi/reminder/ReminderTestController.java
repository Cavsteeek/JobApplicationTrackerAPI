package com.cavsteek.jobapplicationtrackerapi.reminder;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/reminders")
@RequiredArgsConstructor
public class ReminderTestController {

    private final ReminderService reminderService;

    @PostMapping("/run")
    public String run() {
        return "Sent " + reminderService.sendDueReminders() + " reminder email(s)";
    }
}