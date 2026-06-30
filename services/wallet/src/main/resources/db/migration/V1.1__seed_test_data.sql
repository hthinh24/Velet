-- ==========================================
-- WALLET SERVICE SEED DATA
-- ==========================================

-- ==========================================
-- SYSTEM WALLETS
-- ==========================================
INSERT INTO wallets (id, owner_id, category, type, currency, status)
VALUES
    -- EQUITY (id 1 - 10)
    (1, 0, 'EQUITY', 'EQUITY_CAPITAL', 'VND', 'ACTIVE'),
    (2, 0, 'EQUITY', 'REVENUE_MDR', 'VND', 'ACTIVE'),
    (3, 0, 'EQUITY', 'MARKETING_PROMO_BUDGET', 'VND', 'ACTIVE'),

    -- ASSET (id 11 - 50)
    (11, 0, 'ASSET', 'BANK_VAULT', 'VND', 'ACTIVE'),             -- Vietcombank (VCB)
    (12, 0, 'ASSET', 'BANK_VAULT', 'VND', 'ACTIVE'),             -- BIDV
    (13, 0, 'ASSET', 'BANK_VAULT', 'VND', 'ACTIVE'),             -- VietinBank (CTG)
    (14, 0, 'ASSET', 'BANK_VAULT', 'VND', 'ACTIVE'),             -- Agribank
    (15, 0, 'ASSET', 'BANK_VAULT', 'VND', 'ACTIVE'),             -- Techcombank (TCB)

    -- INTERNAL_TECHNICAL (id 51 - 100)
    -- Reserve account for P2P transfers
    (51, 0, 'INTERNAL_TECHNICAL', 'RESERVE_ACCOUNT', 'VND', 'ACTIVE'),

    -- LIABILITY (id 101 - 1000)
    -- PENDING transactions (e.g., topup, withdraw, transfer) are reserved here until they are completed
    (101, 0, 'LIABILITY', 'SUSPENSE_ACCOUNT', 'VND', 'ACTIVE');

-- ==========================================
-- 1. WALLETS (10 records - one per user)
-- ==========================================

-- System reserve wallet — holds funds earmarked by reserve(); never used for P2P transfers
INSERT INTO wallets (id, owner_id, type, currency, status)
VALUES (1001, 1, 'USER_WALLET', 'VND', 'ACTIVE'), -- User 1
       (1002, 2, 'USER_WALLET', 'VND', 'ACTIVE'), -- User 2
       (1003, 3, 'USER_WALLET', 'VND', 'ACTIVE'), -- User 3
       (1004, 4, 'USER_WALLET', 'VND', 'ACTIVE'), -- User 4
       (1005, 5, 'USER_WALLET', 'VND', 'ACTIVE'), -- User 5
       (1006, 6, 'USER_WALLET', 'VND', 'ACTIVE'), -- User 6
       (1007, 7, 'USER_WALLET', 'VND', 'ACTIVE'), -- User 7
       (1008, 8, 'USER_WALLET', 'VND', 'ACTIVE'), -- User 8
       (1009, 9, 'USER_WALLET', 'VND', 'ACTIVE'), -- User 9: no transactions yet
       (1010, 10, 'USER_WALLET', 'VND', 'ACTIVE');-- User 10: no transactions yet


-- ==========================================
-- 2. TRANSACTIONS
-- Pattern: Mix of transfers, topups, withdrawals and payments
-- ==========================================

-- SYSTEM TOP-UPS (5 records - 2B VND each)
INSERT INTO transactions (id, source_wallet_id, destination_wallet_id, amount, currency, type, status, idempotency_key)
VALUES
    (101, 1, 11, 2000000000, 'VND', 'TOPUP', 'SUCCESS', 'sys-init-vcb-2b'),
    (102, 1, 12, 2000000000, 'VND', 'TOPUP', 'SUCCESS', 'sys-init-bidv-2b'),
    (103, 1, 13, 2000000000, 'VND', 'TOPUP', 'SUCCESS', 'sys-init-ctg-2b'),
    (104, 1, 14, 2000000000, 'VND', 'TOPUP', 'SUCCESS', 'sys-init-agri-2b'),
    (105, 1, 15, 2000000000, 'VND', 'TOPUP', 'SUCCESS', 'sys-init-tcb-2b');

