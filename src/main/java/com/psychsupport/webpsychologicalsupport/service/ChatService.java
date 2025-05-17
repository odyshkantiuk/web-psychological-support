package com.psychsupport.webpsychologicalsupport.service;

import com.psychsupport.webpsychologicalsupport.model.Appointment;
import com.psychsupport.webpsychologicalsupport.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ChatService {
    private static final Logger logger = Logger.getLogger(ChatService.class.getName());

    private final Map<Long, Boolean> connectedUsers = new ConcurrentHashMap<>();
    
    @Autowired
    private AppointmentRepository appointmentRepository;

    public void userConnected(Long userId) {
        connectedUsers.put(userId, true);
    }

    public void userDisconnected(Long userId) {
        connectedUsers.remove(userId);
    }

    public boolean isUserConnected(Long userId) {
        return connectedUsers.getOrDefault(userId, false);
    }

    public enum StatusType {
        WAITING_FOR_CLIENT,
        WAITING_FOR_PSYCHOLOGIST,
        CONNECTED,
        CONNECTED_FOR_APPOINTMENT,
        OFFLINE,
        BUSY
    }

    public StatusType getClientStatus(Long psychologistId, Long clientId) {
        try {
            boolean isClientConnected = isUserConnected(clientId);

            boolean hasCurrentAppointment = hasCurrentAppointment(psychologistId, clientId);
            
            if (hasCurrentAppointment) {
                if (isClientConnected) {
                    return StatusType.CONNECTED_FOR_APPOINTMENT;
                } else {
                    return StatusType.WAITING_FOR_CLIENT;
                }
            } else {
                if (isClientConnected) {
                    return StatusType.CONNECTED;
                } else {
                    return StatusType.OFFLINE;
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting client status", e);
            return StatusType.OFFLINE;
        }
    }

    public StatusType getPsychologistStatus(Long clientId, Long psychologistId) {
        try {
            boolean isPsychologistConnected = isUserConnected(psychologistId);

            boolean hasCurrentAppointment = hasCurrentAppointment(psychologistId, clientId);
            
            if (!isPsychologistConnected) {
                if (hasCurrentAppointment) {
                    return StatusType.WAITING_FOR_PSYCHOLOGIST;
                } else {
                    return StatusType.OFFLINE;
                }
            } else {
                if (isPsychologistBusyWithOtherClient(psychologistId, clientId)) {
                    return StatusType.BUSY;
                } else if (hasCurrentAppointment) {
                    return StatusType.CONNECTED_FOR_APPOINTMENT;
                } else {
                    return StatusType.CONNECTED;
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting psychologist status", e);
            return StatusType.OFFLINE;
        }
    }

    private boolean hasCurrentAppointment(Long psychologistId, Long clientId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            return appointmentRepository.findCurrentAppointment(psychologistId, clientId, now)
                    .isPresent();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error checking current appointment", e);
            return false;
        }
    }

    private boolean isPsychologistBusyWithOtherClient(Long psychologistId, Long clientId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Appointment> otherAppointments = appointmentRepository.findCurrentAppointmentsExcludingClient(
                    psychologistId, clientId, now);

            return otherAppointments.stream()
                    .anyMatch(appointment -> isUserConnected(appointment.getClient().getId()));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error checking if psychologist is busy", e);
            return false;
        }
    }
}