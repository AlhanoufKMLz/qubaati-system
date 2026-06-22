package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Model.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Integer> {

    Parent findParentById(Integer id);

    // Resolve the parent profile of the authenticated user (entity or null — no Optional).
    Parent findParentByUserId(Integer userId);
}
