package com.psychsupport.webpsychologicalsupport.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class EncryptionService {

    @Value("${app.encryption.master-key:default-master-key-change-in-production}")
    private String masterKey;

    private final Map<String, String> saltCache = new ConcurrentHashMap<>();
    
    private final SecureRandom secureRandom = new SecureRandom();

    public String getConversationSalt(Long userId1, Long userId2) {
        String conversationId = getConversationId(userId1, userId2);
        
        return saltCache.computeIfAbsent(conversationId, k -> {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                String seedData = conversationId + ":" + masterKey;
                byte[] hash = digest.digest(seedData.getBytes());

                byte[] salt = new byte[16];
                System.arraycopy(hash, 0, salt, 0, 16);
                
                return Base64.getEncoder().encodeToString(salt);
            } catch (NoSuchAlgorithmException e) {
                byte[] salt = new byte[16];
                secureRandom.nextBytes(salt);
                return Base64.getEncoder().encodeToString(salt);
            }
        });
    }

    public String getJournalSalt(Long userId) {
        String journalId = getJournalId(userId);
        
        return saltCache.computeIfAbsent(journalId, k -> {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                String seedData = journalId + ":" + masterKey + ":journal";
                byte[] hash = digest.digest(seedData.getBytes());

                byte[] salt = new byte[16];
                System.arraycopy(hash, 0, salt, 0, 16);
                
                return Base64.getEncoder().encodeToString(salt);
            } catch (NoSuchAlgorithmException e) {
                byte[] salt = new byte[16];
                secureRandom.nextBytes(salt);
                return Base64.getEncoder().encodeToString(salt);
            }
        });
    }

    private String getConversationId(Long userId1, Long userId2) {
        long id1 = Math.min(userId1, userId2);
        long id2 = Math.max(userId1, userId2);
        return "conv_" + id1 + "_" + id2;
    }

    private String getJournalId(Long userId) {
        return "journal_" + userId;
    }

    public boolean isAuthorizedConversation(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) {
            return false;
        }
        return true;
    }

    public void clearSaltCache() {
        saltCache.clear();
    }

    public String getSharedJournalSalt(Long userId, Long psychologistId) {
        String sharedId = getSharedJournalId(userId, psychologistId);
        
        return saltCache.computeIfAbsent(sharedId, k -> {

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                String seedData = sharedId + ":" + masterKey + ":shared-journal";
                byte[] hash = digest.digest(seedData.getBytes());

                byte[] salt = new byte[16];
                System.arraycopy(hash, 0, salt, 0, 16);
                
                return Base64.getEncoder().encodeToString(salt);
            } catch (NoSuchAlgorithmException e) {
                byte[] salt = new byte[16];
                secureRandom.nextBytes(salt);
                return Base64.getEncoder().encodeToString(salt);
            }
        });
    }

    private String getSharedJournalId(Long userId, Long psychologistId) {
        return "shared_journal_" + userId + "_" + psychologistId;
    }

    public void clearSalt(String saltId) {
        saltCache.remove(saltId);
    }

    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("saltCacheSize", saltCache.size());
        stats.put("totalConversations", saltCache.keySet().size());
        return stats;
    }
}