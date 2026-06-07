package com.police.eom.repo;

import com.police.eom.domain.DestructionBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DestructionBatchRepository extends JpaRepository<DestructionBatch, Long> {
    boolean existsByBatchNo(String batchNo);
    Optional<DestructionBatch> findByBatchNo(String batchNo);
    List<DestructionBatch> findByStatus(String status);
    List<DestructionBatch> findByApplicantId(Long applicantId);
}
