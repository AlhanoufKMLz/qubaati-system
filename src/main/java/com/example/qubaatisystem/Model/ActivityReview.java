package com.example.qubaatisystem.Model;

import com.example.qubaatisystem.Enum.ActivityReviewDecision;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "activity_review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityReviewDecision decision;

    @Column(length = 1000)
    private String reviewComment;

    @Column
    private java.time.LocalDateTime reviewedAt;

    // ActivityReview belongs to one Activity (owning side)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    // ActivityReview belongs to one Teacher (owning side)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;
}
