package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.SkillProgressHistoryInDTO;
import com.example.qubaatisystem.DTO.Out.SkillProgressHistoryOutDTO;
import com.example.qubaatisystem.Enum.SkillType;
import com.example.qubaatisystem.Model.Skill;
import com.example.qubaatisystem.Model.SkillProgressHistory;
import com.example.qubaatisystem.Model.Student;
import com.example.qubaatisystem.Model.StudentSkill;
import com.example.qubaatisystem.Repository.SkillProgressHistoryRepository;
import com.example.qubaatisystem.Repository.SkillRepository;
import com.example.qubaatisystem.Repository.StudentRepository;
import com.example.qubaatisystem.Repository.StudentSkillRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillProgressHistoryService {

    private final SkillProgressHistoryRepository skillProgressHistoryRepository;
    private final StudentRepository studentRepository;
    private final SkillRepository skillRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final ModelMapper modelMapper;

    public List<SkillProgressHistoryOutDTO> getAll() {
        return skillProgressHistoryRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public SkillProgressHistoryOutDTO getById(Integer id) {
        SkillProgressHistory skillProgressHistory = skillProgressHistoryRepository.findSkillProgressHistoryById(id);
        if (skillProgressHistory == null) {
            throw new ApiException("SkillProgressHistory with id " + id + " not found");
        }
        return toOut(skillProgressHistory);
    }

    public void create(SkillProgressHistoryInDTO dto) {
        // Manual scalar mapping (DTO has multiple *Id relation fields that would make
        // ModelMapper ambiguous on setId()); relations are resolved by applyRelationships.
        SkillProgressHistory skillProgressHistory = new SkillProgressHistory();
        skillProgressHistory.setPreviousScore(dto.getPreviousScore());
        skillProgressHistory.setNewScore(dto.getNewScore());
        skillProgressHistory.setPreviousLevel(dto.getPreviousLevel());
        skillProgressHistory.setNewLevel(dto.getNewLevel());
        skillProgressHistory.setReason(dto.getReason());
        // changedAt is server-assigned business data, never taken from the client (any DTO value is ignored).
        skillProgressHistory.setChangedAt(LocalDateTime.now());

        applyRelationships(skillProgressHistory, dto);

        skillProgressHistory.setId(null);
        skillProgressHistoryRepository.save(skillProgressHistory);
    }

    public void update(Integer id, SkillProgressHistoryInDTO dto) {
        SkillProgressHistory skillProgressHistory = skillProgressHistoryRepository.findSkillProgressHistoryById(id);
        if (skillProgressHistory == null) {
            throw new ApiException("SkillProgressHistory with id " + id + " not found");
        }

        // Manual scalar mapping (relations are re-resolved by applyRelationships).
        skillProgressHistory.setPreviousScore(dto.getPreviousScore());
        skillProgressHistory.setNewScore(dto.getNewScore());
        skillProgressHistory.setPreviousLevel(dto.getPreviousLevel());
        skillProgressHistory.setNewLevel(dto.getNewLevel());
        skillProgressHistory.setReason(dto.getReason());
        // changedAt is server-assigned business data, never taken from the client (any DTO value is ignored).
        skillProgressHistory.setChangedAt(LocalDateTime.now());
        skillProgressHistory.setId(id);

        applyRelationships(skillProgressHistory, dto);

        skillProgressHistoryRepository.save(skillProgressHistory);
    }

    public void delete(Integer id) {
        SkillProgressHistory skillProgressHistory = skillProgressHistoryRepository.findSkillProgressHistoryById(id);
        if (skillProgressHistory == null) {
            throw new ApiException("SkillProgressHistory with id " + id + " not found");
        }
        skillProgressHistoryRepository.delete(skillProgressHistory);
    }

    // ---------- automatic progress (called after an activity is graded) ----------

    /**
     * Minimal, documented heuristic: after an activity is graded, record skill progress against the first
     * skill of type {@link SkillType#PROBLEM_SOLVING} (the project has no Activity→Skill mapping yet). The
     * new score is the activity percentage on a 0–100 scale and the level is derived in 1–5 bands. If no
     * PROBLEM_SOLVING skill exists, the update is skipped (no fake data). {@code changedAt} is set to now.
     */
    public void recordAutomaticSkillProgress(Student student, int score, int maxScore, String activityTitle) {
        if (student == null) {
            return;
        }
        List<Skill> skills = skillRepository.findSkillsBySkillType(SkillType.PROBLEM_SOLVING);
        if (skills.isEmpty()) {
            return; // No skill to map the activity onto yet -> skip (documented in the Postman README).
        }
        Skill skill = skills.get(0);
        double percentage = maxScore > 0 ? (double) score / maxScore : 0.0;
        double newScore = Math.round(percentage * 100.0);
        int newLevel = computeLevel(newScore);

        StudentSkill studentSkill = studentSkillRepository.findStudentSkillByStudentIdAndSkillId(student.getId(), skill.getId());
        Double previousScore = null;
        Integer previousLevel = null;
        if (studentSkill == null) {
            studentSkill = new StudentSkill();
            studentSkill.setStudent(student);
            studentSkill.setSkill(skill);
        } else {
            previousScore = studentSkill.getScore();
            previousLevel = studentSkill.getLevel();
        }
        studentSkill.setScore(newScore);
        studentSkill.setLevel(newLevel);
        StudentSkill savedStudentSkill = studentSkillRepository.save(studentSkill);

        SkillProgressHistory history = new SkillProgressHistory();
        history.setPreviousScore(previousScore);
        history.setNewScore(newScore);
        history.setPreviousLevel(previousLevel);
        history.setNewLevel(newLevel);
        history.setReason("Updated automatically after completing activity: " + activityTitle);
        history.setChangedAt(LocalDateTime.now());
        history.setStudent(student);
        history.setSkill(skill);
        history.setStudentSkill(savedStudentSkill);
        skillProgressHistoryRepository.save(history);
    }

    private int computeLevel(double score0to100) {
        if (score0to100 >= 90) return 5;
        if (score0to100 >= 75) return 4;
        if (score0to100 >= 50) return 3;
        if (score0to100 >= 25) return 2;
        return 1;
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(SkillProgressHistory skillProgressHistory, SkillProgressHistoryInDTO dto) {
        Student student = studentRepository.findStudentById(dto.getStudentId());
        if (student == null) {
            throw new ApiException("Student with id " + dto.getStudentId() + " not found");
        }
        skillProgressHistory.setStudent(student);

        Skill skill = skillRepository.findSkillById(dto.getSkillId());
        if (skill == null) {
            throw new ApiException("Skill with id " + dto.getSkillId() + " not found");
        }
        skillProgressHistory.setSkill(skill);

        if (dto.getStudentSkillId() != null) {
            StudentSkill studentSkill = studentSkillRepository.findStudentSkillById(dto.getStudentSkillId());
            if (studentSkill == null) {
                throw new ApiException("StudentSkill with id " + dto.getStudentSkillId() + " not found");
            }
            skillProgressHistory.setStudentSkill(studentSkill);
        } else {
            skillProgressHistory.setStudentSkill(null);
        }
    }

    private SkillProgressHistoryOutDTO toOut(SkillProgressHistory skillProgressHistory) {
        SkillProgressHistoryOutDTO out = modelMapper.map(skillProgressHistory, SkillProgressHistoryOutDTO.class);
        if (skillProgressHistory.getStudent() != null) {
            out.setStudentId(skillProgressHistory.getStudent().getId());
            out.setStudentName(skillProgressHistory.getStudent().getFullName());
        }
        if (skillProgressHistory.getSkill() != null) {
            out.setSkillId(skillProgressHistory.getSkill().getId());
            out.setSkillName(skillProgressHistory.getSkill().getName());
        }
        if (skillProgressHistory.getStudentSkill() != null) {
            out.setStudentSkillId(skillProgressHistory.getStudentSkill().getId());
        }
        return out;
    }
}
