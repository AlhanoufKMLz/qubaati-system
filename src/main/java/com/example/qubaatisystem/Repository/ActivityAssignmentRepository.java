package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Model.ActivityAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityAssignmentRepository extends JpaRepository<ActivityAssignment, Integer> {

    List<ActivityAssignment> findActivityAssignmentById(Integer id);
}
