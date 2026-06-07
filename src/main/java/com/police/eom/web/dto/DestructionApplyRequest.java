package com.police.eom.web.dto;

import java.util.List;

public class DestructionApplyRequest {
    private Long applicantId;
    private String applyReason;
    private List<Long> evidenceIds;

    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public String getApplyReason() { return applyReason; }
    public void setApplyReason(String applyReason) { this.applyReason = applyReason; }
    public List<Long> getEvidenceIds() { return evidenceIds; }
    public void setEvidenceIds(List<Long> evidenceIds) { this.evidenceIds = evidenceIds; }
}
