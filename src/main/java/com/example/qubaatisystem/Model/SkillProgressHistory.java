package com.example.qubaatisystem.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "skill_progress_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillProgressHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private Double previousScore;

    @Column
    private Double newScore;

    @Column
    private Integer previousLevel;

    @Column
    private Integer newLevel;

    @Column(length = 2000)
    private String reason;

    @Column
    private java.time.LocalDateTime changedAt;

    // SkillProgressHistory belongs to one Student (owning side, required)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // SkillProgressHistory belongs to one Skill (owning side, required)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    // SkillProgressHistory may reference one StudentSkill (owning side, optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_skill_id")
    private StudentSkill studentSkill;
}
