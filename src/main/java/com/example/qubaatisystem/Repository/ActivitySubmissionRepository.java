package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Model.ActivitySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivitySubmissionRepository extends JpaRepository<ActivitySubmission, Integer> {

    List<ActivitySubmission> findActivitySubmissionById(Integer id);
}