-- USER TOP-UPS (10 records - 50M VND each)
INSERT INTO transactions (id, source_wallet_id, destination_wallet_id, amount, currency, type, status, idempotency_key)
VALUES
    (201, 11, 1001, 50000000, 'VND', 'TOPUP', 'SUCCESS', 'user-topup-1001'),
    (202, 11, 1002, 50000000, 'VND', 'TOPUP', 'SUCCESS', 'user-topup-1002'),
    (203, 11, 1003, 50000000, 'VND', 'TOPUP', 'SUCCESS', 'user-topup-1003'),
    (204, 11, 1004, 50000000, 'VND', 'TOPUP', 'SUCCESS', 'user-topup-1004'),
    (205, 11, 1005, 50000000, 'VND', 'TOPUP', 'SUCCESS', 'user-topup-1005'),
    (206, 11, 1006, 50000000, 'VND', 'TOPUP', 'SUCCESS', 'user-topup-1006'),
    (207, 11, 1007, 50000000, 'VND', 'TOPUP', 'SUCCESS', 'user-topup-1007'),
    (208, 11, 1008, 50000000, 'VND', 'TOPUP', 'SUCCESS', 'user-topup-1008'),
    (209, 11, 1009, 50000000, 'VND', 'TOPUP', 'SUCCESS', 'user-topup-1009'),
    (210, 11, 1010, 50000000, 'VND', 'TOPUP', 'SUCCESS', 'user-topup-1010');

-- USER TRANSACTIONS (20 records - mix of transfers, payments, and bill payments)
-- Amounts: 100k - 1M VND
INSERT INTO transactions (source_wallet_id, destination_wallet_id, amount, currency, type, status, idempotency_key)
VALUES
    -- Tx 1: User 1 → User 2 (250k movie ticket)
    (1001, 1002, 250000, 'VND', 'TRANSFER', 'SUCCESS', 'a1b2c3d4-e5f6-47d8-90a1-bb2cc3dd4ee1'),

    -- Tx 2: User 2 → User 3 (150k transfer)
    (1002, 1003, 150000, 'VND', 'TRANSFER', 'SUCCESS', 'b2c3d4e5-f6a7-48e9-91b2-cc3dd4ee5ff2'),

    -- Tx 3: User 3 → User 4 (500k internet bill)
    (1003, 1004, 500000, 'VND', 'BILL_PAYMENT', 'SUCCESS', 'c3d4e5f6-a7b8-49f0-92c3-dd4ee5ff6002'),

    -- Tx 4: User 4 → User 5 (200k transfer)
    (1004, 1005, 200000, 'VND', 'TRANSFER', 'SUCCESS', 'd4e5f6a7-b8c9-40a1-93d4-ee5ff6001011'),

    -- Tx 5: User 1 → User 5 (300k movie ticket)
    (1001, 1005, 300000, 'VND', 'TRANSFER', 'SUCCESS', 'e5f6a7b8-c9d0-41b2-94e5-f60010113222'),

    -- Tx 6: User 2 → User 4 (100k transfer)
    (1002, 1004, 100000, 'VND', 'TRANSFER', 'SUCCESS', 'f6a7b8c9-d0e1-42c3-95f6-001011322433'),

    -- Tx 7: User 5 → User 6 (750k electric bill)
    (1005, 1006, 750000, 'VND', 'BILL_PAYMENT', 'SUCCESS', 'a7b8c9d0-e1f2-43d4-96a7-010113224335'),

    -- Tx 8: User 6 → User 7 (180k transfer)
    (1006, 1007, 180000, 'VND', 'TRANSFER', 'SUCCESS', 'b8c9d0e1-f2a3-44e5-97b8-101132243366'),

    -- Tx 9: User 3 → User 7 (420k transfer)
    (1003, 1007, 420000, 'VND', 'TRANSFER', 'SUCCESS', 'c9d0e1f2-a3b4-45f6-98c9-101132243376'),

    -- Tx 10: User 7 → User 8 (600k movie ticket)
    (1007, 1008, 600000, 'VND', 'TRANSFER', 'SUCCESS', 'd0e1f2a3-b4c5-46a7-99d0-101132243387'),

    -- Tx 11: User 1 → User 6 (850k water bill)
    (1001, 1006, 850000, 'VND', 'BILL_PAYMENT', 'SUCCESS', 'e1f2a3b4-c5d6-47b8-90e1-101132243398'),

    -- Tx 12: User 4 → User 8 (350k transfer)
    (1004, 1008, 350000, 'VND', 'TRANSFER', 'SUCCESS', 'f2a3b4c5-d6e7-48c9-91f2-101132243399'),

    -- Tx 13: User 2 → User 7 (275k movie ticket)
    (1002, 1007, 275000, 'VND', 'TRANSFER', 'SUCCESS', 'a3b4c5d6-e7f8-49d0-92a3-101132243400'),

    -- Tx 14: User 5 → User 3 (520k transfer)
    (1005, 1003, 520000, 'VND', 'TRANSFER', 'SUCCESS', 'b4c5d6e7-f8a9-40e1-93b4-101132243401'),

    -- Tx 15: User 8 → User 1 (180k transfer)
    (1008, 1001, 180000, 'VND', 'TRANSFER', 'SUCCESS', 'c5d6e7f8-a9b0-41f2-94c5-101132243402'),

    -- Tx 16: User 6 → User 2 (450k transfer)
    (1006, 1002, 450000, 'VND', 'TRANSFER', 'SUCCESS', 'd6e7f8a9-b0c1-42a3-95d6-101132243403'),

    -- Tx 17: User 3 → User 8 (155k transfer)
    (1003, 1008, 155000, 'VND', 'TRANSFER', 'SUCCESS', 'e7f8a9b0-c1d2-43b4-96e7-101132243404'),

    -- Tx 18: User 7 → User 4 (999999 large transfer)
    (1007, 1004, 999999, 'VND', 'TRANSFER', 'SUCCESS', 'f8a9b0c1-d2e3-44c5-97f8-101132243405'),

    -- Tx 19: User 2 → User 6 (345k transfer)
    (1002, 1006, 345000, 'VND', 'TRANSFER', 'SUCCESS', 'a9b0c1d2-e3f4-45d6-98a9-101132243405'),

    -- Tx 20: User 4 → User 3 (678000 transfer)
    (1004, 1003, 678000, 'VND', 'TRANSFER', 'SUCCESS', 'b0c1d2e3-f4a5-46e7-99b0-101132243405');

