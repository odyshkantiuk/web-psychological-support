package com.psychsupport.webpsychologicalsupport.repository;

import com.psychsupport.webpsychologicalsupport.model.Journal;
import com.psychsupport.webpsychologicalsupport.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JournalRepository extends JpaRepository<Journal, Long> {
    List<Journal> findByUser(User user);
    List<Journal> findByUserOrderByCreatedAtDesc(User user);
    List<Journal> findByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end);
    List<Journal> findBySharedWithPsychologist(User psychologist);
}