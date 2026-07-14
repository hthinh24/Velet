-- ============================================================
-- PAYMENT SERVICE SCHEMA
-- ============================================================

CREATE TYPE payment_status AS ENUM ('IN_PROGRESS', 'COMPLETED', 'CANCELLED');

CREATE TYPE voucher_funded_by AS ENUM ('PLATFORM', 'MERCHANT');

CREATE TYPE cancel_reason AS ENUM ('PAYMENT_FAILED', 'PAYMENT_TIMEOUT');

-- ------------------------------------------------------------
-- PAYMENT
-- ------------------------------------------------------------
CREATE TABLE payments
(
    id                BIGSERIAL PRIMARY KEY,
    idempotency_key   VARCHAR(64)    NOT NULL UNIQUE,
    user_id           BIGINT         NOT NULL,
    merchant_id       BIGINT         NOT NULL,
    status            payment_status NOT NULL DEFAULT 'IN_PROGRESS',
    -- Original price before any discount
    original_price    BIGINT         NOT NULL,
    -- Voucher (optional)
    voucher_id        BIGINT,
    voucher_discount  BIGINT         NOT NULL DEFAULT 0,
    voucher_funded_by voucher_funded_by,
    -- Loyalty coin redemption amount
    coin_amount       BIGINT         NOT NULL DEFAULT 0,
    -- MDR snapshot at transaction time
    mdr_rate          NUMERIC(6, 4),                     -- Ex: 0.0150 = 1.5%
    mdr_fee           BIGINT,
    -- Final computed amounts
    final_price       BIGINT,                            -- amount actually charged to the user
    merchant_net      BIGINT,                            -- amount credited to the merchant
    system_subsidy    BIGINT         NOT NULL DEFAULT 0, -- amount covered by the platform

    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    completed_at      TIMESTAMPTZ,

    cancel_reason     cancel_reason,
    cancelled_at      TIMESTAMPTZ,

    version           BIGINT         NOT NULL DEFAULT 1
);

CREATE INDEX idx_payment_reconciliation
    ON payments (created_at)
    WHERE status = 'IN_PROGRESS';

CREATE INDEX idx_payment_user ON payments (user_id, created_at);
CREATE INDEX idx_payment_merchant ON payments (merchant_id, created_at);


-- ============================================================
-- OUTBOX
-- ============================================================

CREATE TYPE aggregate_type AS ENUM ('PAYMENT');

CREATE TYPE event_type AS ENUM ('PAYMENT_CONFIRMED', 'PAYMENT_CANCELLED');

CREATE TYPE outbox_status AS ENUM ('PENDING', 'PROCESSING', 'SENT', 'FAILED');

CREATE TABLE outbox
(
    id             BIGSERIAL PRIMARY KEY,
    aggregate_id   BIGINT         NOT NULL, -- payment.id
    aggregate_type aggregate_type NOT NULL,
    event_type     event_type     NOT NULL,
    payload        JSONB          NOT NULL,
    status         outbox_status  NOT NULL DEFAULT 'PENDING',
    retry_count    INT            NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    sent_at        TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status ON outbox (status) WHERE status = 'PENDING';
CREATE INDEX idx_outbox_created_at ON outbox (created_at);

-- ------------------------------------------------------------
-- PROCESSED_EVENT
-- Idempotency tracking for events processed from outbox
-- ------------------------------------------------------------
CREATE TABLE processed_event
(
    event_id     BIGINT PRIMARY KEY,
    status       VARCHAR(20) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

-- ============================================================
-- SAMPLE DATA — outbox.payload for event_type = PAYMENT_CONFIRMED
-- ============================================================
--
-- {
--   "saga_id": 1001,
--   "user_id": 123,
--   "merchant_id": 456,
--   "final_price": 90000,
--   "merchant_net": 87000,
--   "mdr_fee": 1500,
--   "system_subsidy": 1500,
--   "voucher_id": 789,
--   "coin_amount": 5000,
--   "voucher_funded_by": "MERCHANT"
-- }
--
-- SAMPLE DATA — outbox.payload for event_type = PAYMENT_CANCELLED
--
-- {
--   "saga_id": 1001,
--   "user_id": 123,
--   "merchant_id": 456
-- }
