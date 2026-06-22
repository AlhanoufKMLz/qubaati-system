package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    User findUserById(Integer id);

    // Spring Security Basic Auth principal lookup (entity or null — project convention, no Optional).
    User findUserByUsername(String username);
}
