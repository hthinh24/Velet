-- ==========================================
-- WALLET SERVICE SEED DATA
-- ==========================================

-- ==========================================
-- 1. WALLETS (10 records - one per user)
-- ==========================================
INSERT INTO wallets (owner_id, type, currency, available_balance, pending_balance, status)
VALUES (1, 'USER', 'VND', 10000000, 0, 'ACTIVE'), -- User 1: 10M initial
       (2, 'USER', 'VND', 10000000, 0, 'ACTIVE'), -- User 2: 10M initial
       (3, 'USER', 'VND', 10000000, 0, 'ACTIVE'), -- User 3: 10M initial
       (4, 'USER', 'VND', 10000000, 0, 'ACTIVE'), -- User 4: 10M initial
       (5, 'USER', 'VND', 10000000, 0, 'ACTIVE'), -- User 5: 10M initial
       (6, 'USER', 'VND', 10000000, 0, 'ACTIVE'), -- User 6: 10M initial
       (7, 'USER', 'VND', 10000000, 0, 'ACTIVE'), -- User 7: 10M initial
       (8, 'USER', 'VND', 10000000, 0, 'ACTIVE'), -- User 8: 10M initial
       (9, 'USER', 'VND', 10000000, 0, 'ACTIVE'), -- User 9: no transactions yet
       (10, 'USER', 'VND', 10000000, 0, 'ACTIVE');
-- User 10: no transactions yet

-- ==========================================
-- 2. TRANSACTIONS (20 records)
-- Pattern: Mix of transfers, topups, and payments
-- Amounts: 100k - 1M VND
-- ==========================================
INSERT INTO transactions (source_wallet_id, destination_wallet_id, amount, currency, type, status, idempotency_key)
VALUES
    -- Tx 1: User 1 → User 2 (250k movie ticket)
    (1, 2, 250000, 'VND', 'TRANSFER', 'SUCCESS', 'a1b2c3d4-e5f6-47d8-90a1-bb2cc3dd4ee1'),

    -- Tx 2: User 2 → User 3 (150k transfer)
    (2, 3, 150000, 'VND', 'TRANSFER', 'SUCCESS', 'b2c3d4e5-f6a7-48e9-91b2-cc3dd4ee5ff2'),

    -- Tx 3: User 3 → User 4 (500k internet bill)
    (3, 4, 500000, 'VND', 'BILL_PAYMENT', 'SUCCESS', 'c3d4e5f6-a7b8-49f0-92c3-dd4ee5ff6002'),

    -- Tx 4: User 4 → User 5 (200k transfer)
    (4, 5, 200000, 'VND', 'TRANSFER', 'SUCCESS', 'd4e5f6a7-b8c9-40a1-93d4-ee5ff6001011'),

    -- Tx 5: User 1 → User 5 (300k movie ticket)
    (1, 5, 300000, 'VND', 'TRANSFER', 'SUCCESS', 'e5f6a7b8-c9d0-41b2-94e5-f60010113222'),

    -- Tx 6: User 2 → User 4 (100k transfer)
    (2, 4, 100000, 'VND', 'TRANSFER', 'SUCCESS', 'f6a7b8c9-d0e1-42c3-95f6-001011322433'),

    -- Tx 7: User 5 → User 6 (750k electric bill)
    (5, 6, 750000, 'VND', 'BILL_PAYMENT', 'SUCCESS', 'a7b8c9d0-e1f2-43d4-96a7-010113224335'),

    -- Tx 8: User 6 → User 7 (180k transfer)
    (6, 7, 180000, 'VND', 'TRANSFER', 'SUCCESS', 'b8c9d0e1-f2a3-44e5-97b8-101132243366'),

    -- Tx 9: User 3 → User 7 (420k transfer)
    (3, 7, 420000, 'VND', 'TRANSFER', 'SUCCESS', 'c9d0e1f2-a3b4-45f6-98c9-101132243376'),

    -- Tx 10: User 7 → User 8 (600k movie ticket)
    (7, 8, 600000, 'VND', 'TRANSFER', 'SUCCESS', 'd0e1f2a3-b4c5-46a7-99d0-101132243387'),

    -- Tx 11: User 1 → User 6 (850k water bill)
    (1, 6, 850000, 'VND', 'BILL_PAYMENT', 'SUCCESS', 'e1f2a3b4-c5d6-47b8-90e1-101132243398'),

    -- Tx 12: User 4 → User 8 (350k transfer)
    (4, 8, 350000, 'VND', 'TRANSFER', 'SUCCESS', 'f2a3b4c5-d6e7-48c9-91f2-101132243399'),

    -- Tx 13: User 2 → User 7 (275k movie ticket)
    (2, 7, 275000, 'VND', 'TRANSFER', 'SUCCESS', 'a3b4c5d6-e7f8-49d0-92a3-101132243400'),

    -- Tx 14: User 5 → User 3 (520k transfer)
    (5, 3, 520000, 'VND', 'TRANSFER', 'SUCCESS', 'b4c5d6e7-f8a9-40e1-93b4-101132243401'),

    -- Tx 15: User 8 → User 1 (180k transfer)
    (8, 1, 180000, 'VND', 'TRANSFER', 'SUCCESS', 'c5d6e7f8-a9b0-41f2-94c5-101132243402'),

    -- Tx 16: User 6 → User 2 (450k transfer)
    (6, 2, 450000, 'VND', 'TRANSFER', 'SUCCESS', 'd6e7f8a9-b0c1-42a3-95d6-101132243403'),

    -- Tx 17: User 3 → User 8 (155k transfer)
    (3, 8, 155000, 'VND', 'TRANSFER', 'SUCCESS', 'e7f8a9b0-c1d2-43b4-96e7-101132243404'),

    -- Tx 18: User 7 → User 4 (999999 large transfer)
    (7, 4, 999999, 'VND', 'TRANSFER', 'SUCCESS', 'f8a9b0c1-d2e3-44c5-97f8-101132243405'),

    -- Tx 19: User 2 → User 6 (345k transfer)
    (2, 6, 345000, 'VND', 'TRANSFER', 'SUCCESS', 'a9b0c1d2-e3f4-45d6-98a9-101132243405'),

    -- Tx 20: User 4 → User 3 (678000 transfer)
    (4, 3, 678000, 'VND', 'TRANSFER', 'SUCCESS', 'b0c1d2e3-f4a5-46e7-99b0-101132243405');

