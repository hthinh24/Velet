-- ============================================================
-- Velet — Wallet Service Schema
-- Convention: amounts stored as bigint (VND, no decimals)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------
-- WALLET
-- Represents a wallet owned by a user, merchant, or system
-- ------------------------------------------------------------
CREATE TYPE wallet_type AS ENUM('USER', 'MERCHANT', 'SYSTEM');
CREATE TYPE wallet_status AS ENUM('ACTIVE', 'FROZEN', 'CLOSED');

CREATE TABLE wallets
(
    id                BIGSERIAL PRIMARY KEY,
    owner_id          BIGINT        NOT NULL, -- ref to Identity Service (no FK across services)
    type              wallet_type   NOT NULL DEFAULT 'USER',
    currency          VARCHAR(3)    NOT NULL DEFAULT 'VND',
    available_balance BIGINT        NOT NULL DEFAULT 0 CHECK (available_balance >= 0),
    pending_balance   BIGINT        NOT NULL DEFAULT 0 CHECK (pending_balance >= 0),
    status            wallet_status NOT NULL DEFAULT 'ACTIVE',
    version           BIGINT        NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_wallets_owner_id ON wallets (owner_id);

-- ------------------------------------------------------------
-- TRANSACTION
-- Records money movement between two wallets
-- ------------------------------------------------------------
CREATE TYPE transaction_type AS ENUM('TRANSFER', 'TOPUP', 'WITHDRAW', 'PAYMENT', 'BILL_PAYMENT');
CREATE TYPE transaction_status AS ENUM('PENDING', 'SUCCESS', 'FAILED', 'REVERSED');

CREATE TABLE transactions
(
    id                    BIGSERIAL PRIMARY KEY,
    source_wallet_id      BIGINT             NOT NULL REFERENCES wallets (id),
    destination_wallet_id BIGINT             NOT NULL REFERENCES wallets (id),
    amount                BIGINT             NOT NULL CHECK (amount > 0),
    currency              VARCHAR(3)         NOT NULL DEFAULT 'VND',
    type                  transaction_type   NOT NULL,
    status                transaction_status NOT NULL DEFAULT 'PENDING',
    idempotency_key       varchar(64)        NOT NULL UNIQUE,
    created_at            TIMESTAMPTZ        NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ        NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_source ON transactions (source_wallet_id);
CREATE INDEX idx_transactions_destination ON transactions (destination_wallet_id);
CREATE INDEX idx_transactions_created_at ON transactions (created_at);

-- ------------------------------------------------------------
-- TRANSACTION
-- Records money movement between two wallets
-- ------------------------------------------------------------
CREATE TABLE balance_reservations
(
    id              BIGSERIAL PRIMARY KEY,
    wallet_id       BIGINT    NOT NULL,
    amount          BIGINT    NOT NULL,
    idempotency_key VARCHAR   NOT NULL UNIQUE,
    status          VARCHAR   NOT NULL, -- RESERVED | COMPLETED | RELEASED
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- ------------------------------------------------------------
-- LEDGER_ENTRY
-- Double-entry bookkeeping: every transaction generates 2 entries
-- Append-only — never UPDATE or DELETE
-- ------------------------------------------------------------
CREATE TYPE entry_type AS ENUM('DEBIT', 'CREDIT');

CREATE TABLE ledger_entries
(
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT      NOT NULL REFERENCES transactions (id),
    wallet_id       BIGINT      NOT NULL REFERENCES wallets (id),
    entry_type      entry_type  NOT NULL,
    amount          BIGINT      NOT NULL CHECK (amount > 0),
    running_balance BIGINT      NOT NULL, -- balance after this entry
    idempotency_key varchar(64) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ledger_wallet_id ON ledger_entries (wallet_id);
CREATE INDEX idx_ledger_transaction_id ON ledger_entries (transaction_id);
CREATE INDEX idx_ledger_created_at ON ledger_entries (created_at);

-- ------------------------------------------------------------
-- ORDER
-- Payment context: aggregates transaction + loyalty usage
-- ------------------------------------------------------------
CREATE TYPE order_type AS ENUM('PAYMENT', 'BILL_PAYMENT');
CREATE TYPE order_status AS ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED');

CREATE TABLE orders
(
    id                  BIGSERIAL PRIMARY KEY,
    transaction_id      BIGINT REFERENCES transactions (id),
    type                order_type   NOT NULL,
    gross_amount        BIGINT       NOT NULL CHECK (gross_amount > 0),
    discount_amount     BIGINT       NOT NULL DEFAULT 0,
    final_amount        BIGINT       NOT NULL CHECK (final_amount >= 0),
    status              order_status NOT NULL DEFAULT 'PENDING',
    voucher_instance_id BIGINT, -- ref to Loyalty Service (no FK)
    points_used         BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_transaction_id ON orders (transaction_id);
CREATE INDEX idx_orders_status ON orders (status);

-- ------------------------------------------------------------
-- BILL
-- Extension of ORDER for utility bill payments
-- Stores provider-specific context
-- ------------------------------------------------------------
CREATE TYPE bill_status AS ENUM('UNPAID', 'PAID');

CREATE TABLE bills
(
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT       NOT NULL UNIQUE REFERENCES orders (id),
    provider_code VARCHAR(20)  NOT NULL, -- 'EVN', 'VNPT', 'FPT', ...
    bill_ref_no   VARCHAR(100) NOT NULL, -- bill reference number from provider
    period VARCHAR(20
) ,           -- billing period, e.g., '2024-05'
    amount        BIGINT       NOT NULL CHECK (amount > 0),
    status        bill_status  NOT NULL DEFAULT 'UNPAID',
    due_date      TIMESTAMPTZ,
    paid_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_bills_order_id ON bills (order_id);
CREATE INDEX idx_bills_provider_code ON bills (provider_code);
CREATE INDEX idx_bills_bill_ref_no ON bills (bill_ref_no);

-- ------------------------------------------------------------
-- OUTBOX
-- Transactional outbox pattern
-- ------------------------------------------------------------
CREATE TYPE outbox_status AS ENUM('PENDING', 'SENT', 'FAILED');

CREATE TABLE outbox
(
    id             BIGSERIAL PRIMARY KEY,
    aggregate_id   BIGINT        NOT NULL, -- transaction_id or order_id
    aggregate_type VARCHAR(50)   NOT NULL, -- 'TRANSACTION' | 'ORDER'
    event_type     VARCHAR(100)  NOT NULL, -- 'payment.completed', 'order.created'
    payload        JSONB         NOT NULL,
    status         outbox_status NOT NULL DEFAULT 'PENDING',
    retry_count    INT           NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    sent_at        TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status ON outbox (status) WHERE status = 'PENDING';
CREATE INDEX idx_outbox_created_at ON outbox (created_at);