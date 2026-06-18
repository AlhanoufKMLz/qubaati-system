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
        Recommendation recommendation = recommendationRepository.findRecommendationById(id);
        if (recommendation == null) {
            throw new ApiException("Recommendation with id " + id + " not found");
        }
        return toOut(recommendation);
    }

    public void create(RecommendationInDTO dto) {
        // Map scalar fields manually; relation ids are resolved in applyRelationships.
        // (ModelMapper can't disambiguate the multiple *Id source fields against setId.)
        Recommendation recommendation = new Recommendation();
        recommendation.setTitle(dto.getTitle());
        recommendation.setDescription(dto.getDescription());
        recommendation.setType(dto.getType());
        recommendation.setPriority(dto.getPriority());
        recommendation.setStatus(dto.getStatus());
        recommendation.setReason(dto.getReason());
        recommendation.setGeneratedAt(dto.getGeneratedAt());

        applyRelationships(recommendation, dto);

        recommendation.setId(null);
        recommendationRepository.save(recommendation);
    }

    public void update(Integer id, RecommendationInDTO dto) {
        Recommendation recommendation = recommendationRepository.findRecommendationById(id);
        if (recommendation == null) {
            throw new ApiException("Recommendation with id " + id + " not found");
        }

        // Map scalar fields manually; applyRelationships re-resolves the relations.
        recommendation.setTitle(dto.getTitle());
        recommendation.setDescription(dto.getDescription());
        recommendation.setType(dto.getType());
        recommendation.setPriority(dto.getPriority());
        recommendation.setStatus(dto.getStatus());
        recommendation.setReason(dto.getReason());
        recommendation.setGeneratedAt(dto.getGeneratedAt());
        recommendation.setId(id);

        applyRelationships(recommendation, dto);

        recommendationRepository.save(recommendation);
    }

    public void delete(Integer id) {
        Recommendation recommendation = recommendationRepository.findRecommendationById(id);
        if (recommendation == null) {
            throw new ApiException("Recommendation with id " + id + " not found");
        }
        recommendationRepository.delete(recommendation);
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(Recommendation recommendation, RecommendationInDTO dto) {
        Student student = studentRepository.findStudentById(dto.getStudentId());
        if (student == null) {
            throw new ApiException("Student with id " + dto.getStudentId() + " not found");
        }
        recommendation.setStudent(student);

        if (dto.getSkillId() != null) {
            Skill skill = skillRepository.findSkillById(dto.getSkillId());
            if (skill == null) {
                throw new ApiException("Skill with id " + dto.getSkillId() + " not found");
            }
            recommendation.setSkill(skill);
        } else {
            recommendation.setSkill(null);
        }

        if (dto.getMissionId() != null) {
            Mission mission = missionRepository.findMissionById(dto.getMissionId());
            if (mission == null) {
                throw new ApiException("Mission with id " + dto.getMissionId() + " not found");
            }
            recommendation.setMission(mission);
        } else {
            recommendation.setMission(null);
        }

        if (dto.getActivityId() != null) {
            Activity activity = activityRepository.findActivityById(dto.getActivityId());
            if (activity == null) {
                throw new ApiException("Activity with id " + dto.getActivityId() + " not found");
            }
            recommendation.setActivity(activity);
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
