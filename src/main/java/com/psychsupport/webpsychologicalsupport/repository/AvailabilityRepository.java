package com.psychsupport.webpsychologicalsupport.repository;

import com.psychsupport.webpsychologicalsupport.model.Availability;
import com.psychsupport.webpsychologicalsupport.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findByPsychologist(User psychologist);
    List<Availability> findByPsychologistAndDayOfWeek(User psychologist, DayOfWeek dayOfWeek);
}