-- ==========================================
-- 3. LEDGER ENTRIES (40 records = 20 transactions × 2)
-- DEBIT: source wallet loses money
-- CREDIT: destination wallet gains money
-- ==========================================

-- 3.1. Ledger Entries for SYSTEM TOP-UPS (5 records)
INSERT INTO ledger_entries (transaction_id, wallet_id, entry_type, amount, status, idempotency_key)
VALUES
    (101, 11, 'DEBIT',  2000000000, 'POSTED', 'ledger-init-vcb-debit'),
    (101, 1,  'CREDIT', 2000000000, 'POSTED', 'ledger-init-vcb-credit'),

    (102, 12, 'DEBIT',  2000000000, 'POSTED', 'ledger-init-bidv-debit'),
    (102, 1,  'CREDIT', 2000000000, 'POSTED', 'ledger-init-bidv-credit'),

    (103, 13, 'DEBIT',  2000000000, 'POSTED', 'ledger-init-ctg-debit'),
    (103, 1,  'CREDIT', 2000000000, 'POSTED', 'ledger-init-ctg-credit'),

    (104, 14, 'DEBIT',  2000000000, 'POSTED', 'ledger-init-agri-debit'),
    (104, 1,  'CREDIT', 2000000000, 'POSTED', 'ledger-init-agri-credit'),

    (105, 15, 'DEBIT',  2000000000, 'POSTED', 'ledger-init-tcb-debit'),
    (105, 1,  'CREDIT', 2000000000, 'POSTED', 'ledger-init-tcb-credit');

