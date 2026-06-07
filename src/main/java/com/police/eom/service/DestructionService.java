package com.police.eom.service;

import com.police.eom.domain.*;
import com.police.eom.repo.*;
import com.police.eom.web.ApiException;
import com.police.eom.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class DestructionService {

    private final DestructionBatchRepository batchRepo;
    private final DestructionBatchEvidenceRepository batchEvidenceRepo;
    private final DestructionSupervisionRepository supervisionRepo;
    private final EvidenceRepository evidenceRepo;
    private final OfficerRepository officerRepo;
    private final CustodyRecordRepository custodyRepo;

    private final AtomicInteger batchCounter = new AtomicInteger(0);

    public DestructionService(DestructionBatchRepository batchRepo,
                              DestructionBatchEvidenceRepository batchEvidenceRepo,
                              DestructionSupervisionRepository supervisionRepo,
                              EvidenceRepository evidenceRepo,
                              OfficerRepository officerRepo,
                              CustodyRecordRepository custodyRepo) {
        this.batchRepo = batchRepo;
        this.batchEvidenceRepo = batchEvidenceRepo;
        this.supervisionRepo = supervisionRepo;
        this.evidenceRepo = evidenceRepo;
        this.officerRepo = officerRepo;
        this.custodyRepo = custodyRepo;
    }

    public List<DestructionBatch> listBatches(String status) {
        if (status != null && !status.isBlank()) return batchRepo.findByStatus(status);
        return batchRepo.findAll();
    }

    public DestructionBatch getBatch(Long id) {
        return batchRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("销毁批次不存在"));
    }

    public List<Evidence> getBatchEvidence(Long batchId) {
        getBatch(batchId);
        List<DestructionBatchEvidence> links = batchEvidenceRepo.findByBatchId(batchId);
        List<Long> evidenceIds = links.stream().map(DestructionBatchEvidence::getEvidenceId).collect(Collectors.toList());
        return evidenceRepo.findAllById(evidenceIds);
    }

    public DestructionSupervision getSupervision(Long batchId) {
        return supervisionRepo.findByBatchId(batchId)
                .orElseThrow(() -> ApiException.notFound("该批次尚未执行监销"));
    }

    @Transactional
    public DestructionBatch apply(DestructionApplyRequest req) {
        if (req.getApplicantId() == null || !officerRepo.existsById(req.getApplicantId())) {
            throw ApiException.badRequest("申请人不存在");
        }
        if (req.getEvidenceIds() == null || req.getEvidenceIds().isEmpty()) {
            throw ApiException.badRequest("请至少选择一件物证");
        }

        String batchNo = generateBatchNo();
        while (batchRepo.existsByBatchNo(batchNo)) {
            batchNo = generateBatchNo();
        }

        List<Long> invalidEvidenceIds = new ArrayList<>();
        for (Long evidenceId : req.getEvidenceIds()) {
            Evidence ev = evidenceRepo.findById(evidenceId)
                    .orElseThrow(() -> ApiException.badRequest("物证不存在: " + evidenceId));
            validateDestructionEligibility(ev);
            if (batchEvidenceRepo.existsByEvidenceId(evidenceId)) {
                throw ApiException.conflict("物证已在其他销毁批次中: " + ev.getEvidenceNo());
            }
        }

        DestructionBatch batch = new DestructionBatch();
        batch.setBatchNo(batchNo);
        batch.setApplicantId(req.getApplicantId());
        batch.setApplyReason(req.getApplyReason() == null ? "" : req.getApplyReason());
        batch.setStatus("PENDING_APPROVAL");
        DestructionBatch saved = batchRepo.save(batch);

        for (Long evidenceId : req.getEvidenceIds()) {
            Evidence ev = evidenceRepo.findById(evidenceId).orElseThrow();
            ev.setStatus("IN_DESTRUCTION");
            evidenceRepo.save(ev);

            DestructionBatchEvidence link = new DestructionBatchEvidence();
            link.setBatchId(saved.getId());
            link.setEvidenceId(evidenceId);
            batchEvidenceRepo.save(link);

            CustodyRecord rec = new CustodyRecord();
            rec.setEvidenceId(evidenceId);
            rec.setAction("DEST_APPLY");
            rec.setFromOfficer(req.getApplicantId());
            rec.setRemark("加入销毁批次: " + batchNo);
            custodyRepo.save(rec);
        }

        return saved;
    }

    @Transactional
    public DestructionBatch approve(Long batchId, DestructionApprovalRequest req) {
        if (req.getApproverId() == null || !officerRepo.existsById(req.getApproverId())) {
            throw ApiException.badRequest("审批人不存在");
        }
        DestructionBatch batch = getBatch(batchId);
        if (!"PENDING_APPROVAL".equals(batch.getStatus())) {
            throw ApiException.conflict("该批次当前状态不可审批: " + batch.getStatus());
        }
        if (req.getApproverId().equals(batch.getApplicantId())) {
            throw ApiException.conflict("审批人与申请人不能为同一人");
        }

        batch.setApproverId(req.getApproverId());
        batch.setApprovalRemark(req.getApprovalRemark() == null ? "" : req.getApprovalRemark());
        batch.setApprovedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(req.getApproved())) {
            batch.setStatus("APPROVED");
            List<DestructionBatchEvidence> links = batchEvidenceRepo.findByBatchId(batchId);
            for (DestructionBatchEvidence link : links) {
                Evidence ev = evidenceRepo.findById(link.getEvidenceId()).orElseThrow();
                ev.setStatus("PENDING_DESTRUCTION");
                evidenceRepo.save(ev);

                CustodyRecord rec = new CustodyRecord();
                rec.setEvidenceId(link.getEvidenceId());
                rec.setAction("DEST_APPROVE");
                rec.setFromOfficer(req.getApproverId());
                rec.setRemark("销毁审批通过，批次: " + batch.getBatchNo());
                custodyRepo.save(rec);
            }
        } else {
            batch.setStatus("REJECTED");
            List<DestructionBatchEvidence> links = batchEvidenceRepo.findByBatchId(batchId);
            for (DestructionBatchEvidence link : links) {
                Evidence ev = evidenceRepo.findById(link.getEvidenceId()).orElseThrow();
                ev.setStatus("IN_STORAGE");
                evidenceRepo.save(ev);

                CustodyRecord rec = new CustodyRecord();
                rec.setEvidenceId(link.getEvidenceId());
                rec.setAction("DEST_REJECT");
                rec.setFromOfficer(req.getApproverId());
                rec.setRemark("销毁申请被驳回，批次: " + batch.getBatchNo());
                custodyRepo.save(rec);
            }
            batchEvidenceRepo.deleteAll(links);
        }

        return batchRepo.save(batch);
    }

    @Transactional
    public DestructionSupervision execute(Long batchId, DestructionSuperviseRequest req) {
        DestructionBatch batch = getBatch(batchId);
        if (!"APPROVED".equals(batch.getStatus())) {
            throw ApiException.conflict("该批次当前状态不可执行监销: " + batch.getStatus());
        }
        if (supervisionRepo.existsByBatchId(batchId)) {
            throw ApiException.conflict("该批次已执行监销");
        }

        if (req.getSupervisor1Id() == null || req.getSupervisor2Id() == null) {
            throw ApiException.badRequest("两名监销人均需指定");
        }
        if (req.getSupervisor1Id().equals(req.getSupervisor2Id())) {
            throw ApiException.badRequest("两名监销人不能为同一人");
        }
        if (!officerRepo.existsById(req.getSupervisor1Id()) || !officerRepo.existsById(req.getSupervisor2Id())) {
            throw ApiException.badRequest("监销人不存在");
        }
        if (req.getSupervisor1Id().equals(batch.getApplicantId())
                || req.getSupervisor2Id().equals(batch.getApplicantId())) {
            throw ApiException.conflict("监销人不能与申请人为同一人");
        }
        if (req.getLocation() == null || req.getLocation().isBlank()) {
            throw ApiException.badRequest("监销地点不能为空");
        }
        if (req.getMethod() == null || req.getMethod().isBlank()) {
            throw ApiException.badRequest("销毁方式不能为空");
        }
        if (req.getSupervisionTime() == null) {
            req.setSupervisionTime(LocalDateTime.now());
        }

        DestructionSupervision supervision = new DestructionSupervision();
        supervision.setBatchId(batchId);
        supervision.setSupervisionTime(req.getSupervisionTime());
        supervision.setLocation(req.getLocation());
        supervision.setMethod(req.getMethod());
        supervision.setSupervisor1Id(req.getSupervisor1Id());
        supervision.setSupervisor2Id(req.getSupervisor2Id());
        supervision.setResultRemark(req.getResultRemark() == null ? "" : req.getResultRemark());
        DestructionSupervision savedSupervision = supervisionRepo.save(supervision);

        batch.setStatus("COMPLETED");
        batchRepo.save(batch);

        List<DestructionBatchEvidence> links = batchEvidenceRepo.findByBatchId(batchId);
        for (DestructionBatchEvidence link : links) {
            Evidence ev = evidenceRepo.findById(link.getEvidenceId()).orElseThrow();
            ev.setStatus("DESTROYED");
            evidenceRepo.save(ev);

            CustodyRecord rec = new CustodyRecord();
            rec.setEvidenceId(link.getEvidenceId());
            rec.setAction("DESTROYED");
            rec.setFromOfficer(req.getSupervisor1Id());
            rec.setToOfficer(req.getSupervisor2Id());
            rec.setRemark("已销毁，监销方式: " + req.getMethod() + "，批次: " + batch.getBatchNo());
            custodyRepo.save(rec);
        }

        return savedSupervision;
    }

    public DestructionManifest getManifest(Long batchId) {
        DestructionBatch batch = getBatch(batchId);
        List<Evidence> evidenceList = getBatchEvidence(batchId);
        DestructionSupervision supervision = null;
        if ("COMPLETED".equals(batch.getStatus())) {
            supervision = supervisionRepo.findByBatchId(batchId).orElse(null);
        }
        return new DestructionManifest(batch, evidenceList, supervision);
    }

    private void validateDestructionEligibility(Evidence ev) {
        if ("DESTROYED".equals(ev.getStatus())) {
            throw ApiException.conflict("物证已销毁: " + ev.getEvidenceNo());
        }
        if (!"IN_STORAGE".equals(ev.getStatus())) {
            throw ApiException.conflict("物证当前状态不可申请销毁: " + ev.getStatus() + " (" + ev.getEvidenceNo() + ")");
        }
        if (!"CLOSED".equals(ev.getCaseStatus())) {
            throw ApiException.conflict("关联案件尚未办结: " + ev.getEvidenceNo());
        }
        if (ev.getRetentionDueDate() == null) {
            throw ApiException.conflict("物证未设置法定留存期限: " + ev.getEvidenceNo());
        }
        if (ev.getRetentionDueDate().isAfter(LocalDate.now())) {
            throw ApiException.conflict("物证未过法定留存期: " + ev.getEvidenceNo()
                    + " (留存至 " + ev.getRetentionDueDate() + ")");
        }
    }

    private String generateBatchNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = batchCounter.incrementAndGet();
        return String.format("XH-%s-%04d", datePart, seq);
    }
}
