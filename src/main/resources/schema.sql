-- 公安证据与枪械管理平台 - 表结构（MySQL）

CREATE TABLE IF NOT EXISTS officers (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    police_no   VARCHAR(32)  NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    department  VARCHAR(128) NOT NULL DEFAULT '',
    rank_title  VARCHAR(64)  NOT NULL DEFAULT '',
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_officers_police_no (police_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS evidence (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    evidence_no       VARCHAR(48)  NOT NULL,
    case_no           VARCHAR(48)  NOT NULL,
    name              VARCHAR(128) NOT NULL,
    category          VARCHAR(32)  NOT NULL DEFAULT 'OTHER',
    description       VARCHAR(1000) NOT NULL DEFAULT '',
    status            VARCHAR(24)  NOT NULL DEFAULT 'REGISTERED',
    location          VARCHAR(128) NOT NULL DEFAULT '',
    registered_by     BIGINT       NULL,
    case_status       VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
    retention_due_date DATE        NULL,
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_evidence_no (evidence_no),
    KEY idx_evidence_case (case_no),
    KEY idx_evidence_status (status),
    CONSTRAINT fk_evidence_officer FOREIGN KEY (registered_by) REFERENCES officers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 销毁批次表
CREATE TABLE IF NOT EXISTS destruction_batches (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    batch_no        VARCHAR(48)  NOT NULL,
    applicant_id    BIGINT       NOT NULL,
    apply_reason    VARCHAR(500) NOT NULL DEFAULT '',
    status          VARCHAR(24)  NOT NULL DEFAULT 'DRAFT',
    approver_id     BIGINT       NULL,
    approval_remark VARCHAR(500) NOT NULL DEFAULT '',
    approved_at     DATETIME(3)  NULL,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_destruction_batch_no (batch_no),
    KEY idx_destruction_status (status),
    CONSTRAINT fk_destruction_applicant FOREIGN KEY (applicant_id) REFERENCES officers (id),
    CONSTRAINT fk_destruction_approver FOREIGN KEY (approver_id) REFERENCES officers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 销毁批次-物证关联表（一件物证同时只能在一个在途批次）
CREATE TABLE IF NOT EXISTS destruction_batch_evidence (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    batch_id     BIGINT       NOT NULL,
    evidence_id  BIGINT       NOT NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_destruction_evidence (evidence_id),
    KEY idx_destruction_batch (batch_id),
    CONSTRAINT fk_dbe_batch FOREIGN KEY (batch_id) REFERENCES destruction_batches (id),
    CONSTRAINT fk_dbe_evidence FOREIGN KEY (evidence_id) REFERENCES evidence (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 监销执行记录表
CREATE TABLE IF NOT EXISTS destruction_supervisions (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    batch_id         BIGINT       NOT NULL,
    supervision_time DATETIME(3)  NOT NULL,
    location         VARCHAR(255) NOT NULL,
    method           VARCHAR(64)  NOT NULL,
    supervisor1_id   BIGINT       NOT NULL,
    supervisor2_id   BIGINT       NOT NULL,
    result_remark    VARCHAR(1000) NOT NULL DEFAULT '',
    executed_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_supervision_batch (batch_id),
    CONSTRAINT fk_supervision_batch FOREIGN KEY (batch_id) REFERENCES destruction_batches (id),
    CONSTRAINT fk_supervisor1 FOREIGN KEY (supervisor1_id) REFERENCES officers (id),
    CONSTRAINT fk_supervisor2 FOREIGN KEY (supervisor2_id) REFERENCES officers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS custody_records (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    evidence_id  BIGINT       NOT NULL,
    action       VARCHAR(16)  NOT NULL,
    from_officer BIGINT       NULL,
    to_officer   BIGINT       NULL,
    remark       VARCHAR(500) NOT NULL DEFAULT '',
    occurred_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_custody_evidence (evidence_id),
    CONSTRAINT fk_custody_evidence FOREIGN KEY (evidence_id) REFERENCES evidence (id),
    CONSTRAINT fk_custody_from FOREIGN KEY (from_officer) REFERENCES officers (id),
    CONSTRAINT fk_custody_to FOREIGN KEY (to_officer) REFERENCES officers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS firearms (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    serial_no   VARCHAR(48)  NOT NULL,
    model       VARCHAR(64)  NOT NULL,
    type        VARCHAR(32)  NOT NULL DEFAULT 'PISTOL',
    caliber     VARCHAR(32)  NOT NULL DEFAULT '',
    status      VARCHAR(16)  NOT NULL DEFAULT 'IN_STORE',
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_firearm_serial (serial_no),
    KEY idx_firearm_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS firearm_issuances (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    firearm_id    BIGINT      NOT NULL,
    officer_id    BIGINT      NOT NULL,
    purpose       VARCHAR(255) NOT NULL DEFAULT '',
    ammo_issued   INT         NOT NULL DEFAULT 0,
    ammo_returned INT         NULL,
    issued_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    due_at        DATETIME(3) NOT NULL,
    returned_at   DATETIME(3) NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'ISSUED',
    PRIMARY KEY (id),
    KEY idx_issuance_firearm (firearm_id),
    KEY idx_issuance_officer (officer_id),
    KEY idx_issuance_status (status),
    CONSTRAINT fk_issuance_firearm FOREIGN KEY (firearm_id) REFERENCES firearms (id),
    CONSTRAINT fk_issuance_officer FOREIGN KEY (officer_id) REFERENCES officers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
