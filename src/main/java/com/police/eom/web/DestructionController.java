package com.police.eom.web;

import com.police.eom.domain.DestructionBatch;
import com.police.eom.domain.DestructionSupervision;
import com.police.eom.domain.Evidence;
import com.police.eom.service.DestructionService;
import com.police.eom.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destruction")
public class DestructionController {

    private final DestructionService service;

    public DestructionController(DestructionService service) {
        this.service = service;
    }

    @GetMapping("/batches")
    public List<DestructionBatch> listBatches(@RequestParam(required = false) String status) {
        return service.listBatches(status);
    }

    @GetMapping("/batches/{id}")
    public DestructionBatch getBatch(@PathVariable Long id) {
        return service.getBatch(id);
    }

    @GetMapping("/batches/{id}/evidence")
    public List<Evidence> getBatchEvidence(@PathVariable Long id) {
        return service.getBatchEvidence(id);
    }

    @GetMapping("/batches/{id}/supervision")
    public DestructionSupervision getSupervision(@PathVariable Long id) {
        return service.getSupervision(id);
    }

    @GetMapping("/batches/{id}/manifest")
    public DestructionManifest getManifest(@PathVariable Long id) {
        return service.getManifest(id);
    }

    @PostMapping("/apply")
    public ResponseEntity<DestructionBatch> apply(@RequestBody DestructionApplyRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.apply(req));
    }

    @PostMapping("/batches/{id}/approve")
    public DestructionBatch approve(@PathVariable Long id, @RequestBody DestructionApprovalRequest req) {
        return service.approve(id, req);
    }

    @PostMapping("/batches/{id}/execute")
    public DestructionSupervision execute(@PathVariable Long id, @RequestBody DestructionSuperviseRequest req) {
        return service.execute(id, req);
    }
}
