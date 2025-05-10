package org.example.term_pj.repository;

import org.example.term_pj.model.UsageHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UsageHistoryRepository extends JpaRepository<UsageHistory, Long> {
    Page<UsageHistory> findByUserIdOrderByUsageTimeDesc(Long userId, Pageable pageable);

    @Query("SELECT h " +
            "FROM UsageHistory h " +
            "WHERE h.usageTime BETWEEN ?1 AND ?2 ORDER BY h.usageTime DESC")
    Page<UsageHistory> findByUsageTimeBetweenOrderByUsageTimeDesc(
            LocalDateTime startDateTime, LocalDateTime endDateTime, Pageable pageable);
}
