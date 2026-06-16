package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.AuditLogInDTO;
import com.example.qubaatisystem.DTO.Out.AuditLogOutDTO;
import com.example.qubaatisystem.Model.AuditLog;
import com.example.qubaatisystem.Model.User;
import com.example.qubaatisystem.Repository.AuditLogRepository;
import com.example.qubaatisystem.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public List<AuditLogOutDTO> getAll() {
        return auditLogRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public AuditLogOutDTO getById(Integer id) {
        List<AuditLog> auditLogs = auditLogRepository.findAuditLogById(id);
        if (auditLogs.isEmpty()) {
            throw new ApiException("AuditLog with id " + id + " not found");
        }
        return toOut(auditLogs.get(0));
    }

    public void create(AuditLogInDTO dto) {
        AuditLog auditLog = modelMapper.map(dto, AuditLog.class);

        applyRelationships(auditLog, dto);

        auditLogRepository.save(auditLog);
    }

    public void update(Integer id, AuditLogInDTO dto) {
        List<AuditLog> auditLogs = auditLogRepository.findAuditLogById(id);
        if (auditLogs.isEmpty()) {
            throw new ApiException("AuditLog with id " + id + " not found");
        }
        AuditLog auditLog = auditLogs.get(0);

        // Clear relationships first so ModelMapper only copies scalar fields
        // (never mutates the ids of the currently-managed related entities).
        auditLog.setActor(null);
        modelMapper.map(dto, auditLog);

        applyRelationships(auditLog, dto);

        auditLogRepository.save(auditLog);
    }

    public void delete(Integer id) {
        List<AuditLog> auditLogs = auditLogRepository.findAuditLogById(id);
        if (auditLogs.isEmpty()) {
            throw new ApiException("AuditLog with id " + id + " not found");
        }
        auditLogRepository.delete(auditLogs.get(0));
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(AuditLog auditLog, AuditLogInDTO dto) {
        if (dto.getActorId() != null) {
            List<User> users = userRepository.findUserById(dto.getActorId());
            if (users.isEmpty()) {
                throw new ApiException("User with id " + dto.getActorId() + " not found");
            }
            auditLog.setActor(users.get(0));
        } else {
            auditLog.setActor(null);
        }
    }

    private AuditLogOutDTO toOut(AuditLog auditLog) {
        AuditLogOutDTO out = modelMapper.map(auditLog, AuditLogOutDTO.class);
        if (auditLog.getActor() != null) {
            out.setActorId(auditLog.getActor().getId());
            out.setActorUsername(auditLog.getActor().getUsername());
            out.setActorEmail(auditLog.getActor().getEmail());
        }
        return out;
    }
}
