package com.police.eom.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "destruction_supervisions")
public class DestructionSupervision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "supervision_time", nullable = false)
    private LocalDateTime supervisionTime;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(nullable = false, length = 64)
    private String method;

    @Column(name = "supervisor1_id", nullable = false)
    private Long supervisor1Id;

    @Column(name = "supervisor2_id", nullable = false)
    private Long supervisor2Id;

    @Column(name = "result_remark", nullable = false, length = 1000)
    private String resultRemark = "";

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @PrePersist
    public void prePersist() {
        if (executedAt == null) executedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public LocalDateTime getSupervisionTime() { return supervisionTime; }
    public void setSupervisionTime(LocalDateTime supervisionTime) { this.supervisionTime = supervisionTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Long getSupervisor1Id() { return supervisor1Id; }
    public void setSupervisor1Id(Long supervisor1Id) { this.supervisor1Id = supervisor1Id; }
    public Long getSupervisor2Id() { return supervisor2Id; }
    public void setSupervisor2Id(Long supervisor2Id) { this.supervisor2Id = supervisor2Id; }
    public String getResultRemark() { return resultRemark; }
    public void setResultRemark(String resultRemark) { this.resultRemark = resultRemark; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
}
