package com.example.qubaatisystem.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "teacher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(length = 120)
    private String specialization;

    @Column(length = 30)
    private String phoneNumber;

    // Teacher belongs to one User (owning side)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Teacher has many Classrooms (inverse side)
    @OneToMany(mappedBy = "teacher", fetch = FetchType.LAZY)
    private Set<Classroom> classrooms;

    // Teacher has many ActivityAssignments they assigned (inverse side)
    @OneToMany(mappedBy = "assignedByTeacher", fetch = FetchType.LAZY)
    private Set<ActivityAssignment> activityAssignments;

    // Teacher has many ActivityReviews (inverse side)
    @OneToMany(mappedBy = "teacher", fetch = FetchType.LAZY)
    private Set<ActivityReview> activityReviews;
}