-- 3.2. Ledger Entries for USER TOP-UPS (10 records)
INSERT INTO ledger_entries (transaction_id, wallet_id, entry_type, amount, status, idempotency_key)
VALUES
    (201, 11,   'DEBIT', 50000000, 'POSTED', 'ledger-topup-1001-vcb'),
    (201, 1001, 'CREDIT', 50000000, 'POSTED', 'ledger-topup-1001-user'),

    (202, 11,   'DEBIT', 50000000, 'POSTED', 'ledger-topup-1002-vcb'),
    (202, 1002, 'CREDIT', 50000000, 'POSTED', 'ledger-topup-1002-user'),

    (203, 11,   'DEBIT', 50000000, 'POSTED', 'ledger-topup-1003-vcb'),
    (203, 1003, 'CREDIT', 50000000, 'POSTED', 'ledger-topup-1003-user'),

    (204, 11,   'DEBIT', 50000000, 'POSTED', 'ledger-topup-1004-vcb'),
    (204, 1004, 'CREDIT', 50000000, 'POSTED', 'ledger-topup-1004-user'),

    (205, 11,   'DEBIT', 50000000, 'POSTED', 'ledger-topup-1005-vcb'),
    (205, 1005, 'CREDIT', 50000000, 'POSTED', 'ledger-topup-1005-user'),

    (206, 11,   'DEBIT', 50000000, 'POSTED', 'ledger-topup-1006-vcb'),
    (206, 1006, 'CREDIT', 50000000, 'POSTED', 'ledger-topup-1006-user'),

    (207, 11,   'DEBIT', 50000000, 'POSTED', 'ledger-topup-1007-vcb'),
    (207, 1007, 'CREDIT', 50000000, 'POSTED', 'ledger-topup-1007-user'),

    (208, 11,   'DEBIT', 50000000, 'POSTED', 'ledger-topup-1008-vcb'),
    (208, 1008, 'CREDIT', 50000000, 'POSTED', 'ledger-topup-1008-user'),

    (209, 11,   'DEBIT', 50000000, 'POSTED', 'ledger-topup-1009-vcb'),
    (209, 1009, 'CREDIT', 50000000, 'POSTED', 'ledger-topup-1009-user'),

    (210, 11,   'DEBIT', 50000000, 'POSTED', 'ledger-topup-1010-vcb'),
    (210, 1010, 'CREDIT', 50000000, 'POSTED', 'ledger-topup-1010-user');

