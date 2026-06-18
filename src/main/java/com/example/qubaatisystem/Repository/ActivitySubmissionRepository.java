package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Enum.ActivitySubmissionStatus;
import com.example.qubaatisystem.Model.ActivitySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivitySubmissionRepository extends JpaRepository<ActivitySubmission, Integer> {

    ActivitySubmission findActivitySubmissionById(Integer id);

    List<ActivitySubmission> findActivitySubmissionsByStudentId(Integer studentId);

    List<ActivitySubmission> findActivitySubmissionsByStatus(ActivitySubmissionStatus status);

    List<ActivitySubmission> findActivitySubmissionsByActivityAssignmentId(Integer activityAssignmentId);

    List<ActivitySubmission> findActivitySubmissionsByStudentIdAndActivityAssignmentId(Integer studentId, Integer activityAssignmentId);
}
