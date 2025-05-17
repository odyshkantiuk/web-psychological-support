package com.psychsupport.webpsychologicalsupport.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPT = 5;
    private static final int BLOCK_DURATION_MINUTES = 15;
    
    private final LoadingCache<String, Integer> attemptsCache;
    private final HttpServletRequest request;
    
    public LoginAttemptService(HttpServletRequest request) {
        this.request = request;
        attemptsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(BLOCK_DURATION_MINUTES, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }
    
    public void loginSucceeded(String username) {
        String cacheKey = constructCacheKey(username);
        attemptsCache.invalidate(cacheKey);
    }
    
    public void loginFailed(String username) {
        String cacheKey = constructCacheKey(username);
        int attempts;
        try {
            attempts = attemptsCache.get(cacheKey);
        } catch (ExecutionException e) {
            attempts = 0;
        }
        attempts++;
        attemptsCache.put(cacheKey, attempts);
    }
    
    public boolean isBlocked(String username) {
        String cacheKey = constructCacheKey(username);
        try {
            return attemptsCache.get(cacheKey) >= MAX_ATTEMPT;
        } catch (ExecutionException e) {
            return false;
        }
    }
    
    public boolean isBlocked() {
        String ip = getClientIP();
        try {
            return attemptsCache.get(ip) >= MAX_ATTEMPT;
        } catch (ExecutionException e) {
            return false;
        }
    }

    public void resetAttempts(String username) {
        String cacheKey = constructCacheKey(username);
        attemptsCache.invalidate(cacheKey);
    }

    public Map<String, Integer> getBlockedUsers() {
        Map<String, Integer> blocked = new HashMap<>();
        
        Map<String, Integer> cacheMap;
        try {
            cacheMap = attemptsCache.asMap();
        } catch (Exception e) {
            return blocked;
        }

        return cacheMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("user:") && entry.getValue() >= MAX_ATTEMPT)
                .collect(Collectors.toMap(
                    entry -> entry.getKey().substring(5),
                    Map.Entry::getValue
                ));
    }

    public int getRemainingAttempts(String username) {
        String cacheKey = constructCacheKey(username);
        try {
            int attempts = attemptsCache.get(cacheKey);
            return Math.max(0, MAX_ATTEMPT - attempts);
        } catch (ExecutionException e) {
            return MAX_ATTEMPT;
        }
    }

    private String constructCacheKey(String username) {
        if (username == null || username.isEmpty()) {
            return getClientIP();
        }
        return "user:" + username;
    }
    
    private String getClientIP() {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    public int getMaxAttempts() {
        return MAX_ATTEMPT;
    }

    public int getBlockDurationMinutes() {
        return BLOCK_DURATION_MINUTES;
    }
}