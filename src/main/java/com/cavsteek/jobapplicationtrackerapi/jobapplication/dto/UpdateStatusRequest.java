package com.cavsteek.jobapplicationtrackerapi.jobapplication.dto;

import com.cavsteek.jobapplicationtrackerapi.jobapplication.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull ApplicationStatus status) {
}
