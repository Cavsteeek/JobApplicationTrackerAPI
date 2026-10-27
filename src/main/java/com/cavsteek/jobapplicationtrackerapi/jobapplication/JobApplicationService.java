package com.cavsteek.jobapplicationtrackerapi.jobapplication;

import com.cavsteek.jobapplicationtrackerapi.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobApplicationService {
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;


}
