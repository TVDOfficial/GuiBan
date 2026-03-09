package com.enterprise.guiban.utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter {

    private final int maxPerMinute;
    private final Map<UUID, long[]> counts = new ConcurrentHashMap<>();

    public RateLimiter(int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    public boolean allow(UUID uuid) {
        if (maxPerMinute <= 0) return true;
        long now = System.currentTimeMillis();
        long[] pair = counts.compute(uuid, (k, v) -> {
            if (v == null) return new long[]{now, 1};
            if (now - v[0] > 60_000) return new long[]{now, 1};
            return new long[]{v[0], v[1] + 1};
        });
        return pair[1] <= maxPerMinute;
    }

    public int getRemaining(UUID uuid) {
        if (maxPerMinute <= 0) return Integer.MAX_VALUE;
        long[] pair = counts.get(uuid);
        if (pair == null) return maxPerMinute;
        if (System.currentTimeMillis() - pair[0] > 60_000) return maxPerMinute;
        return Math.max(0, maxPerMinute - (int) pair[1]);
    }
}
