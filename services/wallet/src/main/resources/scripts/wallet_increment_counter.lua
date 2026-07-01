-- Atomically increments a counter field in the balance hash and refreshes TTL.
-- Keeps increment + TTL refresh as one atomic unit so TTL never drifts out of sync.
-- KEYS[1] = wallet:balance:{walletId}
-- ARGV[1] = field name (e.g. "posted_debits")
-- ARGV[2] = delta (positive or negative long)
-- ARGV[3] = TTL in seconds
redis.call('HINCRBY', KEYS[1], ARGV[1], ARGV[2])
redis.call('EXPIRE',  KEYS[1], ARGV[3])
return 1
