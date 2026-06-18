package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.StudentSkillInDTO;
import com.example.qubaatisystem.DTO.Out.StudentSkillOutDTO;
import com.example.qubaatisystem.Model.Skill;
import com.example.qubaatisystem.Model.Student;
import com.example.qubaatisystem.Model.StudentSkill;
import com.example.qubaatisystem.Repository.SkillRepository;
import com.example.qubaatisystem.Repository.StudentRepository;
import com.example.qubaatisystem.Repository.StudentSkillRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentSkillService {

    private final StudentSkillRepository studentSkillRepository;
    private final StudentRepository studentRepository;
    private final SkillRepository skillRepository;
    private final ModelMapper modelMapper;

    public List<StudentSkillOutDTO> getAll() {
        return studentSkillRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public StudentSkillOutDTO getById(Integer id) {
        StudentSkill studentSkill = studentSkillRepository.findStudentSkillById(id);
        if (studentSkill == null) {
            throw new ApiException("StudentSkill with id " + id + " not found");
        }
        return toOut(studentSkill);
    }

    public void create(StudentSkillInDTO dto) {
        // Manual scalar mapping: the InDTO holds multiple relation-id fields
        // (studentId, skillId) which ModelMapper would ambiguously match to setId().
        StudentSkill studentSkill = new StudentSkill();
        studentSkill.setScore(dto.getScore());
        studentSkill.setLevel(dto.getLevel());

        applyRelationships(studentSkill, dto);

        studentSkill.setId(null);
        studentSkillRepository.save(studentSkill);
    }

    public void update(Integer id, StudentSkillInDTO dto) {
        StudentSkill studentSkill = studentSkillRepository.findStudentSkillById(id);
        if (studentSkill == null) {
            throw new ApiException("StudentSkill with id " + id + " not found");
        }

        // Manual scalar mapping (ModelMapper would ambiguously match the relation-id
        // fields to setId()); relations are re-resolved by applyRelationships below.
        studentSkill.setScore(dto.getScore());
        studentSkill.setLevel(dto.getLevel());
        studentSkill.setId(id);

        applyRelationships(studentSkill, dto);

        studentSkillRepository.save(studentSkill);
    }

    public void delete(Integer id) {
        StudentSkill studentSkill = studentSkillRepository.findStudentSkillById(id);
        if (studentSkill == null) {
            throw new ApiException("StudentSkill with id " + id + " not found");
        }
        studentSkillRepository.delete(studentSkill);
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(StudentSkill studentSkill, StudentSkillInDTO dto) {
        Student student = studentRepository.findStudentById(dto.getStudentId());
        if (student == null) {
            throw new ApiException("Student with id " + dto.getStudentId() + " not found");
        }
        studentSkill.setStudent(student);

        Skill skill = skillRepository.findSkillById(dto.getSkillId());
        if (skill == null) {
            throw new ApiException("Skill with id " + dto.getSkillId() + " not found");
        }
        studentSkill.setSkill(skill);
    }

    private StudentSkillOutDTO toOut(StudentSkill studentSkill) {
        StudentSkillOutDTO out = modelMapper.map(studentSkill, StudentSkillOutDTO.class);
        // No derived fields to set manually; ModelMapper auto-maps all OutDTO fields
        // (studentId <- student.id, skillId <- skill.id, skillName <- skill.name).
        return out;
    }
}
