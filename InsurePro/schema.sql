-- ================================================================
-- InsurePro - Database Schema (PostgreSQL)
-- Run this script to manually initialize the database.
-- OR: Set spring.jpa.hibernate.ddl-auto=create for auto-creation.
-- ================================================================

-- Create database (run as superuser, skip if already exists)
-- CREATE DATABASE insurepro_db;
-- CREATE USER insurepro_user WITH ENCRYPTED PASSWORD 'insurepro_pass';
-- GRANT ALL PRIVILEGES ON DATABASE insurepro_db TO insurepro_user;

-- ================================================================
-- CONNECT TO insurepro_db before running the rest
-- ================================================================

-- ─────────────────────────────────────────
-- MODULE 1: Identity & Access Management
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
    user_id         BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255)        NOT NULL,
    role            VARCHAR(50)         NOT NULL
                    CHECK (role IN ('POLICYHOLDER','AGENT','BROKER',
                                    'CLAIMS_ADJUSTER','UNDERWRITER',
                                    'COMPLIANCE_OFFICER','ADMIN')),
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255)        NOT NULL,
    phone           VARCHAR(20),
    active          BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP           NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role  ON users (role);

CREATE TABLE IF NOT EXISTS audit_logs (
    audit_id        BIGSERIAL PRIMARY KEY,
    user_id         BIGINT              NOT NULL,
    action          VARCHAR(100)        NOT NULL,
    entity_type     VARCHAR(100),
    entity_id       BIGINT,
    timestamp       TIMESTAMP           NOT NULL DEFAULT NOW(),
    ip_address      VARCHAR(50),
    details         TEXT
    -- Immutable: no UPDATE/DELETE allowed in production. Enforce via DB role permissions.
);

CREATE INDEX idx_audit_user_id    ON audit_logs (user_id);
CREATE INDEX idx_audit_action     ON audit_logs (action);
CREATE INDEX idx_audit_entity     ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_timestamp  ON audit_logs (timestamp);

