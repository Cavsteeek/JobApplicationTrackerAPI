package com.cavsteek.jobapplicationtrackerapi.jobapplication.dto;

import com.cavsteek.jobapplicationtrackerapi.jobapplication.ApplicationStatus;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.StatusHistory;

import java.time.Instant;

public record StatusHistoryResponse(
        Long id,
        ApplicationStatus oldStatus,
        ApplicationStatus newStatus,
        Instant changedAt) {

    public static StatusHistoryResponse from(StatusHistory h) {
        return new StatusHistoryResponse(h.getId(), h.getOldStatus(), h.getNewStatus(), h.getChangedAt());
    }
}