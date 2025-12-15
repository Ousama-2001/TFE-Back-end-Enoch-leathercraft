package com.enoch.leathercraft.services;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContactRateLimitService {

    private final Map<String, LocalDateTime> lastRequests = new ConcurrentHashMap<>();

    // 1 message / IP toutes les 2 minutes
    private static final Duration COOLDOWN = Duration.ofMinutes(2);

    public boolean isAllowed(String ip) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTime = lastRequests.get(ip);

        if (lastTime == null || Duration.between(lastTime, now).compareTo(COOLDOWN) > 0) {
            lastRequests.put(ip, now);
            return true;
        }
        return false;
    }
}
