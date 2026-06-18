package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.ClassroomInDTO;
import com.example.qubaatisystem.DTO.Out.ClassroomDashboardOutDTO;
import com.example.qubaatisystem.DTO.Out.ClassroomOutDTO;
import com.example.qubaatisystem.DTO.Out.StudentOutDTO;
import com.example.qubaatisystem.DTO.Out.StudentProgressOutDTO;
import com.example.qubaatisystem.Model.Classroom;
import com.example.qubaatisystem.Model.Teacher;
import com.example.qubaatisystem.Repository.ClassroomRepository;
import com.example.qubaatisystem.Repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final TeacherRepository teacherRepository;
    private final StudentService studentService;
    private final ModelMapper modelMapper;

    public List<ClassroomOutDTO> getAll() {
        return classroomRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public ClassroomOutDTO getById(Integer id) {
        Classroom classroom = classroomRepository.findClassroomById(id);
        if (classroom == null) {
            throw new ApiException("Classroom with id " + id + " not found");
        }
        return toOut(classroom);
    }

    public void create(ClassroomInDTO classroomInDTO) {
        // Set scalar fields directly — ModelMapper maps teacherId to Classroom.id via token matching,
        // causing JPA to treat the new entity as an existing row and throw an optimistic lock error.
        Classroom classroom = new Classroom();
        classroom.setName(classroomInDTO.getName());
        classroom.setGradeLevel(classroomInDTO.getGradeLevel());
        classroom.setSection(classroomInDTO.getSection());
        applyRelationships(classroom, classroomInDTO);

        classroom.setId(null);
        classroomRepository.save(classroom);
    }

    public void update(Integer id, ClassroomInDTO classroomInDTO) {
        Classroom classroom = classroomRepository.findClassroomById(id);
        if (classroom == null) {
            throw new ApiException("Classroom with id " + id + " not found");
        }

        // Clear relationships first so ModelMapper only copies scalar fields
        // (never mutates the ids of the currently-managed related entities).
        classroom.setTeacher(null);
        modelMapper.map(classroomInDTO, classroom);
        classroom.setId(id);

        // Set scalar fields directly — same reason as create() (ModelMapper ambiguity on teacherId).
        classroom.setName(classroomInDTO.getName());
        classroom.setGradeLevel(classroomInDTO.getGradeLevel());
        classroom.setSection(classroomInDTO.getSection());
        applyRelationships(classroom, classroomInDTO);
        classroomRepository.save(classroom);
    }

    public void delete(Integer id) {
        Classroom classroom = classroomRepository.findClassroomById(id);
        if (classroom == null) {
            throw new ApiException("Classroom with id " + id + " not found");
        }
        classroomRepository.delete(classroom);
    }

    // ========== Student management ==========

    public void enrollStudent(Integer classroomId, Integer studentId) {
        if (classroomRepository.findClassroomById(classroomId) == null) {
            throw new ApiException("Classroom with id " + classroomId + " not found");
        }
        studentService.enrollInClassroom(studentId, classroomId);
    }

    public void removeStudent(Integer classroomId, Integer studentId) {
        if (classroomRepository.findClassroomById(classroomId) == null) {
            throw new ApiException("Classroom with id " + classroomId + " not found");
        }
        studentService.removeFromClassroom(studentId, classroomId);
    }

    public List<StudentOutDTO> getStudents(Integer classroomId) {
        if (classroomRepository.findClassroomById(classroomId) == null) {
            throw new ApiException("Classroom with id " + classroomId + " not found");
        }
        return studentService.getByClassroomId(classroomId);
    }

    public ClassroomDashboardOutDTO getDashboard(Integer classroomId) {
        Classroom classroom = classroomRepository.findClassroomById(classroomId);
        if (classroom == null) {
            throw new ApiException("Classroom with id " + classroomId + " not found");
        }
        ClassroomDashboardOutDTO out = new ClassroomDashboardOutDTO();
        out.setClassroomId(classroom.getId());
        out.setName(classroom.getName());
        out.setGradeLevel(classroom.getGradeLevel());
        out.setSection(classroom.getSection());
        if (classroom.getTeacher() != null) {
            out.setTeacherId(classroom.getTeacher().getId());
            out.setTeacherName(classroom.getTeacher().getFullName());
        }
        out.setStudentCount(studentService.getByClassroomId(classroomId).size());
        return out;
    }

    public List<StudentProgressOutDTO> getProgress(Integer classroomId) {
        if (classroomRepository.findClassroomById(classroomId) == null) {
            throw new ApiException("Classroom with id " + classroomId + " not found");
        }
        return studentService.getByClassroomId(classroomId)
                .stream()
                .map(s -> new StudentProgressOutDTO(s.getId(), s.getFullName(), s.getTotalPoints(), s.getCompletedMissionsCount()))
                .toList();
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(Classroom classroom, ClassroomInDTO dto) {
        Teacher teacher = teacherRepository.findTeacherById(dto.getTeacherId());
        if (teacher == null) {
            throw new ApiException("Teacher with id " + dto.getTeacherId() + " not found");
        }
        classroom.setTeacher(teacher);
    }

    private ClassroomOutDTO toOut(Classroom classroom) {
        ClassroomOutDTO out = modelMapper.map(classroom, ClassroomOutDTO.class);
        // Derived display field ModelMapper cannot infer from field names (teacherName <- teacher.fullName).
        if (classroom.getTeacher() != null) {
            out.setTeacherName(classroom.getTeacher().getFullName());
        }
        return out;
    }
}
