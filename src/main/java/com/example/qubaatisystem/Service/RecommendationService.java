package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.RecommendationInDTO;
import com.example.qubaatisystem.DTO.Out.RecommendationOutDTO;
import com.example.qubaatisystem.Model.Activity;
import com.example.qubaatisystem.Model.Mission;
import com.example.qubaatisystem.Model.Recommendation;
import com.example.qubaatisystem.Model.Skill;
import com.example.qubaatisystem.Model.Student;
import com.example.qubaatisystem.Repository.ActivityRepository;
import com.example.qubaatisystem.Repository.MissionRepository;
import com.example.qubaatisystem.Repository.RecommendationRepository;
import com.example.qubaatisystem.Repository.SkillRepository;
import com.example.qubaatisystem.Repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final StudentRepository studentRepository;
    private final SkillRepository skillRepository;
    private final MissionRepository missionRepository;
    private final ActivityRepository activityRepository;
    private final ModelMapper modelMapper;

    public List<RecommendationOutDTO> getAll() {
        return recommendationRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public RecommendationOutDTO getById(Integer id) {
        List<Recommendation> recommendations = recommendationRepository.findRecommendationById(id);
        if (recommendations.isEmpty()) {
            throw new ApiException("Recommendation with id " + id + " not found");
        }
        return toOut(recommendations.get(0));
    }

    public void create(RecommendationInDTO dto) {
        Recommendation recommendation = modelMapper.map(dto, Recommendation.class);

        applyRelationships(recommendation, dto);

        recommendationRepository.save(recommendation);
    }

    public void update(Integer id, RecommendationInDTO dto) {
        List<Recommendation> recommendations = recommendationRepository.findRecommendationById(id);
        if (recommendations.isEmpty()) {
            throw new ApiException("Recommendation with id " + id + " not found");
        }
        Recommendation recommendation = recommendations.get(0);

        // Clear relationships first so ModelMapper only copies scalar fields
        // (never mutates the ids of the currently-managed related entities).
        recommendation.setStudent(null);
        recommendation.setSkill(null);
        recommendation.setMission(null);
        recommendation.setActivity(null);
        modelMapper.map(dto, recommendation);

        applyRelationships(recommendation, dto);

        recommendationRepository.save(recommendation);
    }

    public void delete(Integer id) {
        List<Recommendation> recommendations = recommendationRepository.findRecommendationById(id);
        if (recommendations.isEmpty()) {
            throw new ApiException("Recommendation with id " + id + " not found");
        }
        recommendationRepository.delete(recommendations.get(0));
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(Recommendation recommendation, RecommendationInDTO dto) {
        List<Student> students = studentRepository.findStudentById(dto.getStudentId());
        if (students.isEmpty()) {
            throw new ApiException("Student with id " + dto.getStudentId() + " not found");
        }
        recommendation.setStudent(students.get(0));

        if (dto.getSkillId() != null) {
            List<Skill> skills = skillRepository.findSkillById(dto.getSkillId());
            if (skills.isEmpty()) {
                throw new ApiException("Skill with id " + dto.getSkillId() + " not found");
            }
            recommendation.setSkill(skills.get(0));
        } else {
            recommendation.setSkill(null);
        }

        if (dto.getMissionId() != null) {
            List<Mission> missions = missionRepository.findMissionById(dto.getMissionId());
            if (missions.isEmpty()) {
                throw new ApiException("Mission with id " + dto.getMissionId() + " not found");
            }
            recommendation.setMission(missions.get(0));
        } else {
            recommendation.setMission(null);
        }

        if (dto.getActivityId() != null) {
            List<Activity> activities = activityRepository.findActivityById(dto.getActivityId());
            if (activities.isEmpty()) {
                throw new ApiException("Activity with id " + dto.getActivityId() + " not found");
            }
            recommendation.setActivity(activities.get(0));
        } else {
            recommendation.setActivity(null);
        }
    }

    private RecommendationOutDTO toOut(Recommendation recommendation) {
        RecommendationOutDTO out = modelMapper.map(recommendation, RecommendationOutDTO.class);
        if (recommendation.getStudent() != null) {
            out.setStudentId(recommendation.getStudent().getId());
            out.setStudentName(recommendation.getStudent().getFullName());
        }
        if (recommendation.getSkill() != null) {
            out.setSkillId(recommendation.getSkill().getId());
            out.setSkillName(recommendation.getSkill().getName());
        }
        if (recommendation.getMission() != null) {
            out.setMissionId(recommendation.getMission().getId());
            out.setMissionTitle(recommendation.getMission().getTitle());
        }
        if (recommendation.getActivity() != null) {
            out.setActivityId(recommendation.getActivity().getId());
            out.setActivityTitle(recommendation.getActivity().getTitle());
        }
        return out;
    }
}
