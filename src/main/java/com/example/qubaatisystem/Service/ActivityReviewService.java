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
        List<ActivityReview> activityReviews = activityReviewRepository.findActivityReviewById(id);
        if (activityReviews.isEmpty()) {
            throw new ApiException("ActivityReview with id " + id + " not found");
        }
        return toOut(activityReviews.get(0));
    }

    public void create(ActivityReviewInDTO dto) {
        ActivityReview activityReview = modelMapper.map(dto, ActivityReview.class);

        applyRelationships(activityReview, dto);

        activityReviewRepository.save(activityReview);
    }

    public void update(Integer id, ActivityReviewInDTO dto) {
        List<ActivityReview> activityReviews = activityReviewRepository.findActivityReviewById(id);
        if (activityReviews.isEmpty()) {
            throw new ApiException("ActivityReview with id " + id + " not found");
        }
        ActivityReview activityReview = activityReviews.get(0);

        // Clear relationships first so ModelMapper only copies scalar fields
        // (never mutates the ids of the currently-managed related entities).
        activityReview.setActivity(null);
        activityReview.setTeacher(null);
        modelMapper.map(dto, activityReview);

        applyRelationships(activityReview, dto);

        activityReviewRepository.save(activityReview);
    }

    public void delete(Integer id) {
        List<ActivityReview> activityReviews = activityReviewRepository.findActivityReviewById(id);
        if (activityReviews.isEmpty()) {
            throw new ApiException("ActivityReview with id " + id + " not found");
        }
        activityReviewRepository.delete(activityReviews.get(0));
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(ActivityReview activityReview, ActivityReviewInDTO dto) {
        List<Activity> activities = activityRepository.findActivityById(dto.getActivityId());
        if (activities.isEmpty()) {
            throw new ApiException("Activity with id " + dto.getActivityId() + " not found");
        }
        activityReview.setActivity(activities.get(0));

        List<Teacher> teachers = teacherRepository.findTeacherById(dto.getTeacherId());
        if (teachers.isEmpty()) {
            throw new ApiException("Teacher with id " + dto.getTeacherId() + " not found");
        }
        activityReview.setTeacher(teachers.get(0));
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
