package com.cavsteek.jobapplicationtrackerapi.jobapplication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateJobApplicationRequest(
        @NotBlank @Size(max = 150) String company,
        @NotBlank @Size(max = 150) String role,
        @Size(max = 500) String jobUrl,
        @NotNull LocalDate appliedDate,
        String notes
) {}
