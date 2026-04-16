package com.example.lolserver.controller.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SocialAccountLinkTokenStore {

    private static final int EXPIRE_SECONDS = 300;

    private final ConcurrentHashMap<String, LinkTokenEntry> store =
            new ConcurrentHashMap<>();

    public String generateToken(Long memberId) {
        evictExpired();
        String token = UUID.randomUUID().toString();
        store.put(token, new LinkTokenEntry(
                memberId, Instant.now().plusSeconds(EXPIRE_SECONDS)));
        return token;
    }

    public Long consumeToken(String token) {
        LinkTokenEntry entry = store.remove(token);
        if (entry == null || entry.isExpired()) {
            return null;
        }
        return entry.memberId();
    }

    private void evictExpired() {
        Iterator<Map.Entry<String, LinkTokenEntry>> it =
                store.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired()) {
                it.remove();
            }
        }
    }

    private record LinkTokenEntry(Long memberId, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
