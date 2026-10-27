package com.cavsteek.jobapplicationtrackerapi.jobapplication.dto;

import com.cavsteek.jobapplicationtrackerapi.jobapplication.ApplicationStatus;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.JobApplication;

import java.time.Instant;
import java.time.LocalDate;

public record JobApplicationResponse(
        Long id,
        String company,
        String role,
        String jobUrl,
        ApplicationStatus status,
        LocalDate appliedDate,
        String notes,
        Instant createdAt,
        Instant lastUpdated
) {

    public static JobApplicationResponse from(JobApplication a) {
        return new JobApplicationResponse(
                a.getId(), a.getCompany(), a.getRole(), a.getJobUrl(),
                a.getStatus(), a.getAppliedDate(), a.getNotes(),
                a.getCreatedAt(), a.getLastUpdated());
    }
}
