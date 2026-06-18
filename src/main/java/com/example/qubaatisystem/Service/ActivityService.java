package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.ActivityInDTO;
import com.example.qubaatisystem.DTO.Out.ActivityOutDTO;
import com.example.qubaatisystem.DTO.Out.ActivityReviewOutDTO;
import com.example.qubaatisystem.Enum.ActivityReviewDecision;
import com.example.qubaatisystem.Enum.ActivityStatus;
import com.example.qubaatisystem.Model.Activity;
import com.example.qubaatisystem.Model.ActivityReview;
import com.example.qubaatisystem.Model.Teacher;
import com.example.qubaatisystem.Repository.ActivityRepository;
import com.example.qubaatisystem.Repository.ActivityReviewRepository;
import com.example.qubaatisystem.Repository.QuestionRepository;
import com.example.qubaatisystem.Repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final TeacherRepository teacherRepository;
    private final ActivityReviewRepository activityReviewRepository;
    private final QuestionRepository questionRepository;
    private final ModelMapper modelMapper;

    public List<ActivityOutDTO> getAll() {
        return activityRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public ActivityOutDTO getById(Integer id) {
        Activity activity = activityRepository.findActivityById(id);
        if (activity == null) {
            throw new ApiException("Activity with id " + id + " not found");
        }
        return toOut(activity);
    }

    public void create(ActivityInDTO dto) {
        Activity activity = modelMapper.map(dto, Activity.class);

        activity.setId(null);
        activityRepository.save(activity);
    }

    public void update(Integer id, ActivityInDTO dto) {
        Activity activity = activityRepository.findActivityById(id);
        if (activity == null) {
            throw new ApiException("Activity with id " + id + " not found");
        }

        modelMapper.map(dto, activity);
        activity.setId(id);

        activityRepository.save(activity);
    }

    public void delete(Integer id) {
        Activity activity = activityRepository.findActivityById(id);
        if (activity == null) {
            throw new ApiException("Activity with id " + id + " not found");
        }
        activityRepository.delete(activity);
    }

    // ====================== REVIEW / APPROVAL FLOW ======================

    public void submitForReview(Integer activityId) {
        Activity activity = requireActivity(activityId);
        if (activity.getStatus() == ActivityStatus.APPROVED) {
            throw new ApiException("Activity is already APPROVED");
        }
        if (activity.getStatus() == ActivityStatus.PENDING_REVIEW) {
            throw new ApiException("Activity is already PENDING_REVIEW");
        }
        if (activity.getStatus() != ActivityStatus.DRAFT && activity.getStatus() != ActivityStatus.REJECTED) {
            throw new ApiException("Only DRAFT or REJECTED activities can be submitted for review");
        }
        if (activity.getTitle() == null || activity.getTitle().isBlank()
                || activity.getDescription() == null || activity.getDescription().isBlank()
                || activity.getType() == null || activity.getDifficulty() == null) {
            throw new ApiException("Activity must have a valid title, description, type and difficulty before review");
        }
        if (questionRepository.findQuestionsByActivityId(activityId).isEmpty()) {
            throw new ApiException("Activity must have at least one question before it can be submitted for review");
        }
        activity.setStatus(ActivityStatus.PENDING_REVIEW);
        activityRepository.save(activity);
    }

    public List<ActivityOutDTO> getReviewQueue() {
        return activityRepository.findActivitiesByStatus(ActivityStatus.PENDING_REVIEW)
                .stream()
                .map(this::toOut)
                .toList();
    }

    public void approveActivity(Integer activityId, Integer teacherId, String reviewComment) {
        Activity activity = requireActivity(activityId);
        Teacher teacher = requireTeacher(teacherId);
        if (activity.getStatus() != ActivityStatus.PENDING_REVIEW) {
            throw new ApiException("Only PENDING_REVIEW activities can be approved");
        }
        String comment = (reviewComment == null || reviewComment.isBlank()) ? "Approved" : reviewComment;
        saveReview(activity, teacher, ActivityReviewDecision.APPROVED, comment);
        activity.setStatus(ActivityStatus.APPROVED);
        activityRepository.save(activity);
    }

    public void rejectActivity(Integer activityId, Integer teacherId, String reviewComment) {
        Activity activity = requireActivity(activityId);
        Teacher teacher = requireTeacher(teacherId);
        if (activity.getStatus() != ActivityStatus.PENDING_REVIEW) {
            throw new ApiException("Only PENDING_REVIEW activities can be rejected");
        }
        String comment = (reviewComment == null || reviewComment.isBlank()) ? "Rejected" : reviewComment;
        saveReview(activity, teacher, ActivityReviewDecision.REJECTED, comment);
        activity.setStatus(ActivityStatus.REJECTED);
        activityRepository.save(activity);
    }

    public void requestRevision(Integer activityId, Integer teacherId, String reviewComment) {
        Activity activity = requireActivity(activityId);
        Teacher teacher = requireTeacher(teacherId);
        if (activity.getStatus() != ActivityStatus.PENDING_REVIEW) {
            throw new ApiException("Only PENDING_REVIEW activities can be sent back for revision");
        }
        String comment = (reviewComment == null || reviewComment.isBlank()) ? "Revision requested" : reviewComment;
        saveReview(activity, teacher, ActivityReviewDecision.REVISION_REQUESTED, comment);
        // Preferred convention: send the activity back to DRAFT so it can be edited/refined again.
        activity.setStatus(ActivityStatus.DRAFT);
        activityRepository.save(activity);
    }

    public List<ActivityReviewOutDTO> getReviewHistory(Integer activityId) {
        requireActivity(activityId);
        return activityReviewRepository.findActivityReviewsByActivityId(activityId)
                .stream()
                .map(this::toReviewOut)
                .toList();
    }

    // ====================== helpers ======================

    private Activity requireActivity(Integer activityId) {
        Activity activity = activityRepository.findActivityById(activityId);
        if (activity == null) {
            throw new ApiException("Activity with id " + activityId + " not found");
        }
        return activity;
    }

    private Teacher requireTeacher(Integer teacherId) {
        Teacher teacher = teacherRepository.findTeacherById(teacherId);
        if (teacher == null) {
            throw new ApiException("Teacher with id " + teacherId + " not found");
        }
        return teacher;
    }

    // Review data is stored in ActivityReview (Activity.reviewedAt/reviewComment were removed).
    private void saveReview(Activity activity, Teacher teacher, ActivityReviewDecision decision, String comment) {
        ActivityReview review = new ActivityReview();
        review.setActivity(activity);
        review.setTeacher(teacher);
        review.setDecision(decision);
        review.setReviewComment(comment);
        review.setReviewedAt(LocalDateTime.now());
        activityReviewRepository.save(review);
    }

    private ActivityReviewOutDTO toReviewOut(ActivityReview review) {
        ActivityReviewOutDTO out = modelMapper.map(review, ActivityReviewOutDTO.class);
        if (review.getActivity() != null) {
            out.setActivityId(review.getActivity().getId());
            out.setActivityTitle(review.getActivity().getTitle());
        }
        if (review.getTeacher() != null) {
            out.setTeacherId(review.getTeacher().getId());
            out.setTeacherName(review.getTeacher().getFullName());
        }
        return out;
    }

    private ActivityOutDTO toOut(Activity activity) {
        return modelMapper.map(activity, ActivityOutDTO.class);
    }
}
