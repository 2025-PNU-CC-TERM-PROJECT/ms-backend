package org.example.term_pj.repository;


import org.example.term_pj.model.FilePath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface FilePathRepository extends JpaRepository<FilePath, Long> {
    Optional<FilePath> findByUsageHistoryId(Long usageHistoryId);
}
