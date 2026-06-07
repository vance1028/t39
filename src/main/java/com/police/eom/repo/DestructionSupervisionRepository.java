package com.police.eom.repo;

import com.police.eom.domain.DestructionSupervision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DestructionSupervisionRepository extends JpaRepository<DestructionSupervision, Long> {
    Optional<DestructionSupervision> findByBatchId(Long batchId);
    boolean existsByBatchId(Long batchId);
}
