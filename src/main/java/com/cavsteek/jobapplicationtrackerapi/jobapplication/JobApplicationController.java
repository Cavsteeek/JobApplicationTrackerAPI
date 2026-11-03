package com.cavsteek.jobapplicationtrackerapi.jobapplication;

import com.cavsteek.jobapplicationtrackerapi.jobapplication.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobApplicationResponse create(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody CreateJobApplicationRequest req) {
        return service.create(userId(jwt), req);
    }

    @GetMapping
    public Page<JobApplicationResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 20, sort = "appliedDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(userId(jwt), status, pageable);
    }

    @GetMapping("/{id}")
    public JobApplicationResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(userId(jwt), id);
    }

    @PutMapping("/{id}")
    public JobApplicationResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                         @Valid @RequestBody UpdateJobApplicationRequest req) {
        return service.update(userId(jwt), id, req);
    }

    @PatchMapping("/{id}/status")
    public JobApplicationResponse updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                               @Valid @RequestBody UpdateStatusRequest req) {
        return service.updateStatus(userId(jwt), id, req);
    }

    @GetMapping("/{id}/history")
    public List<StatusHistoryResponse> history(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.history(userId(jwt), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(userId(jwt), id);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
/*    @PostMapping
    public ResponseEntity<JobApplicationResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateJobApplicationRequest req) {
        JobApplicationResponse created = service.create(userId(jwt), req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }*/
