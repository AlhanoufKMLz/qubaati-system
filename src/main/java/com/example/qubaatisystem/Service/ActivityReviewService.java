package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.ActivityReviewInDTO;
import com.example.qubaatisystem.DTO.Out.ActivityReviewOutDTO;
import com.example.qubaatisystem.Model.Activity;
import com.example.qubaatisystem.Model.ActivityReview;
import com.example.qubaatisystem.Model.Teacher;
import com.example.qubaatisystem.Repository.ActivityRepository;
import com.example.qubaatisystem.Repository.ActivityReviewRepository;
import com.example.qubaatisystem.Repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityReviewService {

    private final ActivityReviewRepository activityReviewRepository;
    private final ActivityRepository activityRepository;
    private final TeacherRepository teacherRepository;
    private final ModelMapper modelMapper;

    public List<ActivityReviewOutDTO> getAll() {
        return activityReviewRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public ActivityReviewOutDTO getById(Integer id) {
        ActivityReview activityReview = activityReviewRepository.findActivityReviewById(id);
        if (activityReview == null) {
            throw new ApiException("ActivityReview with id " + id + " not found");
        }
        return toOut(activityReview);
    }

    public void create(ActivityReviewInDTO dto) {
        // Map scalar fields manually; relation ids ("...Id") are resolved in applyRelationships.
        ActivityReview activityReview = new ActivityReview();
        activityReview.setDecision(dto.getDecision());
        activityReview.setReviewComment(dto.getReviewComment());
        activityReview.setReviewedAt(dto.getReviewedAt());

        applyRelationships(activityReview, dto);

        activityReview.setId(null);
        activityReviewRepository.save(activityReview);
    }

    public void update(Integer id, ActivityReviewInDTO dto) {
        ActivityReview activityReview = activityReviewRepository.findActivityReviewById(id);
        if (activityReview == null) {
            throw new ApiException("ActivityReview with id " + id + " not found");
        }

        // Map scalar fields manually; relation ids ("...Id") are resolved in applyRelationships.
        activityReview.setDecision(dto.getDecision());
        activityReview.setReviewComment(dto.getReviewComment());
        activityReview.setReviewedAt(dto.getReviewedAt());
        activityReview.setId(id);

        applyRelationships(activityReview, dto);

        activityReviewRepository.save(activityReview);
    }

    public void delete(Integer id) {
        ActivityReview activityReview = activityReviewRepository.findActivityReviewById(id);
        if (activityReview == null) {
            throw new ApiException("ActivityReview with id " + id + " not found");
        }
        activityReviewRepository.delete(activityReview);
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(ActivityReview activityReview, ActivityReviewInDTO dto) {
        Activity activity = activityRepository.findActivityById(dto.getActivityId());
        if (activity == null) {
            throw new ApiException("Activity with id " + dto.getActivityId() + " not found");
        }
        activityReview.setActivity(activity);

        Teacher teacher = teacherRepository.findTeacherById(dto.getTeacherId());
        if (teacher == null) {
            throw new ApiException("Teacher with id " + dto.getTeacherId() + " not found");
        }
        activityReview.setTeacher(teacher);
    }

    private ActivityReviewOutDTO toOut(ActivityReview activityReview) {
        ActivityReviewOutDTO out = modelMapper.map(activityReview, ActivityReviewOutDTO.class);
        if (activityReview.getActivity() != null) {
            out.setActivityId(activityReview.getActivity().getId());
            out.setActivityTitle(activityReview.getActivity().getTitle());
        }
        if (activityReview.getTeacher() != null) {
            out.setTeacherId(activityReview.getTeacher().getId());
            out.setTeacherName(activityReview.getTeacher().getFullName());
        }
        return out;
    }
}
