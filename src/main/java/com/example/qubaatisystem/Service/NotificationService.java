package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.NotificationInDTO;
import com.example.qubaatisystem.DTO.Out.NotificationOutDTO;
import com.example.qubaatisystem.Model.Notification;
import com.example.qubaatisystem.Model.User;
import com.example.qubaatisystem.Repository.NotificationRepository;
import com.example.qubaatisystem.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public List<NotificationOutDTO> getAll() {
        return notificationRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public NotificationOutDTO getById(Integer id) {
        Notification notification = notificationRepository.findNotificationById(id);
        if (notification == null) {
            throw new ApiException("Notification with id " + id + " not found");
        }
        return toOut(notification);
    }

    public void create(NotificationInDTO dto) {
        Notification notification = modelMapper.map(dto, Notification.class);

        applyRelationships(notification, dto);

        notification.setId(null);
        notificationRepository.save(notification);
    }

    public void update(Integer id, NotificationInDTO dto) {
        Notification notification = notificationRepository.findNotificationById(id);
        if (notification == null) {
            throw new ApiException("Notification with id " + id + " not found");
        }

        // Clear relationships first so ModelMapper only copies scalar fields
        // (never mutates the ids of the currently-managed related entities).
        notification.setRecipient(null);
        modelMapper.map(dto, notification);
        notification.setId(id);

        applyRelationships(notification, dto);

        notificationRepository.save(notification);
    }

    public void delete(Integer id) {
        Notification notification = notificationRepository.findNotificationById(id);
        if (notification == null) {
            throw new ApiException("Notification with id " + id + " not found");
        }
        notificationRepository.delete(notification);
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(Notification notification, NotificationInDTO dto) {
        User recipient = userRepository.findUserById(dto.getRecipientId());
        if (recipient == null) {
            throw new ApiException("User with id " + dto.getRecipientId() + " not found");
        }
        notification.setRecipient(recipient);
    }

    private NotificationOutDTO toOut(Notification notification) {
        NotificationOutDTO out = modelMapper.map(notification, NotificationOutDTO.class);
        if (notification.getRecipient() != null) {
            out.setRecipientId(notification.getRecipient().getId());
            out.setRecipientUsername(notification.getRecipient().getUsername());
            out.setRecipientEmail(notification.getRecipient().getEmail());
        }
        return out;
    }
}
