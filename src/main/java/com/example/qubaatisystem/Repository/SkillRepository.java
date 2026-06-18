package com.example.qubaatisystem.Repository;

import com.example.qubaatisystem.Enum.SkillType;
import com.example.qubaatisystem.Model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Integer> {

    Skill findSkillById(Integer id);

    List<Skill> findSkillsBySkillType(SkillType skillType);
}