-- ─────────────────────────────────────────
-- MODULE 2: Customer & Policy Management
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS customers (
    customer_id         BIGSERIAL PRIMARY KEY,
    name                VARCHAR(255)    NOT NULL,
    dob                 DATE            NOT NULL,
    email               VARCHAR(255)    UNIQUE NOT NULL,
    phone               VARCHAR(20),
    address             TEXT,
    kyc_status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                        CHECK (kyc_status IN ('PENDING','VERIFIED','REJECTED')),
    kyc_document_ref    VARCHAR(500),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_email      ON customers (email);
CREATE INDEX idx_customers_kyc_status ON customers (kyc_status);
CREATE INDEX idx_customers_name       ON customers (name);

CREATE TABLE IF NOT EXISTS policies (
    policy_id           BIGSERIAL PRIMARY KEY,
    customer_id         BIGINT          NOT NULL REFERENCES customers(customer_id),
    policy_type         VARCHAR(50)     NOT NULL
                        CHECK (policy_type IN ('HEALTH','LIFE','AUTO','HOME','TRAVEL','LIABILITY')),
    coverage_amount     NUMERIC(15,2)   NOT NULL,
    premium_amount      NUMERIC(10,2)   NOT NULL,
    start_date          DATE            NOT NULL,
    end_date            DATE            NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT'
                        CHECK (status IN ('DRAFT','ACTIVE','EXPIRED','CANCELLED','PENDING_RENEWAL')),
    approved_by         BIGINT,         -- underwriter user_id
    terms_and_conditions TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_policy_dates CHECK (end_date > start_date)
);

CREATE INDEX idx_policies_customer_id ON policies (customer_id);
CREATE INDEX idx_policies_status      ON policies (status);
CREATE INDEX idx_policies_end_date    ON policies (end_date);

-- ─────────────────────────────────────────
-- MODULE 3: Claims Management
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS claims (
    claim_id                BIGSERIAL PRIMARY KEY,
    policy_id               BIGINT          NOT NULL REFERENCES policies(policy_id),
    customer_id             BIGINT          NOT NULL REFERENCES customers(customer_id),
    claim_date              DATE            NOT NULL,
    claim_type              VARCHAR(50)     NOT NULL
                            CHECK (claim_type IN ('HEALTH','ACCIDENT','FIRE','THEFT',
                                                  'NATURAL_DISASTER','LIABILITY','DEATH')),
    status                  VARCHAR(30)     NOT NULL DEFAULT 'SUBMITTED'
                            CHECK (status IN ('SUBMITTED','UNDER_REVIEW','DOCUMENTS_REQUIRED',
                                              'VALIDATED','APPROVED','REJECTED','SETTLED','CLOSED')),
    amount_requested        NUMERIC(15,2)   NOT NULL,
    description             TEXT,
    document_paths          VARCHAR(1000),
    assigned_adjuster_id    BIGINT,
    rejection_reason        TEXT,
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_claims_customer_id   ON claims (customer_id);
CREATE INDEX idx_claims_policy_id     ON claims (policy_id);
CREATE INDEX idx_claims_status        ON claims (status);
CREATE INDEX idx_claims_claim_date    ON claims (claim_date);
CREATE INDEX idx_claims_adjuster      ON claims (assigned_adjuster_id);

CREATE TABLE IF NOT EXISTS settlements (
    settlement_id       BIGSERIAL PRIMARY KEY,
    claim_id            BIGINT          NOT NULL UNIQUE REFERENCES claims(claim_id),
    amount_approved     NUMERIC(15,2)   NOT NULL,
    approved_by         BIGINT          NOT NULL,   -- adjuster user_id
    settlement_date     DATE            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','PROCESSING','PAID','FAILED')),
    payment_reference   VARCHAR(255),
    notes               TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_settlements_claim_id ON settlements (claim_id);
CREATE INDEX idx_settlements_status   ON settlements (status);

-- ─────────────────────────────────────────
-- MODULE 4: Premium & Payment Management
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS invoices (
    invoice_id              BIGSERIAL PRIMARY KEY,
    policy_id               BIGINT          NOT NULL REFERENCES policies(policy_id),
    customer_id             BIGINT          NOT NULL REFERENCES customers(customer_id),
    amount                  NUMERIC(12,2)   NOT NULL,
    invoice_date            DATE            NOT NULL,
    due_date                DATE            NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'DRAFT'
                            CHECK (status IN ('DRAFT','ISSUED','PAID','OVERDUE','CANCELLED')),
    billing_period_start    DATE,
    billing_period_end      DATE,
    notes                   VARCHAR(500),
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoices_customer_id ON invoices (customer_id);
CREATE INDEX idx_invoices_policy_id   ON invoices (policy_id);
CREATE INDEX idx_invoices_status      ON invoices (status);
CREATE INDEX idx_invoices_due_date    ON invoices (due_date);

CREATE TABLE IF NOT EXISTS payments (
    payment_id              BIGSERIAL PRIMARY KEY,
    invoice_id              BIGINT          NOT NULL REFERENCES invoices(invoice_id),
    amount                  NUMERIC(12,2)   NOT NULL,
    payment_date            DATE            NOT NULL,
    method                  VARCHAR(30)     NOT NULL
                            CHECK (method IN ('CREDIT_CARD','DEBIT_CARD','NET_BANKING',
                                              'UPI','BANK_TRANSFER','CHEQUE')),
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING','PROCESSING','SUCCESS','FAILED','REFUNDED')),
    transaction_reference   VARCHAR(255),
    gateway_name            VARCHAR(100),
    failure_reason          VARCHAR(500),
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_invoice_id  ON payments (invoice_id);
CREATE INDEX idx_payments_status      ON payments (status);
CREATE INDEX idx_payments_txn_ref     ON payments (transaction_reference);

-- ─────────────────────────────────────────
-- MODULE 5: Compliance & Audit Management
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS compliance_reports (
    report_id           BIGSERIAL PRIMARY KEY,
    report_type         VARCHAR(50)     NOT NULL,
    scope               VARCHAR(50)     NOT NULL,
    metrics             TEXT            NOT NULL,
    generated_by_email  VARCHAR(255)    NOT NULL,
    generated_date      TIMESTAMP       NOT NULL DEFAULT NOW(),
    period_start        TIMESTAMP,
    period_end          TIMESTAMP,
    export_file_path    VARCHAR(500),
    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT'
);

CREATE INDEX idx_compliance_reports_type   ON compliance_reports (report_type);
CREATE INDEX idx_compliance_reports_date   ON compliance_reports (generated_date);

-- ─────────────────────────────────────────
-- MODULE 6: Analytics & Reporting
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS kpi_reports (
    report_id       BIGSERIAL PRIMARY KEY,
    scope           VARCHAR(50)     NOT NULL,
    metrics         TEXT            NOT NULL,
    generated_date  TIMESTAMP       NOT NULL DEFAULT NOW(),
    period_start    TIMESTAMP,
    period_end      TIMESTAMP
);

CREATE INDEX idx_kpi_reports_scope ON kpi_reports (scope);
CREATE INDEX idx_kpi_reports_date  ON kpi_reports (generated_date);

-- ─────────────────────────────────────────
-- MODULE 7: Notifications & Alerts
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS notifications (
    notification_id     BIGSERIAL PRIMARY KEY,
    user_id             BIGINT,
    recipient_email     VARCHAR(255),
    subject             VARCHAR(255)    NOT NULL,
    message             VARCHAR(500)    NOT NULL,
    category            VARCHAR(50)     NOT NULL
                        CHECK (category IN ('CLAIM_UPDATE','PAYMENT_REMINDER',
                                            'PAYMENT_CONFIRMATION','POLICY_RENEWAL',
                                            'POLICY_EXPIRY','KYC_UPDATE',
                                            'COMPLIANCE_ALERT','SYSTEM')),
    status              VARCHAR(20)     NOT NULL DEFAULT 'UNREAD',
    channels            VARCHAR(100)    NOT NULL DEFAULT 'INAPP,EMAIL',
    created_date        TIMESTAMP       NOT NULL DEFAULT NOW(),
    read_at             TIMESTAMP
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_status  ON notifications (status);
CREATE INDEX idx_notifications_date    ON notifications (created_date);

-- ================================================================
-- SEED DATA: Default Admin User
-- Password = "Admin@123" (BCrypt hash — change in production!)
-- ================================================================

INSERT INTO users (name, role, email, password_hash, phone, active)
VALUES (
    'System Admin',
    'ADMIN',
    'admin@insurepro.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    '+91-9999999999',
    TRUE
) ON CONFLICT (email) DO NOTHING;