-- 3.3. Ledger Entries for USER TRANSACTIONS (20 records × 2 = 40 records)
INSERT INTO ledger_entries (transaction_id, wallet_id, entry_type, amount, idempotency_key)
VALUES
    -- Transaction 1 (250k: User1 → User2)
    (1, 1001, 'DEBIT', 250000, '550e8400-e29b-41d4-a716-446655440001'),
    (1, 1002, 'CREDIT', 250000, '550e8400-e29b-41d4-a716-446655440002'),

    -- Transaction 2 (150k: User2 → User3)
    (2, 1002, 'DEBIT', 150000, '550e8400-e29b-41d4-a716-446655440003'),
    (2, 1003, 'CREDIT', 150000, '550e8400-e29b-41d4-a716-446655440004'),

    -- Transaction 3 (500k: User3 → User4)
    (3, 1003, 'DEBIT', 500000, '550e8400-e29b-41d4-a716-446655440005'),
    (3, 1004, 'CREDIT', 500000, '550e8400-e29b-41d4-a716-446655440006'),

    -- Transaction 4 (200k: User4 → User5)
    (4, 1004, 'DEBIT', 200000, '550e8400-e29b-41d4-a716-446655440007'),
    (4, 1005, 'CREDIT', 200000, '550e8400-e29b-41d4-a716-446655440008'),

    -- Transaction 5 (300k: User1 → User5)
    (5, 1001, 'DEBIT', 300000, '550e8400-e29b-41d4-a716-446655440009'),
    (5, 1005, 'CREDIT', 300000, '550e8400-e29b-41d4-a716-446655440010'),

    -- Transaction 6 (100k: User2 → User4)
    (6, 1002, 'DEBIT', 100000, '550e8400-e29b-41d4-a716-446655440011'),
    (6, 1004, 'CREDIT', 100000, '550e8400-e29b-41d4-a716-446655440012'),

    -- Transaction 7 (750k: User5 → User6)
    (7, 1005, 'DEBIT', 750000, '550e8400-e29b-41d4-a716-446655440013'),
    (7, 1006, 'CREDIT', 750000, '550e8400-e29b-41d4-a716-446655440014'),

    -- Transaction 8 (180k: User6 → User7)
    (8, 1006, 'DEBIT', 180000, '550e8400-e29b-41d4-a716-446655440015'),
    (8, 1007, 'CREDIT', 180000, '550e8400-e29b-41d4-a716-446655440016'),

    -- Transaction 9 (420k: User3 → User7)
    (9, 1003, 'DEBIT', 420000, '550e8400-e29b-41d4-a716-446655440017'),
    (9, 1007, 'CREDIT', 420000, '550e8400-e29b-41d4-a716-446655440018'),

    -- Transaction 10 (600k: User7 → User8)
    (10, 1007, 'DEBIT', 600000, '550e8400-e29b-41d4-a716-446655440019'),
    (10, 1008, 'CREDIT', 600000, '550e8400-e29b-41d4-a716-446655440020'),

    -- Transaction 11 (850k: User1 → User6)
    (11, 1001, 'DEBIT', 850000, '550e8400-e29b-41d4-a716-446655440021'),
    (11, 1006, 'CREDIT', 850000, '550e8400-e29b-41d4-a716-446655440022'),

    -- Transaction 12 (350k: User4 → User8)
    (12, 1004, 'DEBIT', 350000, '550e8400-e29b-41d4-a716-446655440023'),
    (12, 1008, 'CREDIT', 350000, '550e8400-e29b-41d4-a716-446655440024'),

    -- Transaction 13 (275k: User2 → User7)
    (13, 1002, 'DEBIT', 275000, '550e8400-e29b-41d4-a716-446655440025'),
    (13, 1007, 'CREDIT', 275000, '550e8400-e29b-41d4-a716-446655440026'),

    -- Transaction 14 (520k: User5 → User3)
    (14, 1005, 'DEBIT', 520000, '550e8400-e29b-41d4-a716-446655440027'),
    (14, 1003, 'CREDIT', 520000, '550e8400-e29b-41d4-a716-446655440028'),

    -- Transaction 15 (180k: User8 → User1)
    (15, 1008, 'DEBIT', 180000, '550e8400-e29b-41d4-a716-446655440029'),
    (15, 1001, 'CREDIT', 180000, '550e8400-e29b-41d4-a716-446655440030'),

    -- Transaction 16 (450k: User6 → User2)
    (16, 1006, 'DEBIT', 450000, '550e8400-e29b-41d4-a716-446655440031'),
    (16, 1002, 'CREDIT', 450000, '550e8400-e29b-41d4-a716-446655440032'),

    -- Transaction 17 (155k: User3 → User8)
    (17, 1003, 'DEBIT', 155000, '550e8400-e29b-41d4-a716-446655440033'),
    (17, 1008, 'CREDIT', 155000, '550e8400-e29b-41d4-a716-446655440034'),

    -- Transaction 18 (999999: User7 → User4)
    (18, 1007, 'DEBIT', 999999, '550e8400-e29b-41d4-a716-446655440035'),
    (18, 1004, 'CREDIT', 999999, '550e8400-e29b-41d4-a716-446655440036'),

    -- Transaction 19 (345k: User2 → User6)
    (19, 1002, 'DEBIT', 345000, '550e8400-e29b-41d4-a716-446655440037'),
    (19, 1006, 'CREDIT', 345000, '550e8400-e29b-41d4-a716-446655440038'),

    -- Transaction 20 (678k: User4 → User3)
    (20, 1004, 'DEBIT', 678000, '550e8400-e29b-41d4-a716-446655440039'),
    (20, 1003, 'CREDIT', 678000, '550e8400-e29b-41d4-a716-446655440040');

-- ==========================================
-- 4. ORDERS (10 records - for payment & bill transactions)
-- ==========================================
INSERT INTO orders (transaction_id, type, gross_amount, discount_amount, final_amount, status, voucher_instance_id,
                    points_used)
