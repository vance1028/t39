package com.police.eom.web.dto;

import com.police.eom.domain.DestructionBatch;
import com.police.eom.domain.DestructionSupervision;
import com.police.eom.domain.Evidence;

import java.util.List;

public class DestructionManifest {
    private DestructionBatch batch;
    private List<Evidence> evidenceList;
    private DestructionSupervision supervision;

    public DestructionManifest() {}

    public DestructionManifest(DestructionBatch batch, List<Evidence> evidenceList, DestructionSupervision supervision) {
        this.batch = batch;
        this.evidenceList = evidenceList;
        this.supervision = supervision;
    }

    public DestructionBatch getBatch() { return batch; }
    public void setBatch(DestructionBatch batch) { this.batch = batch; }
    public List<Evidence> getEvidenceList() { return evidenceList; }
    public void setEvidenceList(List<Evidence> evidenceList) { this.evidenceList = evidenceList; }
    public DestructionSupervision getSupervision() { return supervision; }
    public void setSupervision(DestructionSupervision supervision) { this.supervision = supervision; }
}
