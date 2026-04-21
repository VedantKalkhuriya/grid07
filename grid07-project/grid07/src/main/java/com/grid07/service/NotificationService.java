package com.grid07.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String NOTIF_COOLDOWN_KEY = "notif:cooldown:user_%d";
    private static final String PENDING_NOTIFS_KEY = "user:%d:pending_notifs";
    private static final long NOTIF_COOLDOWN_MINUTES = 15;

    public void handleBotNotification(Long userId, String botName, Long postId) {
        String cooldownKey = String.format(NOTIF_COOLDOWN_KEY, userId);
        String message = String.format("Bot %s replied to your post (postId: %d)", botName, postId);

        Boolean onCooldown = redisTemplate.hasKey(cooldownKey);

        if (Boolean.TRUE.equals(onCooldown)) {
            String pendingKey = String.format(PENDING_NOTIFS_KEY, userId);
            redisTemplate.opsForList().rightPush(pendingKey, message);
            log.debug("User {} already notified recently, queued: {}", userId, message);
        } else {
            log.info("Push Notification Sent to User {}: {}", userId, message);
            redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofMinutes(NOTIF_COOLDOWN_MINUTES));
        }
    }

    public List<String> drainPendingNotifications(Long userId) {
        String pendingKey = String.format(PENDING_NOTIFS_KEY, userId);
        List<String> messages = new ArrayList<>();

        String msg;
        while ((msg = redisTemplate.opsForList().leftPop(pendingKey)) != null) {
            messages.add(msg);
        }

        return messages;
    }

    public List<Long> getUsersWithPendingNotifications() {
        var keys = redisTemplate.keys("user:*:pending_notifs");
        List<Long> userIds = new ArrayList<>();

        if (keys == null) return userIds;

        for (String key : keys) {
            try {
                String[] parts = key.split(":");
                Long userId = Long.parseLong(parts[1]);
                Long size = redisTemplate.opsForList().size(key);
                if (size != null && size > 0) {
                    userIds.add(userId);
                }
            } catch (Exception e) {
                log.error("Couldn't parse user id from key: {}", key);
            }
        }

        return userIds;
    }
}
