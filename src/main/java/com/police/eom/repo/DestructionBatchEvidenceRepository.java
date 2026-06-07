package com.police.eom.repo;

import com.police.eom.domain.DestructionBatchEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DestructionBatchEvidenceRepository extends JpaRepository<DestructionBatchEvidence, Long> {
    List<DestructionBatchEvidence> findByBatchId(Long batchId);
    Optional<DestructionBatchEvidence> findByEvidenceId(Long evidenceId);
    boolean existsByEvidenceId(Long evidenceId);
    void deleteByBatchIdAndEvidenceId(Long batchId, Long evidenceId);
}
