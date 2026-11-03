package com.cavsteek.jobapplicationtrackerapi.jobapplication;

import com.cavsteek.jobapplicationtrackerapi.common.InvalidRequestException;
import com.cavsteek.jobapplicationtrackerapi.common.ResourceNotFoundException;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.dto.*;
import com.cavsteek.jobapplicationtrackerapi.user.User;
import com.cavsteek.jobapplicationtrackerapi.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public JobApplicationResponse create(Long userId, CreateJobApplicationRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        JobApplication application = JobApplication.builder()
                .user(user)
                .company(req.company())
                .role(req.role())
                .jobUrl(req.jobUrl())
                .appliedDate(req.appliedDate())
                .notes(req.notes())
                .build();

        JobApplication saved = jobApplicationRepository.save(application);
        recordHistory(saved, null, saved.getStatus());
        return JobApplicationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<JobApplicationResponse> list(Long userId, ApplicationStatus status, Pageable pageable) {
        validateSort(pageable);
        Page<JobApplication> page = (status == null)
                ? jobApplicationRepository.findByUserId(userId, pageable)
                : jobApplicationRepository.findByUserIdAndStatus(userId, status, pageable);

        return page.map(JobApplicationResponse::from);
    }

    @Transactional(readOnly = true)
    public JobApplicationResponse get(Long userId, Long id) {
        return JobApplicationResponse.from(findOwned(userId, id));
    }

    @Transactional
    public JobApplicationResponse update(Long userId, Long id, UpdateJobApplicationRequest req) {
        JobApplication application = findOwned(userId, id);

        application.setCompany(req.company());
        application.setRole(req.role());
        application.setJobUrl(req.jobUrl());
        application.setAppliedDate(req.appliedDate());
        application.setNotes(req.notes());

        return JobApplicationResponse.from(jobApplicationRepository.saveAndFlush(application));
    }

    @Transactional
    public JobApplicationResponse updateStatus(Long userId, Long id, UpdateStatusRequest req) {
        JobApplication application = findOwned(userId, id);
        ApplicationStatus oldStatus = application.getStatus();

        if (oldStatus == req.status()) {
            return JobApplicationResponse.from(application);
        }

        application.setStatus(req.status());
        recordHistory(application, oldStatus, req.status());
        return JobApplicationResponse.from(jobApplicationRepository.saveAndFlush(application));
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> history(Long userId, Long id) {
        findOwned(userId, id);
        return statusHistoryRepository.findByApplicationIdOrderByChangedAtDesc(id).stream()
                .map(StatusHistoryResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long id) {
        jobApplicationRepository.delete(findOwned(userId, id));
    }

    private void recordHistory(JobApplication application, ApplicationStatus oldStatus, ApplicationStatus newStatus) {
        statusHistoryRepository.save(StatusHistory.builder()
                .application(application)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .build());
    }

    private JobApplication findOwned(Long userId, Long id) {
        return jobApplicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("appliedDate", "company", "role", "status", "createdAt", "lastUpdated");

    private void validateSort(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidRequestException(
                        "Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE_FIELDS);
            }
        });
    }
}