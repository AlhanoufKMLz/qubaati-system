package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.SkillProgressHistoryInDTO;
import com.example.qubaatisystem.DTO.Out.SkillProgressHistoryOutDTO;
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
        List<SkillProgressHistory> skillProgressHistories = skillProgressHistoryRepository.findSkillProgressHistoryById(id);
        if (skillProgressHistories.isEmpty()) {
            throw new ApiException("SkillProgressHistory with id " + id + " not found");
        }
        return toOut(skillProgressHistories.get(0));
    }

    public void create(SkillProgressHistoryInDTO dto) {
        SkillProgressHistory skillProgressHistory = modelMapper.map(dto, SkillProgressHistory.class);

        applyRelationships(skillProgressHistory, dto);

        skillProgressHistoryRepository.save(skillProgressHistory);
    }

    public void update(Integer id, SkillProgressHistoryInDTO dto) {
        List<SkillProgressHistory> skillProgressHistories = skillProgressHistoryRepository.findSkillProgressHistoryById(id);
        if (skillProgressHistories.isEmpty()) {
            throw new ApiException("SkillProgressHistory with id " + id + " not found");
        }
        SkillProgressHistory skillProgressHistory = skillProgressHistories.get(0);

        // Clear relationships first so ModelMapper only copies scalar fields
        // (never mutates the ids of the currently-managed related entities).
        skillProgressHistory.setStudent(null);
        skillProgressHistory.setSkill(null);
        skillProgressHistory.setStudentSkill(null);
        modelMapper.map(dto, skillProgressHistory);

        applyRelationships(skillProgressHistory, dto);

        skillProgressHistoryRepository.save(skillProgressHistory);
    }

    public void delete(Integer id) {
        List<SkillProgressHistory> skillProgressHistories = skillProgressHistoryRepository.findSkillProgressHistoryById(id);
        if (skillProgressHistories.isEmpty()) {
            throw new ApiException("SkillProgressHistory with id " + id + " not found");
        }
        skillProgressHistoryRepository.delete(skillProgressHistories.get(0));
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(SkillProgressHistory skillProgressHistory, SkillProgressHistoryInDTO dto) {
        List<Student> students = studentRepository.findStudentById(dto.getStudentId());
        if (students.isEmpty()) {
            throw new ApiException("Student with id " + dto.getStudentId() + " not found");
        }
        skillProgressHistory.setStudent(students.get(0));

        List<Skill> skills = skillRepository.findSkillById(dto.getSkillId());
        if (skills.isEmpty()) {
            throw new ApiException("Skill with id " + dto.getSkillId() + " not found");
        }
        skillProgressHistory.setSkill(skills.get(0));

        if (dto.getStudentSkillId() != null) {
            List<StudentSkill> studentSkills = studentSkillRepository.findStudentSkillById(dto.getStudentSkillId());
            if (studentSkills.isEmpty()) {
                throw new ApiException("StudentSkill with id " + dto.getStudentSkillId() + " not found");
            }
            skillProgressHistory.setStudentSkill(studentSkills.get(0));
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
