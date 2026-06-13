-- ============================================================
-- Velet — Wallet Service Schema
-- Convention: amounts stored as bigint (VND, no decimals)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------
-- ACCOUNT
-- Represents a wallet owned by a user, merchant, or system
-- ------------------------------------------------------------
CREATE TYPE account_type   AS ENUM ('USER', 'MERCHANT', 'SYSTEM');
CREATE TYPE account_status AS ENUM ('ACTIVE', 'FROZEN', 'CLOSED');

CREATE TABLE accounts (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id          UUID         NOT NULL,                        -- ref to Identity Service (no FK across services)
    type              account_type NOT NULL DEFAULT 'USER',
    currency          VARCHAR(3)   NOT NULL DEFAULT 'VND',
    available_balance BIGINT       NOT NULL DEFAULT 0 CHECK (available_balance >= 0),
    pending_balance   BIGINT       NOT NULL DEFAULT 0 CHECK (pending_balance   >= 0),
    status            account_status NOT NULL DEFAULT 'ACTIVE',
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_owner_id ON accounts (owner_id);

-- ------------------------------------------------------------
-- TRANSACTION
-- Records money movement between two accounts
-- ------------------------------------------------------------
CREATE TYPE transaction_type   AS ENUM ('TRANSFER', 'TOPUP', 'WITHDRAW', 'PAYMENT', 'BILL_PAYMENT');
CREATE TYPE transaction_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'REVERSED');

CREATE TABLE transactions (
    id                     UUID               PRIMARY KEY DEFAULT gen_random_uuid(),
    source_account_id      UUID               NOT NULL REFERENCES accounts (id),
    destination_account_id UUID               NOT NULL REFERENCES accounts (id),
    amount                 BIGINT             NOT NULL CHECK (amount > 0),
    currency               VARCHAR(3)         NOT NULL DEFAULT 'VND',
    type                   transaction_type   NOT NULL,
    status                 transaction_status NOT NULL DEFAULT 'PENDING',
    idempotency_key        UUID               NOT NULL UNIQUE,
    created_at             TIMESTAMPTZ        NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ        NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_source      ON transactions (source_account_id);
CREATE INDEX idx_transactions_destination ON transactions (destination_account_id);
CREATE INDEX idx_transactions_created_at  ON transactions (created_at);

-- ------------------------------------------------------------
-- LEDGER_ENTRY
-- Double-entry bookkeeping: every transaction generates 2 entries
-- Append-only — never UPDATE or DELETE
-- ------------------------------------------------------------
CREATE TYPE entry_type AS ENUM ('DEBIT', 'CREDIT');

CREATE TABLE ledger_entries (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id   UUID        NOT NULL REFERENCES transactions (id),
    account_id       UUID        NOT NULL REFERENCES accounts (id),
    entry_type       entry_type  NOT NULL,
    amount           BIGINT      NOT NULL CHECK (amount > 0),
    running_balance  BIGINT      NOT NULL,                          -- balance after this entry
    idempotency_key  UUID        NOT NULL UNIQUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ledger_account_id      ON ledger_entries (account_id);
CREATE INDEX idx_ledger_transaction_id  ON ledger_entries (transaction_id);
CREATE INDEX idx_ledger_created_at      ON ledger_entries (created_at);

-- ------------------------------------------------------------
-- ORDER
-- Payment context: aggregates transaction + loyalty usage
-- ------------------------------------------------------------
CREATE TYPE order_type   AS ENUM ('PAYMENT', 'BILL_PAYMENT');
CREATE TYPE order_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED');

CREATE TABLE orders (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id       UUID         REFERENCES transactions (id),
    type                 order_type   NOT NULL,
    gross_amount         BIGINT       NOT NULL CHECK (gross_amount > 0),
    discount_amount      BIGINT       NOT NULL DEFAULT 0,
    final_amount         BIGINT       NOT NULL CHECK (final_amount >= 0),
    status               order_status NOT NULL DEFAULT 'PENDING',
    voucher_instance_id  UUID,                                        -- ref to Loyalty Service (no FK)
    points_used          BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_transaction_id ON orders (transaction_id);
CREATE INDEX idx_orders_status         ON orders (status);

-- ------------------------------------------------------------
-- BILL
-- Extension of ORDER for utility bill payments
-- Stores provider-specific context
-- ------------------------------------------------------------
CREATE TYPE bill_status AS ENUM ('UNPAID', 'PAID');

CREATE TABLE bills (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id      UUID        NOT NULL UNIQUE REFERENCES orders (id),
    provider_code VARCHAR(20) NOT NULL,                              -- 'EVN', 'VNPT', 'FPT', ...
    bill_ref_no   VARCHAR(100) NOT NULL,                            -- bill reference number from provider
    period        VARCHAR(20),                                       -- billing period, e.g., '2024-05'
    amount        BIGINT      NOT NULL CHECK (amount > 0),
    status        bill_status NOT NULL DEFAULT 'UNPAID',
    due_date      TIMESTAMPTZ,
    paid_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bills_order_id      ON bills (order_id);
CREATE INDEX idx_bills_provider_code ON bills (provider_code);
CREATE INDEX idx_bills_bill_ref_no   ON bills (bill_ref_no);

-- ------------------------------------------------------------
-- OUTBOX
-- Transactional outbox pattern
-- ------------------------------------------------------------
CREATE TYPE outbox_status AS ENUM ('PENDING', 'SENT', 'FAILED');

CREATE TABLE outbox (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id   UUID         NOT NULL,                            -- transaction_id or order_id
    aggregate_type VARCHAR(50)  NOT NULL,                            -- 'TRANSACTION' | 'ORDER'
    event_type     VARCHAR(100) NOT NULL,                            -- 'payment.completed', 'order.created'
    payload        JSONB        NOT NULL,
    status         outbox_status NOT NULL DEFAULT 'PENDING',
    retry_count    INT          NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    sent_at        TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status     ON outbox (status) WHERE status = 'PENDING';
CREATE INDEX idx_outbox_created_at ON outbox (created_at);