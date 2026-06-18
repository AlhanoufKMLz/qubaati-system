package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.LearningStyleHistoryInDTO;
import com.example.qubaatisystem.DTO.Out.LearningStyleHistoryOutDTO;
import com.example.qubaatisystem.Enum.ActivityType;
import com.example.qubaatisystem.Model.LearningStyle;
import com.example.qubaatisystem.Model.LearningStyleHistory;
import com.example.qubaatisystem.Model.Student;
import com.example.qubaatisystem.Repository.LearningStyleHistoryRepository;
import com.example.qubaatisystem.Repository.LearningStyleRepository;
import com.example.qubaatisystem.Repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningStyleHistoryService {

    private final LearningStyleHistoryRepository learningStyleHistoryRepository;
    private final StudentRepository studentRepository;
    private final LearningStyleRepository learningStyleRepository;
    private final ModelMapper modelMapper;

    public List<LearningStyleHistoryOutDTO> getAll() {
        return learningStyleHistoryRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public LearningStyleHistoryOutDTO getById(Integer id) {
        LearningStyleHistory learningStyleHistory = learningStyleHistoryRepository.findLearningStyleHistoryById(id);
        if (learningStyleHistory == null) {
            throw new ApiException("LearningStyleHistory with id " + id + " not found");
        }
        return toOut(learningStyleHistory);
    }

    public void create(LearningStyleHistoryInDTO dto) {
        // Map scalar fields manually so ModelMapper never tries to resolve the
        // relation-id fields (studentId/learningStyleId) into setId().
        LearningStyleHistory learningStyleHistory = new LearningStyleHistory();
        learningStyleHistory.setPreviousPrimaryStyle(dto.getPreviousPrimaryStyle());
        learningStyleHistory.setNewPrimaryStyle(dto.getNewPrimaryStyle());
        learningStyleHistory.setPreviousSecondaryStyle(dto.getPreviousSecondaryStyle());
        learningStyleHistory.setNewSecondaryStyle(dto.getNewSecondaryStyle());
        learningStyleHistory.setPreviousConfidence(dto.getPreviousConfidence());
        learningStyleHistory.setNewConfidence(dto.getNewConfidence());
        learningStyleHistory.setReason(dto.getReason());
        // changedAt is server-assigned business data, never taken from the client (any DTO value is ignored).
        learningStyleHistory.setChangedAt(LocalDateTime.now());

        applyRelationships(learningStyleHistory, dto);

        learningStyleHistory.setId(null);
        learningStyleHistoryRepository.save(learningStyleHistory);
    }

    public void update(Integer id, LearningStyleHistoryInDTO dto) {
        LearningStyleHistory learningStyleHistory = learningStyleHistoryRepository.findLearningStyleHistoryById(id);
        if (learningStyleHistory == null) {
            throw new ApiException("LearningStyleHistory with id " + id + " not found");
        }

        // Map scalar fields manually onto the managed entity (relations are
        // re-resolved by applyRelationships, so ModelMapper is not used here).
        learningStyleHistory.setPreviousPrimaryStyle(dto.getPreviousPrimaryStyle());
        learningStyleHistory.setNewPrimaryStyle(dto.getNewPrimaryStyle());
        learningStyleHistory.setPreviousSecondaryStyle(dto.getPreviousSecondaryStyle());
        learningStyleHistory.setNewSecondaryStyle(dto.getNewSecondaryStyle());
        learningStyleHistory.setPreviousConfidence(dto.getPreviousConfidence());
        learningStyleHistory.setNewConfidence(dto.getNewConfidence());
        learningStyleHistory.setReason(dto.getReason());
        // changedAt is server-assigned business data, never taken from the client (any DTO value is ignored).
        learningStyleHistory.setChangedAt(LocalDateTime.now());
        learningStyleHistory.setId(id);

        applyRelationships(learningStyleHistory, dto);

        learningStyleHistoryRepository.save(learningStyleHistory);
    }

    public void delete(Integer id) {
        LearningStyleHistory learningStyleHistory = learningStyleHistoryRepository.findLearningStyleHistoryById(id);
        if (learningStyleHistory == null) {
            throw new ApiException("LearningStyleHistory with id " + id + " not found");
        }
        learningStyleHistoryRepository.delete(learningStyleHistory);
    }

    // ---------- automatic update (called after an activity is graded) ----------

    /**
     * Conservative, documented heuristic: full learning-style detection needs mission/session behavioral
     * signals we do not have here, so we only nudge confidence. If the student has a {@link LearningStyle}
     * record and just completed a QUIZ, we gently raise its confidence by 0.05 (capped at 1.0) and record a
     * history row (keeping the same primary/secondary styles). If there is no learning-style record, or the
     * confidence is already capped, we skip — no fake style change. {@code changedAt} is set to now.
     */
    public void recordAutomaticLearningStyleUpdate(Student student, ActivityType activityType, String activityTitle) {
        if (student == null || activityType != ActivityType.QUIZ) {
            return;
        }
        LearningStyle learningStyle = learningStyleRepository.findLearningStyleByStudentId(student.getId());
        if (learningStyle == null) {
            return; // No learning-style record -> skip (documented; needs behavioral signals to detect).
        }
        if (learningStyle.getPrimaryStyle() == null) {
            return; // Defensive: primaryStyle is the non-null column we copy to the history row; never null in practice.
        }
        double previousConfidence = learningStyle.getConfidence() != null ? learningStyle.getConfidence() : 0.5;
        double newConfidence = Math.min(1.0, previousConfidence + 0.05);
        if (newConfidence == previousConfidence) {
            return; // Already capped -> no real change, so no history row is written.
        }
        learningStyle.setConfidence(newConfidence);
        LearningStyle savedLearningStyle = learningStyleRepository.save(learningStyle);

        LearningStyleHistory history = new LearningStyleHistory();
        history.setPreviousPrimaryStyle(savedLearningStyle.getPrimaryStyle());
        history.setNewPrimaryStyle(savedLearningStyle.getPrimaryStyle());
        history.setPreviousSecondaryStyle(savedLearningStyle.getSecondaryStyle());
        history.setNewSecondaryStyle(savedLearningStyle.getSecondaryStyle());
        history.setPreviousConfidence(previousConfidence);
        history.setNewConfidence(newConfidence);
        history.setReason("Learning style confidence updated automatically after completing activity: " + activityTitle);
        history.setChangedAt(LocalDateTime.now());
        history.setStudent(student);
        history.setLearningStyle(savedLearningStyle);
        learningStyleHistoryRepository.save(history);
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(LearningStyleHistory learningStyleHistory, LearningStyleHistoryInDTO dto) {
        Student student = studentRepository.findStudentById(dto.getStudentId());
        if (student == null) {
            throw new ApiException("Student with id " + dto.getStudentId() + " not found");
        }
        learningStyleHistory.setStudent(student);

        if (dto.getLearningStyleId() != null) {
            LearningStyle learningStyle = learningStyleRepository.findLearningStyleById(dto.getLearningStyleId());
            if (learningStyle == null) {
                throw new ApiException("LearningStyle with id " + dto.getLearningStyleId() + " not found");
            }
            learningStyleHistory.setLearningStyle(learningStyle);
        } else {
            learningStyleHistory.setLearningStyle(null);
        }
    }

    private LearningStyleHistoryOutDTO toOut(LearningStyleHistory learningStyleHistory) {
        LearningStyleHistoryOutDTO out = modelMapper.map(learningStyleHistory, LearningStyleHistoryOutDTO.class);
        if (learningStyleHistory.getStudent() != null) {
            out.setStudentId(learningStyleHistory.getStudent().getId());
            out.setStudentName(learningStyleHistory.getStudent().getFullName());
        }
        if (learningStyleHistory.getLearningStyle() != null) {
            out.setLearningStyleId(learningStyleHistory.getLearningStyle().getId());
        }
        return out;
    }
}
