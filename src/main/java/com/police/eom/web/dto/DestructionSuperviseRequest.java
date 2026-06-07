package com.police.eom.web.dto;

import java.time.LocalDateTime;

public class DestructionSuperviseRequest {
    private LocalDateTime supervisionTime;
    private String location;
    private String method;
    private Long supervisor1Id;
    private Long supervisor2Id;
    private String resultRemark;

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
}