-- ==========================================
-- 3. LEDGER ENTRIES (40 records = 20 transactions × 2)
-- DEBIT: source wallet loses money
-- CREDIT: destination wallet gains money
-- ==========================================
INSERT INTO ledger_entries (transaction_id, wallet_id, entry_type, amount, running_balance, idempotency_key)
VALUES
    -- Transaction 1 (250k: User1 → User2)
    (1, 1, 'DEBIT', 250000, 9750000, '550e8400-e29b-41d4-a716-446655440001'),    -- User 1 after paying 250k
    (1, 2, 'CREDIT', 250000, 10250000, '550e8400-e29b-41d4-a716-446655440002'),  -- User 2 after receiving 250k

    -- Transaction 2 (150k: User2 → User3)
    (2, 2, 'DEBIT', 150000, 10100000, '550e8400-e29b-41d4-a716-446655440003'),   -- User 2 after paying 150k
    (2, 3, 'CREDIT', 150000, 10150000, '550e8400-e29b-41d4-a716-446655440004'),  -- User 3 after receiving 150k

    -- Transaction 3 (500k: User3 → User4)
    (3, 3, 'DEBIT', 500000, 9650000, '550e8400-e29b-41d4-a716-446655440005'),    -- User 3 after paying 500k
    (3, 4, 'CREDIT', 500000, 10500000, '550e8400-e29b-41d4-a716-446655440006'),  -- User 4 after receiving 500k

    -- Transaction 4 (200k: User4 → User5)
    (4, 4, 'DEBIT', 200000, 10300000, '550e8400-e29b-41d4-a716-446655440007'),   -- User 4 after paying 200k
    (4, 5, 'CREDIT', 200000, 10200000, '550e8400-e29b-41d4-a716-446655440008'),  -- User 5 after receiving 200k

    -- Transaction 5 (300k: User1 → User5)
    (5, 1, 'DEBIT', 300000, 9450000, '550e8400-e29b-41d4-a716-446655440009'),    -- User 1 after paying 300k
    (5, 5, 'CREDIT', 300000, 10500000, '550e8400-e29b-41d4-a716-446655440010'),  -- User 5 after receiving 300k

    -- Transaction 6 (100k: User2 → User4)
    (6, 2, 'DEBIT', 100000, 10000000, '550e8400-e29b-41d4-a716-446655440011'),   -- User 2 after paying 100k
    (6, 4, 'CREDIT', 100000, 10400000, '550e8400-e29b-41d4-a716-446655440012'),  -- User 4 after receiving 100k

    -- Transaction 7 (750k: User5 → User6)
    (7, 5, 'DEBIT', 750000, 9750000, '550e8400-e29b-41d4-a716-446655440013'),    -- User 5 after paying 750k
    (7, 6, 'CREDIT', 750000, 10750000, '550e8400-e29b-41d4-a716-446655440014'),  -- User 6 after receiving 750k

    -- Transaction 8 (180k: User6 → User7)
    (8, 6, 'DEBIT', 180000, 10570000, '550e8400-e29b-41d4-a716-446655440015'),   -- User 6 after paying 180k
    (8, 7, 'CREDIT', 180000, 10180000, '550e8400-e29b-41d4-a716-446655440016'),  -- User 7 after receiving 180k

    -- Transaction 9 (420k: User3 → User7)
    (9, 3, 'DEBIT', 420000, 9230000, '550e8400-e29b-41d4-a716-446655440017'),    -- User 3 after paying 420k
    (9, 7, 'CREDIT', 420000, 10600000, '550e8400-e29b-41d4-a716-446655440018'),  -- User 7 after receiving 420k

    -- Transaction 10 (600k: User7 → User8)
    (10, 7, 'DEBIT', 600000, 10000000, '550e8400-e29b-41d4-a716-446655440019'),  -- User 7 after paying 600k
    (10, 8, 'CREDIT', 600000, 10600000, '550e8400-e29b-41d4-a716-446655440020'), -- User 8 after receiving 600k

    -- Transaction 11 (850k: User1 → User6)
    (11, 1, 'DEBIT', 850000, 8600000, '550e8400-e29b-41d4-a716-446655440021'),   -- User 1 after paying 850k
    (11, 6, 'CREDIT', 850000, 11420000, '550e8400-e29b-41d4-a716-446655440022'), -- User 6 after receiving 850k

    -- Transaction 12 (350k: User4 → User8)
    (12, 4, 'DEBIT', 350000, 10050000, '550e8400-e29b-41d4-a716-446655440023'),  -- User 4 after paying 350k
    (12, 8, 'CREDIT', 350000, 10950000, '550e8400-e29b-41d4-a716-446655440024'), -- User 8 after receiving 350k

    -- Transaction 13 (275k: User2 → User7)
    (13, 2, 'DEBIT', 275000, 9725000, '550e8400-e29b-41d4-a716-446655440025'),   -- User 2 after paying 275k
    (13, 7, 'CREDIT', 275000, 10875000, '550e8400-e29b-41d4-a716-446655440026'), -- User 7 after receiving 275k

    -- Transaction 14 (520k: User5 → User3)
    (14, 5, 'DEBIT', 520000, 9230000, '550e8400-e29b-41d4-a716-446655440027'),   -- User 5 after paying 520k
    (14, 3, 'CREDIT', 520000, 9750000, '550e8400-e29b-41d4-a716-446655440028'),  -- User 3 after receiving 520k

    -- Transaction 15 (180k: User8 → User1)
    (15, 8, 'DEBIT', 180000, 10770000, '550e8400-e29b-41d4-a716-446655440029'),  -- User 8 after paying 180k
    (15, 1, 'CREDIT', 180000, 8780000, '550e8400-e29b-41d4-a716-446655440030'),  -- User 1 after receiving 180k

    -- Transaction 16 (450k: User6 → User2)
    (16, 6, 'DEBIT', 450000, 10970000, '550e8400-e29b-41d4-a716-446655440031'),  -- User 6 after paying 450k
    (16, 2, 'CREDIT', 450000, 10175000, '550e8400-e29b-41d4-a716-446655440032'), -- User 2 after receiving 450k

    -- Transaction 17 (155k: User3 → User8)
    (17, 3, 'DEBIT', 155000, 9595000, '550e8400-e29b-41d4-a716-446655440033'),   -- User 3 after paying 155k
    (17, 8, 'CREDIT', 155000, 10925000, '550e8400-e29b-41d4-a716-446655440034'), -- User 8 after receiving 155k

    -- Transaction 18 (999999: User7 → User4)
    (18, 7, 'DEBIT', 999999, 9875001, '550e8400-e29b-41d4-a716-446655440035'),   -- User 7 after paying 999999
    (18, 4, 'CREDIT', 999999, 11050000, '550e8400-e29b-41d4-a716-446655440036'), -- User 4 after receiving 999999

    -- Transaction 19 (345k: User2 → User6)
    (19, 2, 'DEBIT', 345000, 9830000, '550e8400-e29b-41d4-a716-446655440037'),   -- User 2 after paying 345k
    (19, 6, 'CREDIT', 345000, 11315000, '550e8400-e29b-41d4-a716-446655440038'), -- User 6 after receiving 345k

    -- Transaction 20 (678k: User4 → User3)
    (20, 4, 'DEBIT', 678000, 10372000, '550e8400-e29b-41d4-a716-446655440039'),  -- User 4 after paying 678k
    (20, 3, 'CREDIT', 678000, 10273000, '550e8400-e29b-41d4-a716-446655440040');
-- User 3 after receiving 678k

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

