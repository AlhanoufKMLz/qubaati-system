package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.LearningStyleHistoryInDTO;
import com.example.qubaatisystem.DTO.Out.LearningStyleHistoryOutDTO;
import com.example.qubaatisystem.Model.LearningStyle;
import com.example.qubaatisystem.Model.LearningStyleHistory;
import com.example.qubaatisystem.Model.Student;
import com.example.qubaatisystem.Repository.LearningStyleHistoryRepository;
import com.example.qubaatisystem.Repository.LearningStyleRepository;
import com.example.qubaatisystem.Repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

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
        List<LearningStyleHistory> learningStyleHistories = learningStyleHistoryRepository.findLearningStyleHistoryById(id);
        if (learningStyleHistories.isEmpty()) {
            throw new ApiException("LearningStyleHistory with id " + id + " not found");
        }
        return toOut(learningStyleHistories.get(0));
    }

    public void create(LearningStyleHistoryInDTO dto) {
        LearningStyleHistory learningStyleHistory = modelMapper.map(dto, LearningStyleHistory.class);

        applyRelationships(learningStyleHistory, dto);

        learningStyleHistoryRepository.save(learningStyleHistory);
    }

    public void update(Integer id, LearningStyleHistoryInDTO dto) {
        List<LearningStyleHistory> learningStyleHistories = learningStyleHistoryRepository.findLearningStyleHistoryById(id);
        if (learningStyleHistories.isEmpty()) {
            throw new ApiException("LearningStyleHistory with id " + id + " not found");
        }
        LearningStyleHistory learningStyleHistory = learningStyleHistories.get(0);

        // Clear relationships first so ModelMapper only copies scalar fields
        // (never mutates the ids of the currently-managed related entities).
        learningStyleHistory.setStudent(null);
        learningStyleHistory.setLearningStyle(null);
        modelMapper.map(dto, learningStyleHistory);

        applyRelationships(learningStyleHistory, dto);

        learningStyleHistoryRepository.save(learningStyleHistory);
    }

    public void delete(Integer id) {
        List<LearningStyleHistory> learningStyleHistories = learningStyleHistoryRepository.findLearningStyleHistoryById(id);
        if (learningStyleHistories.isEmpty()) {
            throw new ApiException("LearningStyleHistory with id " + id + " not found");
        }
        learningStyleHistoryRepository.delete(learningStyleHistories.get(0));
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(LearningStyleHistory learningStyleHistory, LearningStyleHistoryInDTO dto) {
        List<Student> students = studentRepository.findStudentById(dto.getStudentId());
        if (students.isEmpty()) {
            throw new ApiException("Student with id " + dto.getStudentId() + " not found");
        }
        learningStyleHistory.setStudent(students.get(0));

        if (dto.getLearningStyleId() != null) {
            List<LearningStyle> learningStyles = learningStyleRepository.findLearningStyleById(dto.getLearningStyleId());
            if (learningStyles.isEmpty()) {
                throw new ApiException("LearningStyle with id " + dto.getLearningStyleId() + " not found");
            }
            learningStyleHistory.setLearningStyle(learningStyles.get(0));
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
