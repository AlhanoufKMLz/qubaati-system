package com.example.qubaatisystem.Model;

import com.example.qubaatisystem.Enum.ActivityStatus;
import com.example.qubaatisystem.Enum.ActivityType;
import com.example.qubaatisystem.Enum.DifficultyLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Set;

@Entity
@Table(name = "activity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DifficultyLevel difficulty;

    @Column
    private Integer maxScore;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    // Activity has many Questions (inverse side)
    @OneToMany(mappedBy = "activity", fetch = FetchType.LAZY)
    private Set<Question> questions;

    // Activity has many ActivityAssignments (inverse side)
    @OneToMany(mappedBy = "activity", fetch = FetchType.LAZY)
    private Set<ActivityAssignment> activityAssignments;

    // Activity has many ActivityReviews (inverse side)
    @OneToMany(mappedBy = "activity", fetch = FetchType.LAZY)
    private Set<ActivityReview> activityReviews;
}
