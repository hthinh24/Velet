-- ========================================================
-- 1. SEED 50 USER WALLETS (id: 1011 -> 1060, owner_id: 11 -> 60)
-- ========================================================
INSERT INTO wallets (id, owner_id, type, currency, status)
SELECT
    i AS id,
    (i - 1000) AS owner_id,
    'USER_WALLET' AS type,
    'VND' AS currency,
    'ACTIVE' AS status
FROM generate_series(1011, 1110) AS i;

-- ========================================================
-- 2. CREATE TRANSACTIONS (Top-up 50M VND from System Wallet 1)
-- id: 1001 -> 1050
-- ========================================================
INSERT INTO transactions (id, source_wallet_id, destination_wallet_id, amount, currency, type, status, idempotency_key)
SELECT
    (100001 + (i - 1011)) AS id,           -- transaction_id from 100001 to 100050
    1 AS source_wallet_id,                 -- Equity Capital wallet_id = 1
    i AS destination_wallet_id,            -- wallet_id from 1011 to 1060
    50000000 AS amount,                    -- 50M VND
    'VND' AS currency,
    'TOPUP' AS type,
    'SUCCESS' AS status,
    'benchmark-seed-topup-' || i AS idempotency_key
FROM generate_series(1011, 1110) AS i;


-- ========================================================
-- 3. CREATE DOUBLE-ENTRY LEDGER ENTRIES
-- DEBIT (Wallet 1) & CREDIT (User Wallet)
-- ========================================================
-- DEBIT Entries (Wallet 1)
INSERT INTO ledger_entries (transaction_id, wallet_id, entry_type, amount, status, idempotency_key)
SELECT
    (100001 + (i - 1011)) AS transaction_id,
    1 AS wallet_id,
    'DEBIT' AS entry_type,
    50000000 AS amount,
    'POSTED' AS status,
    'benchmark-ledger-debit-' || i AS idempotency_key
FROM generate_series(1011, 1110) AS i;

-- CREDIT Entries (User Wallets 1011 -> 1060)
INSERT INTO ledger_entries (transaction_id, wallet_id, entry_type, amount, status, idempotency_key)
SELECT
    (100001 + (i - 1011)) AS transaction_id,
    i AS wallet_id,
    'CREDIT' AS entry_type,
    50000000 AS amount,
    'POSTED' AS status,
    'benchmark-ledger-credit-' || i AS idempotency_key
FROM generate_series(1011, 1110) AS i;