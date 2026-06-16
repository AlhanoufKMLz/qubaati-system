package com.example.qubaatisystem.Model;

import com.example.qubaatisystem.Enum.AnswerStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "student_answer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 2000)
    private String answerText;

    @Column
    private Boolean isCorrect;

    @Column
    private Integer earnedPoints;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AnswerStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private java.time.LocalDateTime answeredAt;

    // StudentAnswer belongs to one Question (owning side)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // StudentAnswer belongs to one Student (owning side)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    // StudentAnswer belongs to one ActivitySubmission (owning side)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_submission_id")
    private ActivitySubmission activitySubmission;
}
