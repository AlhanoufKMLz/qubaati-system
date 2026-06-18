package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Enum.ActivityStatus;
import com.example.qubaatisystem.Model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Integer> {

    Activity findActivityById(Integer id);

    List<Activity> findActivitiesByStatus(ActivityStatus status);
}
