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
        List<Notification> notifications = notificationRepository.findNotificationById(id);
        if (notifications.isEmpty()) {
            throw new ApiException("Notification with id " + id + " not found");
        }
        return toOut(notifications.get(0));
    }

    public void create(NotificationInDTO dto) {
        Notification notification = modelMapper.map(dto, Notification.class);

        applyRelationships(notification, dto);

        notificationRepository.save(notification);
    }

    public void update(Integer id, NotificationInDTO dto) {
        List<Notification> notifications = notificationRepository.findNotificationById(id);
        if (notifications.isEmpty()) {
            throw new ApiException("Notification with id " + id + " not found");
        }
        Notification notification = notifications.get(0);

        // Clear relationships first so ModelMapper only copies scalar fields
        // (never mutates the ids of the currently-managed related entities).
        notification.setRecipient(null);
        modelMapper.map(dto, notification);

        applyRelationships(notification, dto);

        notificationRepository.save(notification);
    }

    public void delete(Integer id) {
        List<Notification> notifications = notificationRepository.findNotificationById(id);
        if (notifications.isEmpty()) {
            throw new ApiException("Notification with id " + id + " not found");
        }
        notificationRepository.delete(notifications.get(0));
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(Notification notification, NotificationInDTO dto) {
        List<User> recipients = userRepository.findUserById(dto.getRecipientId());
        if (recipients.isEmpty()) {
            throw new ApiException("User with id " + dto.getRecipientId() + " not found");
        }
        notification.setRecipient(recipients.get(0));
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
