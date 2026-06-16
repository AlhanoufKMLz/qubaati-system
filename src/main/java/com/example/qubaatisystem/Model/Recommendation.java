package com.example.qubaatisystem.Model;

import com.example.qubaatisystem.Enum.RecommendationPriority;
import com.example.qubaatisystem.Enum.RecommendationStatus;
import com.example.qubaatisystem.Enum.RecommendationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recommendation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecommendationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationStatus status;

    @Column(length = 2000)
    private String reason;

    @Column
    private java.time.LocalDateTime generatedAt;

    // Recommendation belongs to one Student (owning side, required)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // Recommendation may reference one Skill (owning side, optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    // Recommendation may reference one Mission (owning side, optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    // Recommendation may reference one Activity (owning side, optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;
}
