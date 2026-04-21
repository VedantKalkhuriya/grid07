package com.grid07.scheduler;

import com.grid07.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationService notificationService;

    @Scheduled(cron = "0 */5 * * * *")
    public void sweepPendingNotifications() {
        log.info("=== CRON Sweeper running - checking for pending notifications ===");

        List<Long> usersWithPendingNotifs = notificationService.getUsersWithPendingNotifications();

        if (usersWithPendingNotifs.isEmpty()) {
            log.info("No pending notifications found.");
            return;
        }

        log.info("Found {} users with pending notifications", usersWithPendingNotifs.size());

        for (Long userId : usersWithPendingNotifs) {
            List<String> messages = notificationService.drainPendingNotifications(userId);

            if (messages.isEmpty()) {
                continue;
            }

            if (messages.size() == 1) {
                log.info("Summarized Push Notification for User {}: {}", userId, messages.get(0));
            } else {
                String firstMessage = messages.get(0);
                String botName = extractBotName(firstMessage);
                int othersCount = messages.size() - 1;

                log.info("Summarized Push Notification for User {}: {} and {} others interacted with your posts.",
                        userId, botName, othersCount);
            }
        }

        log.info("=== CRON Sweeper done ===");
    }

    private String extractBotName(String message) {
        try {
            String afterBot = message.substring("Bot ".length());
            return afterBot.split(" ")[0];
        } catch (Exception e) {
            return "A bot";
        }
    }
}
