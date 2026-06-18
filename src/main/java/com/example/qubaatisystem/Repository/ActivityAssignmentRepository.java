package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Model.ActivityAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityAssignmentRepository extends JpaRepository<ActivityAssignment, Integer> {

    ActivityAssignment findActivityAssignmentById(Integer id);

    List<ActivityAssignment> findActivityAssignmentsByActivityId(Integer activityId);

    List<ActivityAssignment> findActivityAssignmentsByStudentId(Integer studentId);

    List<ActivityAssignment> findActivityAssignmentsByClassroomId(Integer classroomId);

    List<ActivityAssignment> findActivityAssignmentsByActivityIdAndStudentId(Integer activityId, Integer studentId);

    List<ActivityAssignment> findActivityAssignmentsByActivityIdAndClassroomId(Integer activityId, Integer classroomId);
}
