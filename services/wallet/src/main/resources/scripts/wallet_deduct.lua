-- KEYS[1]: wallet cache key (e.g., "wallet:123")
-- ARGV[1]: amount to deduct (String, e.g., "1000")

local walletJson = redis.call('get', KEYS[1])
if not walletJson then
    return -1 -- Wallet not found
end

local wallet = cjson.decode(walletJson)

if wallet['status'] ~= 'ACTIVE' then
    return -2 -- Wallet not active
end

local availableBalance = tonumber(wallet['availableBalance'])
local amountToDuct = tonumber(ARGV[1])

if availableBalance < amountToDuct then
    return -3 -- Insufficient balance
end

wallet['availableBalance'] = availableBalance - amountToDuct

local updatedJson = cjson.encode(wallet)
redis.call('set', KEYS[1], updatedJson)

return 1