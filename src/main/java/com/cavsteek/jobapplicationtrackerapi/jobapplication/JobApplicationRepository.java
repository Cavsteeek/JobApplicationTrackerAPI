package com.cavsteek.jobapplicationtrackerapi.jobapplication;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    Page<JobApplication> findByUserId(Long userId, Pageable pageable);
    Page<JobApplication> findByUserIdAndStatus(Long userId, ApplicationStatus status, Pageable pageable);
    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);

    @Query("""
        select a from JobApplication a
        join fetch a.user
        where a.status in :statuses
          and a.lastUpdated < :staleBefore
          and (a.lastRemindedAt is null or a.lastRemindedAt < :remindedBefore)
        order by a.user.id, a.lastUpdated
        """)
    List<JobApplication> findDueForReminder(@Param("statuses") Collection<ApplicationStatus> statuses,
                                            @Param("staleBefore") Instant staleBefore,
                                            @Param("remindedBefore") Instant remindedBefore);

    @Modifying
    @Transactional
    @Query(value = "update applications set last_reminded_at = :now where id in (:ids)", nativeQuery = true)
    int markReminded(@Param("ids") Collection<Long> ids, @Param("now") Instant now);
}