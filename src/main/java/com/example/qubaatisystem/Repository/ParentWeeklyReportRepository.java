package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Model.ParentWeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParentWeeklyReportRepository extends JpaRepository<ParentWeeklyReport, Integer> {

    // Singular PK finder (project convention — returns the entity or null, never Optional).
    ParentWeeklyReport findParentWeeklyReportById(Integer id);

    // All reports for a parent, newest first.
    List<ParentWeeklyReport> findParentWeeklyReportsByParentIdOrderByGeneratedAtDesc(Integer parentId);

    // The most recent report for a parent (or null).
    ParentWeeklyReport findFirstParentWeeklyReportByParentIdOrderByGeneratedAtDesc(Integer parentId);
}
