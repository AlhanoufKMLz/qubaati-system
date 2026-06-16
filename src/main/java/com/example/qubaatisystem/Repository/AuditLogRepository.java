package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    List<AuditLog> findAuditLogById(Integer id);
}
