package com.example.qubaatisystem.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false)
    private Integer age;

    @Column(length = 50)
    private String grade;

    @Column(nullable = false)
    private Integer totalPoints;

    @Column(nullable = false)
    private Integer completedMissionsCount;

    // Student belongs to one User (owning side). The account is created for the child by the Parent.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Student belongs to one Parent (owning side)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    // Student belongs to one Classroom (owning side, optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    // Student has many StudentSkills (inverse side)
    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private Set<StudentSkill> studentSkills;

    // Student has one LearningStyle (inverse side)
    @OneToOne(mappedBy = "student", fetch = FetchType.LAZY)
    private LearningStyle learningStyle;

    // Student has many ActivitySubmissions (inverse side)
    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private Set<ActivitySubmission> activitySubmissions;

    // Student has many Recommendations (inverse side)
    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private Set<Recommendation> recommendations;

    // Student has many SkillProgressHistory entries (inverse side)
    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private Set<SkillProgressHistory> skillProgressHistory;

    // Student has many LearningStyleHistory entries (inverse side)
    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private Set<LearningStyleHistory> learningStyleHistory;

    // Student has many StudentAnswers (inverse side)
    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private Set<StudentAnswer> studentAnswers;
}
