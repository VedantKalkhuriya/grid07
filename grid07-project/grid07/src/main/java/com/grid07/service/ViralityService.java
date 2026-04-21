package com.grid07.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViralityService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String VIRALITY_KEY = "post:%d:virality_score";

    private static final int BOT_REPLY_POINTS = 1;
    private static final int HUMAN_LIKE_POINTS = 20;
    private static final int HUMAN_COMMENT_POINTS = 50;

    public void addBotReplyPoints(Long postId) {
        String key = String.format(VIRALITY_KEY, postId);
        Long newScore = redisTemplate.opsForValue().increment(key, BOT_REPLY_POINTS);
        log.debug("Bot replied on post {} - virality score is now {}", postId, newScore);
    }

    public void addHumanLikePoints(Long postId) {
        String key = String.format(VIRALITY_KEY, postId);
        Long newScore = redisTemplate.opsForValue().increment(key, HUMAN_LIKE_POINTS);
        log.debug("Human liked post {} - virality score is now {}", postId, newScore);
    }

    public void addHumanCommentPoints(Long postId) {
        String key = String.format(VIRALITY_KEY, postId);
        Long newScore = redisTemplate.opsForValue().increment(key, HUMAN_COMMENT_POINTS);
        log.debug("Human commented on post {} - virality score is now {}", postId, newScore);
    }

    public Long getViralityScore(Long postId) {
        String key = String.format(VIRALITY_KEY, postId);
        String val = redisTemplate.opsForValue().get(key);
        if (val == null) return 0L;
        return Long.parseLong(val);
    }
}
