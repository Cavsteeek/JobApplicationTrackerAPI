package com.cavsteek.jobapplicationtrackerapi.jobapplication;

import com.cavsteek.jobapplicationtrackerapi.jobapplication.dto.CreateJobApplicationRequest;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.dto.JobApplicationResponse;
import com.cavsteek.jobapplicationtrackerapi.jobapplication.dto.UpdateJobApplicationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    // TODO replace with the logged-in user once auth exists
    private static final Long DEV_USER_ID = 1L;

    private final JobApplicationService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobApplicationResponse create(@Valid @RequestBody CreateJobApplicationRequest req) {
        return service.create(DEV_USER_ID, req);
    }

/*    @PostMapping
    public ResponseEntity<JobApplicationResponse> create(@Valid @RequestBody CreateJobApplicationRequest req) {
        JobApplicationResponse created = service.create(DEV_USER_ID, req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }*/

    @GetMapping
    public Page<JobApplicationResponse> list(
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 20, sort = "appliedDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(DEV_USER_ID, status, pageable);
    }

    @GetMapping("/{id}")
    public JobApplicationResponse get(@PathVariable Long id) {
        return service.get(DEV_USER_ID, id);
    }

    @PutMapping("/{id}")
    public JobApplicationResponse update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateJobApplicationRequest req) {
        return service.update(DEV_USER_ID, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(DEV_USER_ID, id);
    }
}