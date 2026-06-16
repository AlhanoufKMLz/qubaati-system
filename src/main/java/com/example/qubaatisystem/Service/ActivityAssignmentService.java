package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.ActivityAssignmentInDTO;
import com.example.qubaatisystem.DTO.Out.ActivityAssignmentOutDTO;
import com.example.qubaatisystem.Model.Activity;
import com.example.qubaatisystem.Model.ActivityAssignment;
import com.example.qubaatisystem.Model.Classroom;
import com.example.qubaatisystem.Model.Student;
import com.example.qubaatisystem.Model.Teacher;
import com.example.qubaatisystem.Repository.ActivityAssignmentRepository;
import com.example.qubaatisystem.Repository.ActivityRepository;
import com.example.qubaatisystem.Repository.ClassroomRepository;
import com.example.qubaatisystem.Repository.StudentRepository;
import com.example.qubaatisystem.Repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityAssignmentService {

    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final ActivityRepository activityRepository;
    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final TeacherRepository teacherRepository;
    private final ModelMapper modelMapper;

    public List<ActivityAssignmentOutDTO> getAll() {
        return activityAssignmentRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public ActivityAssignmentOutDTO getById(Integer id) {
        List<ActivityAssignment> activityAssignments = activityAssignmentRepository.findActivityAssignmentById(id);
        if (activityAssignments.isEmpty()) {
            throw new ApiException("ActivityAssignment with id " + id + " not found");
        }
        return toOut(activityAssignments.get(0));
    }

    public void create(ActivityAssignmentInDTO dto) {
        ActivityAssignment activityAssignment = modelMapper.map(dto, ActivityAssignment.class);

        applyRelationships(activityAssignment, dto);

        activityAssignmentRepository.save(activityAssignment);
    }

    public void update(Integer id, ActivityAssignmentInDTO dto) {
        List<ActivityAssignment> activityAssignments = activityAssignmentRepository.findActivityAssignmentById(id);
        if (activityAssignments.isEmpty()) {
            throw new ApiException("ActivityAssignment with id " + id + " not found");
        }
        ActivityAssignment activityAssignment = activityAssignments.get(0);

        // Clear relationships first so ModelMapper only copies scalar fields
        // (never mutates the ids of the currently-managed related entities).
        activityAssignment.setActivity(null);
        activityAssignment.setStudent(null);
        activityAssignment.setClassroom(null);
        activityAssignment.setAssignedByTeacher(null);
        modelMapper.map(dto, activityAssignment);

        applyRelationships(activityAssignment, dto);

        activityAssignmentRepository.save(activityAssignment);
    }

    public void delete(Integer id) {
        List<ActivityAssignment> activityAssignments = activityAssignmentRepository.findActivityAssignmentById(id);
        if (activityAssignments.isEmpty()) {
            throw new ApiException("ActivityAssignment with id " + id + " not found");
        }
        activityAssignmentRepository.delete(activityAssignments.get(0));
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(ActivityAssignment activityAssignment, ActivityAssignmentInDTO dto) {
        List<Activity> activities = activityRepository.findActivityById(dto.getActivityId());
        if (activities.isEmpty()) {
            throw new ApiException("Activity with id " + dto.getActivityId() + " not found");
        }
        activityAssignment.setActivity(activities.get(0));

        List<Teacher> teachers = teacherRepository.findTeacherById(dto.getAssignedByTeacherId());
        if (teachers.isEmpty()) {
            throw new ApiException("Teacher with id " + dto.getAssignedByTeacherId() + " not found");
        }
        activityAssignment.setAssignedByTeacher(teachers.get(0));

        if (dto.getStudentId() != null) {
            List<Student> students = studentRepository.findStudentById(dto.getStudentId());
            if (students.isEmpty()) {
                throw new ApiException("Student with id " + dto.getStudentId() + " not found");
            }
            activityAssignment.setStudent(students.get(0));
        } else {
            activityAssignment.setStudent(null);
        }

        if (dto.getClassroomId() != null) {
            List<Classroom> classrooms = classroomRepository.findClassroomById(dto.getClassroomId());
            if (classrooms.isEmpty()) {
                throw new ApiException("Classroom with id " + dto.getClassroomId() + " not found");
            }
            activityAssignment.setClassroom(classrooms.get(0));
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
