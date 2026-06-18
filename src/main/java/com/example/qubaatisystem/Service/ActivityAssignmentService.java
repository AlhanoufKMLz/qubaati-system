package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.ActivityAssignInDTO;
import com.example.qubaatisystem.DTO.In.ActivityAssignmentBulkInDTO;
import com.example.qubaatisystem.DTO.In.ActivityAssignmentDeadlineInDTO;
import com.example.qubaatisystem.DTO.In.ActivityAssignmentInDTO;
import com.example.qubaatisystem.DTO.Out.ActivityAssignmentOutDTO;
import com.example.qubaatisystem.Enum.ActivityAssignmentStatus;
import com.example.qubaatisystem.Enum.ActivityStatus;
import com.example.qubaatisystem.Enum.ActivitySubmissionStatus;
import com.example.qubaatisystem.Model.Activity;
import com.example.qubaatisystem.Model.ActivityAssignment;
import com.example.qubaatisystem.Model.ActivitySubmission;
import com.example.qubaatisystem.Model.Classroom;
import com.example.qubaatisystem.Model.Student;
import com.example.qubaatisystem.Model.Teacher;
import com.example.qubaatisystem.Repository.ActivityAssignmentRepository;
import com.example.qubaatisystem.Repository.ActivityRepository;
import com.example.qubaatisystem.Repository.ActivitySubmissionRepository;
import com.example.qubaatisystem.Repository.ClassroomRepository;
import com.example.qubaatisystem.Repository.StudentRepository;
import com.example.qubaatisystem.Repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActivityAssignmentService {

    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final ActivityRepository activityRepository;
    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final TeacherRepository teacherRepository;
    private final ActivitySubmissionRepository activitySubmissionRepository;
    private final ModelMapper modelMapper;

    public List<ActivityAssignmentOutDTO> getAll() {
        return activityAssignmentRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public ActivityAssignmentOutDTO getById(Integer id) {
        ActivityAssignment activityAssignment = activityAssignmentRepository.findActivityAssignmentById(id);
        if (activityAssignment == null) {
            throw new ApiException("ActivityAssignment with id " + id + " not found");
        }
        return toOut(activityAssignment);
    }

    public void create(ActivityAssignmentInDTO dto) {
        // Map scalar fields manually; relation-id fields (activityId, studentId,
        // classroomId, assignedByTeacherId) are resolved in applyRelationships.
        ActivityAssignment activityAssignment = new ActivityAssignment();
        activityAssignment.setAssignedAt(dto.getAssignedAt());
        activityAssignment.setDueDate(dto.getDueDate());
        activityAssignment.setStatus(dto.getStatus());

        applyRelationships(activityAssignment, dto);

        activityAssignment.setId(null);
        activityAssignmentRepository.save(activityAssignment);
    }

    public void update(Integer id, ActivityAssignmentInDTO dto) {
        ActivityAssignment activityAssignment = activityAssignmentRepository.findActivityAssignmentById(id);
        if (activityAssignment == null) {
            throw new ApiException("ActivityAssignment with id " + id + " not found");
        }

        // Map scalar fields manually; relations are re-resolved in applyRelationships.
        activityAssignment.setAssignedAt(dto.getAssignedAt());
        activityAssignment.setDueDate(dto.getDueDate());
        activityAssignment.setStatus(dto.getStatus());
        activityAssignment.setId(id);

        applyRelationships(activityAssignment, dto);

        activityAssignmentRepository.save(activityAssignment);
    }

    public void delete(Integer id) {
        ActivityAssignment activityAssignment = activityAssignmentRepository.findActivityAssignmentById(id);
        if (activityAssignment == null) {
            throw new ApiException("ActivityAssignment with id " + id + " not found");
        }
        activityAssignmentRepository.delete(activityAssignment);
    }

    // ====================== FLOW: ASSIGNMENT ======================

    public ActivityAssignmentOutDTO assignToStudent(Integer activityId, Integer studentId, ActivityAssignInDTO dto) {
        Activity activity = requireApprovedActivity(activityId);
        Student student = requireStudent(studentId);
        Teacher teacher = requireTeacher(dto.getAssignedByTeacherId());
        LocalDateTime dueDate = validateOptionalFutureDueDate(dto.getDueDate());

        if (hasActiveAssignmentForStudent(activityId, studentId)) {
            throw new ApiException("Activity " + activityId + " is already assigned to student " + studentId);
        }

        ActivityAssignment assignment = new ActivityAssignment();
        assignment.setActivity(activity);
        assignment.setStudent(student);
        assignment.setClassroom(null);
        assignment.setAssignedByTeacher(teacher);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setDueDate(dueDate);
        assignment.setStatus(ActivityAssignmentStatus.ASSIGNED);

        return toOut(activityAssignmentRepository.save(assignment));
    }

    public ActivityAssignmentOutDTO assignToClassroom(Integer activityId, Integer classroomId, ActivityAssignInDTO dto) {
        Activity activity = requireApprovedActivity(activityId);
        Classroom classroom = requireClassroom(classroomId);
        Teacher teacher = requireTeacher(dto.getAssignedByTeacherId());
        LocalDateTime dueDate = validateOptionalFutureDueDate(dto.getDueDate());

        if (hasActiveAssignmentForClassroom(activityId, classroomId)) {
            throw new ApiException("Activity " + activityId + " is already assigned to classroom " + classroomId);
        }

        ActivityAssignment assignment = new ActivityAssignment();
        assignment.setActivity(activity);
        assignment.setClassroom(classroom);
        assignment.setStudent(null);
        assignment.setAssignedByTeacher(teacher);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setDueDate(dueDate);
        assignment.setStatus(ActivityAssignmentStatus.ASSIGNED);

        return toOut(activityAssignmentRepository.save(assignment));
    }

    public ApiResponse assignToBulkStudents(Integer activityId, ActivityAssignmentBulkInDTO dto) {
        Activity activity = requireApprovedActivity(activityId);
        Teacher teacher = requireTeacher(dto.getAssignedByTeacherId());
        if (dto.getStudentIds() == null || dto.getStudentIds().isEmpty()) {
            throw new ApiException("studentIds must not be empty");
        }
        LocalDateTime dueDate = validateOptionalFutureDueDate(dto.getDueDate());

        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (Integer studentId : dto.getStudentIds()) {
            Student student = studentRepository.findStudentById(studentId);
            if (student == null) {
                failed++;
                continue;
            }
            if (hasActiveAssignmentForStudent(activityId, studentId)) {
                skipped++;
                continue;
            }
            ActivityAssignment assignment = new ActivityAssignment();
            assignment.setActivity(activity);
            assignment.setStudent(student);
            assignment.setClassroom(null);
            assignment.setAssignedByTeacher(teacher);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment.setDueDate(dueDate);
            assignment.setStatus(ActivityAssignmentStatus.ASSIGNED);
            activityAssignmentRepository.save(assignment);
            created++;
        }

        return new ApiResponse("Bulk assignment completed. Created: " + created + ", Skipped: " + skipped + ", Failed: " + failed);
    }

    public List<ActivityAssignmentOutDTO> getAssignmentsByActivity(Integer activityId) {
        if (activityRepository.findActivityById(activityId) == null) {
            throw new ApiException("Activity with id " + activityId + " not found");
        }
        return activityAssignmentRepository.findActivityAssignmentsByActivityId(activityId)
                .stream()
                .map(this::toOut)
                .toList();
    }

    public List<ActivityAssignmentOutDTO> getAssignmentsByStudent(Integer studentId) {
        Student student = requireStudent(studentId);

        // Direct assignments + assignments targeting the student's classroom (deduplicated by id).
        Map<Integer, ActivityAssignment> byId = new LinkedHashMap<>();
        for (ActivityAssignment a : activityAssignmentRepository.findActivityAssignmentsByStudentId(studentId)) {
            byId.put(a.getId(), a);
        }
        if (student.getClassroom() != null) {
            for (ActivityAssignment a : activityAssignmentRepository.findActivityAssignmentsByClassroomId(student.getClassroom().getId())) {
                byId.put(a.getId(), a);
            }
        }
        return byId.values().stream().map(this::toOut).toList();
    }

    public void cancelAssignment(Integer assignmentId) {
        ActivityAssignment assignment = activityAssignmentRepository.findActivityAssignmentById(assignmentId);
        if (assignment == null) {
            throw new ApiException("ActivityAssignment with id " + assignmentId + " not found");
        }
        if (assignment.getStatus() == ActivityAssignmentStatus.CANCELLED) {
            throw new ApiException("ActivityAssignment is already cancelled");
        }
        boolean hasSubmittedWork = activitySubmissionRepository.findActivitySubmissionsByActivityAssignmentId(assignmentId)
                .stream()
                .anyMatch(s -> s.getStatus() == ActivitySubmissionStatus.SUBMITTED
                        || s.getStatus() == ActivitySubmissionStatus.GRADED);
        if (hasSubmittedWork) {
            throw new ApiException("Cannot cancel: there are already submitted or graded submissions for this assignment");
        }
        assignment.setStatus(ActivityAssignmentStatus.CANCELLED);
        activityAssignmentRepository.save(assignment);
    }

    public void extendDeadline(Integer assignmentId, ActivityAssignmentDeadlineInDTO dto) {
        ActivityAssignment assignment = activityAssignmentRepository.findActivityAssignmentById(assignmentId);
        if (assignment == null) {
            throw new ApiException("ActivityAssignment with id " + assignmentId + " not found");
        }
        if (assignment.getStatus() == ActivityAssignmentStatus.CANCELLED) {
            throw new ApiException("Cannot extend the deadline of a cancelled assignment");
        }
        LocalDateTime newDueDate = dto.getDueDate();
        if (newDueDate == null || !newDueDate.isAfter(LocalDateTime.now())) {
            throw new ApiException("New dueDate is required and must be in the future");
        }
        if (assignment.getDueDate() != null && !newDueDate.isAfter(assignment.getDueDate())) {
            throw new ApiException("New dueDate must be after the current dueDate");
        }
        assignment.setDueDate(newDueDate);
        activityAssignmentRepository.save(assignment);
    }

    // ====================== helpers ======================

    private Activity requireApprovedActivity(Integer activityId) {
        Activity activity = activityRepository.findActivityById(activityId);
        if (activity == null) {
            throw new ApiException("Activity with id " + activityId + " not found");
        }
        if (activity.getStatus() != ActivityStatus.APPROVED) {
            throw new ApiException("Activity must be APPROVED before it can be assigned");
        }
        return activity;
    }

    private Student requireStudent(Integer studentId) {
        Student student = studentRepository.findStudentById(studentId);
        if (student == null) {
            throw new ApiException("Student with id " + studentId + " not found");
        }
        return student;
    }

    private Classroom requireClassroom(Integer classroomId) {
        Classroom classroom = classroomRepository.findClassroomById(classroomId);
        if (classroom == null) {
            throw new ApiException("Classroom with id " + classroomId + " not found");
        }
        return classroom;
    }

    private Teacher requireTeacher(Integer teacherId) {
        Teacher teacher = teacherRepository.findTeacherById(teacherId);
        if (teacher == null) {
            throw new ApiException("Teacher with id " + teacherId + " not found");
        }
        return teacher;
    }

    private LocalDateTime validateOptionalFutureDueDate(LocalDateTime dueDate) {
        if (dueDate != null && !dueDate.isAfter(LocalDateTime.now())) {
            throw new ApiException("dueDate must be in the future");
        }
        return dueDate;
    }

    private boolean hasActiveAssignmentForStudent(Integer activityId, Integer studentId) {
        return activityAssignmentRepository.findActivityAssignmentsByActivityIdAndStudentId(activityId, studentId)
                .stream()
                .anyMatch(a -> a.getStatus() != ActivityAssignmentStatus.CANCELLED);
    }

    private boolean hasActiveAssignmentForClassroom(Integer activityId, Integer classroomId) {
        return activityAssignmentRepository.findActivityAssignmentsByActivityIdAndClassroomId(activityId, classroomId)
                .stream()
                .anyMatch(a -> a.getStatus() != ActivityAssignmentStatus.CANCELLED);
    }

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(ActivityAssignment activityAssignment, ActivityAssignmentInDTO dto) {
        Activity activity = activityRepository.findActivityById(dto.getActivityId());
        if (activity == null) {
            throw new ApiException("Activity with id " + dto.getActivityId() + " not found");
        }
        activityAssignment.setActivity(activity);

        Teacher teacher = teacherRepository.findTeacherById(dto.getAssignedByTeacherId());
        if (teacher == null) {
            throw new ApiException("Teacher with id " + dto.getAssignedByTeacherId() + " not found");
        }
        activityAssignment.setAssignedByTeacher(teacher);

        if (dto.getStudentId() != null) {
            Student student = studentRepository.findStudentById(dto.getStudentId());
            if (student == null) {
                throw new ApiException("Student with id " + dto.getStudentId() + " not found");
            }
            activityAssignment.setStudent(student);
        } else {
            activityAssignment.setStudent(null);
        }

        if (dto.getClassroomId() != null) {
            Classroom classroom = classroomRepository.findClassroomById(dto.getClassroomId());
            if (classroom == null) {
                throw new ApiException("Classroom with id " + dto.getClassroomId() + " not found");
            }
            activityAssignment.setClassroom(classroom);
        } else {
            activityAssignment.setClassroom(null);
        }
    }

    private ActivityAssignmentOutDTO toOut(ActivityAssignment e) {
        ActivityAssignmentOutDTO out = modelMapper.map(e, ActivityAssignmentOutDTO.class);
        if (e.getActivity() != null) {
            out.setActivityId(e.getActivity().getId());
            out.setActivityTitle(e.getActivity().getTitle());
        }
        if (e.getStudent() != null) {
            out.setStudentId(e.getStudent().getId());
            out.setStudentName(e.getStudent().getFullName());
        }
        if (e.getClassroom() != null) {
            out.setClassroomId(e.getClassroom().getId());
            out.setClassroomName(e.getClassroom().getName());
        }
        if (e.getAssignedByTeacher() != null) {
            out.setAssignedByTeacherId(e.getAssignedByTeacher().getId());
            out.setAssignedByTeacherName(e.getAssignedByTeacher().getFullName());
        }
        return out;
    }
}
