package com.example.qubaatisystem.Model;

import com.example.qubaatisystem.Enum.LearningStyleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "learning_style_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LearningStyleHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LearningStyleType previousPrimaryStyle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LearningStyleType newPrimaryStyle;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LearningStyleType previousSecondaryStyle;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LearningStyleType newSecondaryStyle;

    @Column
    private Double previousConfidence;

    @Column
    private Double newConfidence;

    @Column(length = 2000)
    private String reason;

    @Column
    private java.time.LocalDateTime changedAt;

    // LearningStyleHistory belongs to one Student (owning side, required)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // LearningStyleHistory may reference one LearningStyle (owning side, optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_style_id")
    private LearningStyle learningStyle;
}
