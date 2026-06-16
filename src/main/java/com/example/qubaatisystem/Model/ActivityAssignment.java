package com.example.qubaatisystem.Model;

import com.example.qubaatisystem.Enum.ActivityAssignmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "activity_assignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private java.time.LocalDateTime assignedAt;

    @Column
    private java.time.LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityAssignmentStatus status;

    // ActivityAssignment belongs to one Activity (owning side, required)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    // ActivityAssignment may belong to one Student (owning side, optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    // ActivityAssignment may belong to one Classroom (owning side, optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    // ActivityAssignment is assigned by one Teacher (owning side, required)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_teacher_id", nullable = false)
    private Teacher assignedByTeacher;
}
