package com.grid07.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuardrailService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_BOT_REPLIES = 100;
    private static final int MAX_DEPTH = 20;
    private static final long COOLDOWN_MINUTES = 10;

    // Redis key patterns
    private static final String BOT_COUNT_KEY = "post:%d:bot_count";
    private static final String COOLDOWN_KEY = "cooldown:bot_%d:human_%d";

    public boolean checkAndIncrementBotCount(Long postId) {
        String key = String.format(BOT_COUNT_KEY, postId);

        Long newCount = redisTemplate.opsForValue().increment(key);

        if (newCount == null) {
            log.error("Redis returned null for INCR on key {}", key);
            return false;
        }

        if (newCount > MAX_BOT_REPLIES) {
            redisTemplate.opsForValue().decrement(key);
            log.warn("Post {} hit horizontal cap. Bot count was at {}, rejecting.", postId, newCount - 1);
            return false;
        }

        log.debug("Post {} bot count is now {}/{}", postId, newCount, MAX_BOT_REPLIES);
        return true;
    }

    public boolean checkDepthLevel(int depthLevel) {
        if (depthLevel > MAX_DEPTH) {
            log.warn("Comment thread too deep: depth {} > max {}", depthLevel, MAX_DEPTH);
            return false;
        }
        return true;
    }

    public boolean checkAndSetCooldown(Long botId, Long humanUserId) {
        String key = String.format(COOLDOWN_KEY, botId, humanUserId);

        Boolean wasSet = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMinutes(COOLDOWN_MINUTES));

        if (Boolean.FALSE.equals(wasSet)) {
            log.warn("Bot {} is on cooldown for user {}. Try again later.", botId, humanUserId);
            return false;
        }

        log.debug("Cooldown set for bot {} -> user {} for {} minutes", botId, humanUserId, COOLDOWN_MINUTES);
        return true;
    }

    public long getBotCount(Long postId) {
        String key = String.format(BOT_COUNT_KEY, postId);
        String val = redisTemplate.opsForValue().get(key);
        return val == null ? 0L : Long.parseLong(val);
    }
}
