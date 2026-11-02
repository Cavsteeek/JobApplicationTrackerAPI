package com.cavsteek.jobapplicationtrackerapi.jobapplication;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByApplicationIdOrderByChangedAtDesc(Long applicationId);
}