VALUES
    -- Movie Ticket Orders (PAYMENT type)
    (1, 'PAYMENT', 250000, 0, 250000, 'COMPLETED', NULL, 0),        -- Order 1: Transaction 1
    (5, 'PAYMENT', 300000, 0, 300000, 'COMPLETED', NULL, 0),        -- Order 2: Transaction 5
    (10, 'PAYMENT', 600000, 50000, 550000, 'COMPLETED', 101, 2500), -- Order 3: Transaction 10 (with discount)
    (13, 'PAYMENT', 275000, 0, 275000, 'COMPLETED', NULL, 0),       -- Order 4: Transaction 13
    (15, 'PAYMENT', 180000, 0, 180000, 'COMPLETED', NULL, 0),       -- Order 5: Transaction 15
    (17, 'PAYMENT', 155000, 15000, 140000, 'COMPLETED', 102, 1500), -- Order 6: Transaction 17 (with discount)
    (19, 'PAYMENT', 345000, 0, 345000, 'COMPLETED', NULL, 0),       -- Order 7: Transaction 19

    -- Bill Payment Orders (BILL_PAYMENT type)
    (3, 'BILL_PAYMENT', 500000, 0, 500000, 'COMPLETED', NULL, 0),   -- Order 8: Transaction 3 (Internet)
    (7, 'BILL_PAYMENT', 750000, 0, 750000, 'COMPLETED', NULL, 0),   -- Order 9: Transaction 7 (Electric)
    (11, 'BILL_PAYMENT', 850000, 0, 850000, 'COMPLETED', NULL, 0);
-- Order 10: Transaction 11 (Water)

-- ==========================================
-- 5. BILLS (4 records - for bill payment transactions)
-- ==========================================
-- INSERT INTO bills (order_id, provider_code, bill_ref_no, period, amount, status, due_date, paid_at)
-- VALUES

