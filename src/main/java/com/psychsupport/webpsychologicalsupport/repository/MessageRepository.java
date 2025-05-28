package com.psychsupport.webpsychologicalsupport.repository;

import com.psychsupport.webpsychologicalsupport.model.Message;
import com.psychsupport.webpsychologicalsupport.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderAndReceiver(User sender, User receiver);

    @Query("SELECT m FROM Message m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) ORDER BY m.sentAt ASC")
    List<Message> findConversation(@Param("user1") User user1, @Param("user2") User user2);

    List<Message> findByReceiverAndReadAtIsNull(User receiver);

    long countByReceiverAndReadAtIsNull(User receiver);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.sender = :sender AND m.receiver = :receiver AND m.readAt IS NULL")
    long countUnreadMessagesFromSender(@Param("sender") User sender, @Param("receiver") User receiver);

    @Query("SELECT m FROM Message m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) ORDER BY m.sentAt DESC")
    List<Message> findLatestMessage(@Param("user1") User user1, @Param("user2") User user2);

    @Modifying
    @Query("DELETE FROM Message m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1)")
    void deleteConversation(@Param("user1") User user1, @Param("user2") User user2);

    List<Message> findByReceiverAndSentAtBetween(User receiver, LocalDateTime start, LocalDateTime end);

    List<Message> findBySenderAndSentAtBetween(User sender, LocalDateTime start, LocalDateTime end);

    @Query("SELECT DISTINCT m.sender FROM Message m WHERE m.receiver = :user")
    List<User> findDistinctSenders(@Param("user") User user);

    @Query("SELECT DISTINCT m.receiver FROM Message m WHERE m.sender = :user")
    List<User> findDistinctReceivers(@Param("user") User user);

    @Query(value = "SELECT m.* FROM messages m " +
            "INNER JOIN (SELECT GREATEST(sender_id, receiver_id) as user1, LEAST(sender_id, receiver_id) as user2, MAX(sent_at) as max_date " +
            "FROM messages GROUP BY user1, user2) latest " +
            "ON ((m.sender_id = latest.user1 AND m.receiver_id = latest.user2) OR (m.sender_id = latest.user2 AND m.receiver_id = latest.user1)) " +
            "AND m.sent_at = latest.max_date " +
            "WHERE m.sender_id = :userId OR m.receiver_id = :userId " +
            "ORDER BY m.sent_at DESC", nativeQuery = true)
    List<Message> findMostRecentMessagesForUser(@Param("userId") Long userId);

    List<Message> findBySenderIdOrReceiverId(Long senderId, Long receiverId);
    
    @Modifying
    @Query("DELETE FROM Message m WHERE m.sender.id = :userId OR m.receiver.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}