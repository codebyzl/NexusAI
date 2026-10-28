
local counter_key = KEYS[1]
local blocked_key = KEYS[2]

local max_failures = tonumber(ARGV[1])
local window_millis = tonumber(ARGV[2])
local block_millis = tonumber(ARGV[3])

-- 1. 登录失败次数 +1
local count = redis.call('INCR', counter_key)

-- 2. 第一次失败，设置统计窗口
if count == 1 then
    redis.call(
        'PEXPIRE',
        counter_key,
        window_millis
    )
end

-- 3. 超过最大失败次数，开始限制登录
if count > max_failures then

    local existing_ttl =
        redis.call('PTTL', blocked_key)

    -- 已经被限制，返回剩余限制时间
    if existing_ttl > 0 then
        return existing_ttl
    end

    -- 创建封禁 Key
    redis.call(
        'SET',
        blocked_key,
        '1',
        'PX',
        block_millis
    )

    return block_millis
end

-- 4. 还没达到封禁条件
return 0