-- ==========================================
INSERT INTO outbox (aggregate_id, aggregate_type, event_type, payload, status)
VALUES
    -- Transaction 1 (Tx1: 250k movie, has order)
    (1, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"1","fromWalletId":"1","toWalletId":"2","amount":"250000","currency":"VND","occurredAt":"2026-06-16T10:15:30Z"}',
     'PENDING'),
    (1, 'TRANSACTION', 'LOYALTY_TRANSFER_EVENT',
     '{"transactionId":"1","userId":"1","occurredAt":"2026-06-16T10:15:30Z"}', 'PENDING'),

    -- Transaction 2 (Tx2: 150k transfer, no loyalty)
    (2, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"2","fromWalletId":"2","toWalletId":"3","amount":"150000","currency":"VND","occurredAt":"2026-06-16T10:20:45Z"}',
     'PENDING'),

    -- Transaction 3 (Tx3: 500k internet bill)
    (3, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"3","fromWalletId":"3","toWalletId":"4","amount":"500000","currency":"VND","occurredAt":"2026-06-16T10:25:12Z"}',
     'PENDING'),
    (3, 'TRANSACTION', 'LOYALTY_TRANSFER_EVENT',
     '{"transactionId":"3","userId":"3","points":5000,"occurredAt":"2026-06-16T10:25:12Z"}', 'PENDING'),

    -- Transaction 4 (Tx4: 200k transfer)
    (4, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"4","fromWalletId":"4","toWalletId":"5","amount":"200000","currency":"VND","occurredAt":"2026-06-16T10:30:00Z"}',
     'PENDING'),

    -- Transaction 5 (Tx5: 300k movie, has order with loyalty)
    (5, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"5","fromWalletId":"1","toWalletId":"5","amount":"300000","currency":"VND","occurredAt":"2026-06-16T10:35:22Z"}',
     'PENDING'),
    (5, 'TRANSACTION', 'LOYALTY_TRANSFER_EVENT',
     '{"transactionId":"5","userId":"1","voucherId":"101","points":3000,"occurredAt":"2026-06-16T10:35:22Z"}',
     'PENDING'),

    -- Transaction 6 (Tx6: 100k transfer)
    (6, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"6","fromWalletId":"2","toWalletId":"4","amount":"100000","currency":"VND","occurredAt":"2026-06-16T10:40:15Z"}',
     'PENDING'),

    -- Transaction 7 (Tx7: 750k electric bill)
    (7, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"7","fromWalletId":"5","toWalletId":"6","amount":"750000","currency":"VND","occurredAt":"2026-06-16T10:45:33Z"}',
     'PENDING'),
    (7, 'TRANSACTION', 'LOYALTY_TRANSFER_EVENT',
     '{"transactionId":"7","userId":"5","points":7500,"occurredAt":"2026-06-16T10:45:33Z"}', 'PENDING'),

    -- Transaction 8 (Tx8: 180k transfer)
    (8, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"8","fromWalletId":"6","toWalletId":"7","amount":"180000","currency":"VND","occurredAt":"2026-06-16T10:50:44Z"}',
     'PENDING'),

    -- Transaction 9 (Tx9: 420k transfer)
    (9, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"9","fromWalletId":"3","toWalletId":"7","amount":"420000","currency":"VND","occurredAt":"2026-06-16T10:55:11Z"}',
     'PENDING'),

    -- Transaction 10 (Tx10: 600k movie, has order with loyalty discount)
    (10, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"10","fromWalletId":"7","toWalletId":"8","amount":"600000","currency":"VND","occurredAt":"2026-06-16T11:00:25Z"}',
     'PENDING'),
    (10, 'TRANSACTION', 'LOYALTY_TRANSFER_EVENT',
     '{"transactionId":"10","userId":"7","voucherId":"101","points":2500,"occurredAt":"2026-06-16T11:00:25Z"}',
     'PENDING'),

    -- Transaction 11 (Tx11: 850k water bill)
    (11, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"11","fromWalletId":"1","toWalletId":"6","amount":"850000","currency":"VND","occurredAt":"2026-06-16T11:05:40Z"}',
     'PENDING'),
    (11, 'TRANSACTION', 'LOYALTY_TRANSFER_EVENT',
     '{"transactionId":"11","userId":"1","points":8500,"occurredAt":"2026-06-16T11:05:40Z"}', 'PENDING'),

    -- Transaction 12 (Tx12: 350k transfer)
    (12, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"12","fromWalletId":"4","toWalletId":"8","amount":"350000","currency":"VND","occurredAt":"2026-06-16T11:10:50Z"}',
     'PENDING'),

    -- Transaction 13 (Tx13: 275k movie, has order)
    (13, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"13","fromWalletId":"2","toWalletId":"7","amount":"275000","currency":"VND","occurredAt":"2026-06-16T11:15:33Z"}',
     'PENDING'),
    (13, 'TRANSACTION', 'LOYALTY_TRANSFER_EVENT',
     '{"transactionId":"13","userId":"2","points":2750,"occurredAt":"2026-06-16T11:15:33Z"}', 'PENDING'),

    -- Transaction 14-20: TRANSFER_COMPLETED only (no loyalty events)
    (14, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"14","fromWalletId":"5","toWalletId":"3","amount":"520000","currency":"VND","occurredAt":"2026-06-16T11:20:12Z"}',
     'PENDING'),
    (15, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"15","fromWalletId":"8","toWalletId":"1","amount":"180000","currency":"VND","occurredAt":"2026-06-16T11:25:44Z"}',
     'PENDING'),
    (16, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"16","fromWalletId":"6","toWalletId":"2","amount":"450000","currency":"VND","occurredAt":"2026-06-16T11:30:22Z"}',
     'PENDING'),
    (17, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"17","fromWalletId":"3","toWalletId":"8","amount":"155000","currency":"VND","occurredAt":"2026-06-16T11:35:55Z"}',
     'PENDING'),
    (17, 'TRANSACTION', 'LOYALTY_TRANSFER_EVENT',
     '{"transactionId":"17","userId":"3","voucherId":"102","points":1500,"occurredAt":"2026-06-16T11:35:55Z"}',
     'PENDING'),
    (18, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"18","fromWalletId":"7","toWalletId":"4","amount":"999999","currency":"VND","occurredAt":"2026-06-16T11:40:30Z"}',
     'PENDING'),
    (19, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"19","fromWalletId":"2","toWalletId":"6","amount":"345000","currency":"VND","occurredAt":"2026-06-16T11:45:18Z"}',
     'PENDING'),
    (20, 'TRANSACTION', 'TRANSFER_COMPLETED',
     '{"transactionId":"20","fromWalletId":"4","toWalletId":"3","amount":"678000","currency":"VND","occurredAt":"2026-06-16T11:50:42Z"}',
     'PENDING');

