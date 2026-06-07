package com.police.eom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @Test
    void health_ok() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void officers_seeded() throws Exception {
        mvc.perform(get("/api/officers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void officer_duplicate_policeNo_conflict() throws Exception {
        String body = "{\"policeNo\":\"030001\",\"name\":\"重复警号\"}";
        mvc.perform(post("/api/officers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void officer_blank_name_badRequest() throws Exception {
        String body = "{\"policeNo\":\"099999\",\"name\":\"\"}";
        mvc.perform(post("/api/officers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evidence_list_byCase() throws Exception {
        mvc.perform(get("/api/evidence").param("caseNo", "AJ-2026-0101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void evidence_register_creates_custody_register_record() throws Exception {
        String body = "{\"evidenceNo\":\"WZ-TEST-9001\",\"caseNo\":\"AJ-TEST\",\"name\":\"测试物证\",\"category\":\"OTHER\",\"registeredBy\":3}";
        String resp = mvc.perform(post("/api/evidence").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_STORAGE"))
                .andReturn().getResponse().getContentAsString();
        long id = om.readTree(resp).get("id").asLong();

        mvc.perform(get("/api/evidence/" + id + "/custody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].action").value("REGISTER"));
    }

    @Test
    void evidence_duplicate_no_conflict() throws Exception {
        String body = "{\"evidenceNo\":\"WZ-2026-0001\",\"caseNo\":\"X\",\"name\":\"dup\"}";
        mvc.perform(post("/api/evidence").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void evidence_checkout_then_checkin_chain() throws Exception {
        // evidence 1 是 IN_STORAGE
        mvc.perform(post("/api/evidence/1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":1,\"remark\":\"勘验借出\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"));

        // 再次借出应 409
        mvc.perform(post("/api/evidence/1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":1}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/evidence/1/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":3,\"remark\":\"归还入库\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_STORAGE"));

        // 链路应包含 REGISTER + CHECK_OUT + CHECK_IN
        mvc.perform(get("/api/evidence/1/custody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void evidence_checkin_without_checkout_conflict() throws Exception {
        // evidence 2 处于 IN_STORAGE，未借出
        mvc.perform(post("/api/evidence/2/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":3}"))
                .andExpect(status().isConflict());
    }

    @Test
    void firearm_issue_and_return_flow() throws Exception {
        // firearm 1 在库
        String due = LocalDateTime.now().plusHours(6).toString();
        String issueBody = "{\"officerId\":4,\"purpose\":\"巡逻\",\"ammoIssued\":15,\"dueAt\":\"" + due + "\"}";
        mvc.perform(post("/api/firearms/1/issue").contentType(MediaType.APPLICATION_JSON).content(issueBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ISSUED"));

        // 枪应变为 ISSUED
        mvc.perform(get("/api/firearms/1"))
                .andExpect(jsonPath("$.status").value("ISSUED"));

        // 再次领用应 409
        mvc.perform(post("/api/firearms/1/issue").contentType(MediaType.APPLICATION_JSON).content(issueBody))
                .andExpect(status().isConflict());

        // 归还
        mvc.perform(post("/api/firearms/1/return").contentType(MediaType.APPLICATION_JSON).content("{\"ammoReturned\":13}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        mvc.perform(get("/api/firearms/1"))
                .andExpect(jsonPath("$.status").value("IN_STORE"));
    }

    @Test
    void firearm_issue_pastDue_badRequest() throws Exception {
        String due = LocalDateTime.now().minusHours(1).toString();
        String issueBody = "{\"officerId\":4,\"ammoIssued\":10,\"dueAt\":\"" + due + "\"}";
        mvc.perform(post("/api/firearms/3/issue").contentType(MediaType.APPLICATION_JSON).content(issueBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void firearm_return_ammo_exceeds_issued_badRequest() throws Exception {
        // firearm 2 在种子里是 ISSUED（issuance id=1, ammo_issued=15）
        mvc.perform(post("/api/firearms/2/return").contentType(MediaType.APPLICATION_JSON).content("{\"ammoReturned\":99}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void firearm_return_not_issued_conflict() throws Exception {
        // firearm 3 在库，未领用
        mvc.perform(post("/api/firearms/3/return").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void evidence_not_found() throws Exception {
        mvc.perform(get("/api/evidence/99999"))
                .andExpect(status().isNotFound());
    }

    // ========== 销毁流程测试 ==========

    @Test
    void destruction_full_flow_happy_path() throws Exception {
        // 1. 提请销毁：物证 4、5、6 都是已结案、过留存期的
        String applyBody = "{\"applicantId\":3,\"applyReason\":\"案件已办结且过留存期，统一销毁\",\"evidenceIds\":[4,5,6]}";
        String resp = mvc.perform(post("/api/destruction/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        long batchId = om.readTree(resp).get("id").asLong();
        String batchNo = om.readTree(resp).get("batchNo").asText();

        // 物证状态应变为 IN_DESTRUCTION
        mvc.perform(get("/api/evidence/4"))
                .andExpect(jsonPath("$.status").value("IN_DESTRUCTION"));

        // 批次物证应能查到
        mvc.perform(get("/api/destruction/batches/" + batchId + "/evidence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        // 保管链应写入 DEST_APPLY 记录
        mvc.perform(get("/api/evidence/4/custody"))
                .andExpect(jsonPath("$[-1].action").value("DEST_APPLY"));

        // 2. 审批通过（审批人不能是申请人 3，用 1）
        String approveBody = "{\"approverId\":1,\"approved\":true,\"approvalRemark\":\"同意销毁\"}";
        mvc.perform(post("/api/destruction/batches/" + batchId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 物证状态应变为 PENDING_DESTRUCTION
        mvc.perform(get("/api/evidence/4"))
                .andExpect(jsonPath("$.status").value("PENDING_DESTRUCTION"));

        // 保管链应写入 DEST_APPROVE 记录
        mvc.perform(get("/api/evidence/4/custody"))
                .andExpect(jsonPath("$[-1].action").value("DEST_APPROVE"));

        // 3. 执行监销（双人监销，都不能是申请人 3）
        String superviseBody = "{\"supervisionTime\":\"2026-06-07T10:00:00\",\"location\":\"物证销毁室\",\"method\":\"高温焚烧\",\"supervisor1Id\":1,\"supervisor2Id\":2,\"resultRemark\":\"全部物证已彻底销毁，无残留\"}";
        mvc.perform(post("/api/destruction/batches/" + batchId + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(superviseBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("高温焚烧"));

        // 批次状态应变为 COMPLETED
        mvc.perform(get("/api/destruction/batches/" + batchId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // 物证状态应变为 DESTROYED
        mvc.perform(get("/api/evidence/4"))
                .andExpect(jsonPath("$.status").value("DESTROYED"));
        mvc.perform(get("/api/evidence/5"))
                .andExpect(jsonPath("$.status").value("DESTROYED"));
        mvc.perform(get("/api/evidence/6"))
                .andExpect(jsonPath("$.status").value("DESTROYED"));

        // 保管链应写入 DESTROYED 记录
        mvc.perform(get("/api/evidence/4/custody"))
                .andExpect(jsonPath("$[-1].action").value("DESTROYED"));

        // 销毁清单应能查到
        mvc.perform(get("/api/destruction/batches/" + batchId + "/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.batchNo").value(batchNo))
                .andExpect(jsonPath("$.evidenceList.length()").value(3))
                .andExpect(jsonPath("$.supervision.method").value("高温焚烧"));

        // 监销记录应能查到
        mvc.perform(get("/api/destruction/batches/" + batchId + "/supervision"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supervisor1Id").value(1))
                .andExpect(jsonPath("$.supervisor2Id").value(2));
    }

    @Test
    void destruction_apply_invalid_evidence_rejected() throws Exception {
        // 物证 1 是 OPEN 案件，应被拦截
        String applyBody = "{\"applicantId\":3,\"applyReason\":\"测试\",\"evidenceIds\":[1]}";
        mvc.perform(post("/api/destruction/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody))
                .andExpect(status().isConflict());
    }

    @Test
    void destruction_approve_same_as_applicant_rejected() throws Exception {
        // 先建一个批次
        String applyBody = "{\"applicantId\":3,\"applyReason\":\"测试\",\"evidenceIds\":[4]}";
        String resp = mvc.perform(post("/api/destruction/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long batchId = om.readTree(resp).get("id").asLong();

        // 审批人 = 申请人，应拦截
        String approveBody = "{\"approverId\":3,\"approved\":true}";
        mvc.perform(post("/api/destruction/batches/" + batchId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody))
                .andExpect(status().isConflict());
    }

    @Test
    void destruction_supervision_same_supervisor_rejected() throws Exception {
        // 先建批次并审批通过
        String applyBody = "{\"applicantId\":3,\"applyReason\":\"测试\",\"evidenceIds\":[5]}";
        String resp = mvc.perform(post("/api/destruction/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long batchId = om.readTree(resp).get("id").asLong();

        mvc.perform(post("/api/destruction/batches/" + batchId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approverId\":1,\"approved\":true}"))
                .andExpect(status().isOk());

        // 两名监销人相同，应拦截
        String badBody = "{\"location\":\"销毁室\",\"method\":\"焚烧\",\"supervisor1Id\":1,\"supervisor2Id\":1}";
        mvc.perform(post("/api/destruction/batches/" + batchId + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void destruction_supervision_same_as_applicant_rejected() throws Exception {
        // 先建批次（申请人 3）并审批通过
        String applyBody = "{\"applicantId\":3,\"applyReason\":\"测试\",\"evidenceIds\":[6]}";
        String resp = mvc.perform(post("/api/destruction/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long batchId = om.readTree(resp).get("id").asLong();

        mvc.perform(post("/api/destruction/batches/" + batchId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approverId\":1,\"approved\":true}"))
                .andExpect(status().isOk());

        // 监销人包含申请人 3，应拦截
        String badBody = "{\"location\":\"销毁室\",\"method\":\"焚烧\",\"supervisor1Id\":3,\"supervisor2Id\":2}";
        mvc.perform(post("/api/destruction/batches/" + batchId + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badBody))
                .andExpect(status().isConflict());
    }

    @Test
    void destroyed_evidence_cannot_checkout() throws Exception {
        // 先走完销毁流程
        String applyBody = "{\"applicantId\":3,\"applyReason\":\"测试\",\"evidenceIds\":[4]}";
        String resp = mvc.perform(post("/api/destruction/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long batchId = om.readTree(resp).get("id").asLong();

        mvc.perform(post("/api/destruction/batches/" + batchId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approverId\":1,\"approved\":true}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/destruction/batches/" + batchId + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\"销毁室\",\"method\":\"焚烧\",\"supervisor1Id\":1,\"supervisor2Id\":2}"))
                .andExpect(status().isOk());

        // 已销毁物证借出应被拦截
        mvc.perform(post("/api/evidence/4/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":1}"))
                .andExpect(status().isConflict());
    }
}
