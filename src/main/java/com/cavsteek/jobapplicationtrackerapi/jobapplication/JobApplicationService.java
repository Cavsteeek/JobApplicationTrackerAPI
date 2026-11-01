package com.cavsteek.jobapplicationtrackerapi.jobapplication;

import com.cavsteek.jobapplicationtrackerapi.common.ResourceNotFoundException;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.dto.CreateJobApplicationRequest;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.dto.JobApplicationResponse;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.dto.UpdateJobApplicationRequest;
import com.cavsteek.jobapplicationtrackerapi.user.User;
import com.cavsteek.jobapplicationtrackerapi.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
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

        return JobApplicationResponse.from(jobApplicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public Page<JobApplicationResponse> list(Long userId, ApplicationStatus status, Pageable pageable) {
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
    public void delete(Long userId, Long id) {
        jobApplicationRepository.delete(findOwned(userId, id));
    }

    private JobApplication findOwned(Long userId, Long id) {
        return jobApplicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }
}