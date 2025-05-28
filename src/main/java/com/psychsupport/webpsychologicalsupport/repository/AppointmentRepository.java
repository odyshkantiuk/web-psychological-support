package com.psychsupport.webpsychologicalsupport.repository;

import com.psychsupport.webpsychologicalsupport.model.Appointment;
import com.psychsupport.webpsychologicalsupport.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPsychologist(User psychologist);
    List<Appointment> findByClient(User client);
    List<Appointment> findByPsychologistAndStartTimeBetween(User psychologist, LocalDateTime start, LocalDateTime end);
    List<Appointment> findByClientAndStartTimeBetween(User client, LocalDateTime start, LocalDateTime end);
    List<Appointment> findByStatus(Appointment.Status status);

    @Query("SELECT a FROM Appointment a " +
           "WHERE a.psychologist.id = :psychologistId " +
           "AND a.client.id = :clientId " +
           "AND a.startTime <= :currentTime " +
           "AND a.endTime >= :currentTime " +
           "AND a.status = 'SCHEDULED'")
    Optional<Appointment> findCurrentAppointment(
            @Param("psychologistId") Long psychologistId,
            @Param("clientId") Long clientId,
            @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT a FROM Appointment a " +
           "WHERE a.psychologist.id = :psychologistId " +
           "AND a.client.id != :excludedClientId " +
           "AND a.startTime <= :currentTime " +
           "AND a.endTime >= :currentTime " +
           "AND a.status = 'SCHEDULED'")
    List<Appointment> findCurrentAppointmentsExcludingClient(
            @Param("psychologistId") Long psychologistId,
            @Param("excludedClientId") Long excludedClientId,
            @Param("currentTime") LocalDateTime currentTime);
    
    @Modifying
    @Query("DELETE FROM Appointment a WHERE a.psychologist.id = :userId OR a.client.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}