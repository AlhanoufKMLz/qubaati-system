package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.StudentInDTO;
import com.example.qubaatisystem.DTO.Out.CareerWorldOutDTO;
import com.example.qubaatisystem.DTO.Out.LearningStyleHistoryOutDTO;
import com.example.qubaatisystem.DTO.Out.SkillProgressHistoryOutDTO;
import com.example.qubaatisystem.DTO.Out.StudentOutDTO;
import com.example.qubaatisystem.Enum.UserRole;
import com.example.qubaatisystem.Model.Classroom;
import com.example.qubaatisystem.Model.LearningStyleHistory;
import com.example.qubaatisystem.Model.Parent;
import com.example.qubaatisystem.Model.SkillProgressHistory;
import com.example.qubaatisystem.Model.Student;
import com.example.qubaatisystem.Model.User;
import com.example.qubaatisystem.Repository.CareerWorldRepository;
import com.example.qubaatisystem.Repository.ClassroomRepository;
import com.example.qubaatisystem.Repository.LearningStyleHistoryRepository;
import com.example.qubaatisystem.Repository.ParentRepository;
import com.example.qubaatisystem.Repository.SkillProgressHistoryRepository;
import com.example.qubaatisystem.Repository.StudentRepository;
import com.example.qubaatisystem.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ParentRepository parentRepository;
    private final ClassroomRepository classroomRepository;
    private final CareerWorldRepository careerWorldRepository;
    private final SkillProgressHistoryRepository skillProgressHistoryRepository;
    private final LearningStyleHistoryRepository learningStyleHistoryRepository;
    private final ModelMapper modelMapper;

    public List<StudentOutDTO> getAll() {
        return studentRepository.findAll()
                .stream()
                .map(this::mapStudentToOutDTO)
                .toList();
    }

    public StudentOutDTO getById(Integer id) {
        Student student = studentRepository.findStudentById(id);
        if (student == null) {
            throw new ApiException("Student with id " + id + " not found");
        }
        return mapStudentToOutDTO(student);
    }

    @Transactional
    public StudentOutDTO create(StudentInDTO studentInDTO) {
        // Create the linked child User account. The role is assigned internally; the parent creates
        // this account for the child (the student does not create it himself).
        User user = new User();
        user.setUsername(studentInDTO.getUsername());
        user.setEmail(studentInDTO.getEmail());
        user.setPassword(studentInDTO.getPassword());
        user.setRole(UserRole.STUDENT);
        User savedUser = userRepository.save(user);

        // Build the Student profile with manual scalar mapping. ModelMapper is NOT used for the
        // InDTO -> Student copy because StudentInDTO carries two relation ids (parentId, classroomId):
        // ModelMapper's STANDARD matching treats both as candidates for Student.setId() and aborts with
        // "destination property setId matches multiple source property hierarchies". The relation ids are
        // resolved explicitly below instead.
        Student student = new Student();
        student.setFullName(studentInDTO.getFullName());
        student.setAge(studentInDTO.getAge());
        student.setGrade(studentInDTO.getGrade());
        student.setTotalPoints(studentInDTO.getTotalPoints());
        student.setCompletedMissionsCount(studentInDTO.getCompletedMissionsCount());
        applyDefaults(student);

        student.setUser(savedUser);
        student.setParent(resolveParent(studentInDTO.getParentId()));
        student.setClassroom(resolveClassroom(studentInDTO.getClassroomId()));

        student.setId(null);
        Student savedStudent = studentRepository.save(student);
        return mapStudentToOutDTO(savedStudent);
    }

    @Transactional
    public StudentOutDTO update(Integer id, StudentInDTO studentInDTO) {
        Student student = studentRepository.findStudentById(id);
        if (student == null) {
            throw new ApiException("Student with id " + id + " not found");
        }

        // Manual scalar mapping (no ModelMapper for the InDTO -> Student copy; see create() for why).
        student.setFullName(studentInDTO.getFullName());
        student.setAge(studentInDTO.getAge());
        student.setGrade(studentInDTO.getGrade());
        student.setTotalPoints(studentInDTO.getTotalPoints());
        student.setCompletedMissionsCount(studentInDTO.getCompletedMissionsCount());
        student.setId(id);
        applyDefaults(student);

        // Update the linked User account fields, keeping the role as STUDENT.
        User user = student.getUser();
        user.setUsername(studentInDTO.getUsername());
        user.setEmail(studentInDTO.getEmail());
        user.setPassword(studentInDTO.getPassword());
        user.setRole(UserRole.STUDENT);
        userRepository.save(user);

        student.setParent(resolveParent(studentInDTO.getParentId()));
        student.setClassroom(resolveClassroom(studentInDTO.getClassroomId()));

        Student savedStudent = studentRepository.save(student);
        return mapStudentToOutDTO(savedStudent);
    }

    public void delete(Integer id) {
        Student student = studentRepository.findStudentById(id);
        if (student == null) {
            throw new ApiException("Student with id " + id + " not found");
        }
        studentRepository.delete(student);
    }

    // ====================== AVAILABILITY / HISTORY ======================

    public List<CareerWorldOutDTO> getAvailableCareerWorlds(Integer studentId) {
        requireStudentExists(studentId);
        // No advanced availability rule yet: every CareerWorld is available to the student.
        return careerWorldRepository.findAll()
                .stream()
                .map(careerWorld -> modelMapper.map(careerWorld, CareerWorldOutDTO.class))
                .toList();
    }

    public List<SkillProgressHistoryOutDTO> getSkillHistory(Integer studentId) {
        requireStudentExists(studentId);
        return skillProgressHistoryRepository.findSkillProgressHistoriesByStudentId(studentId)
                .stream()
                .map(this::toSkillHistoryOut)
                .toList();
    }

    public List<LearningStyleHistoryOutDTO> getLearningStyleHistory(Integer studentId) {
        requireStudentExists(studentId);
        return learningStyleHistoryRepository.findLearningStyleHistoriesByStudentId(studentId)
                .stream()
                .map(this::toLearningStyleHistoryOut)
                .toList();
    }

    // ====================== helpers ======================

    private void requireStudentExists(Integer studentId) {
        if (studentRepository.findStudentById(studentId) == null) {
            throw new ApiException("Student with id " + studentId + " not found");
        }
    }

    private SkillProgressHistoryOutDTO toSkillHistoryOut(SkillProgressHistory history) {
        SkillProgressHistoryOutDTO out = modelMapper.map(history, SkillProgressHistoryOutDTO.class);
        if (history.getStudent() != null) {
            out.setStudentId(history.getStudent().getId());
            out.setStudentName(history.getStudent().getFullName());
        }
        if (history.getSkill() != null) {
            out.setSkillId(history.getSkill().getId());
            out.setSkillName(history.getSkill().getName());
        }
        if (history.getStudentSkill() != null) {
            out.setStudentSkillId(history.getStudentSkill().getId());
        }
        return out;
    }

    private LearningStyleHistoryOutDTO toLearningStyleHistoryOut(LearningStyleHistory history) {
        LearningStyleHistoryOutDTO out = modelMapper.map(history, LearningStyleHistoryOutDTO.class);
        if (history.getStudent() != null) {
            out.setStudentId(history.getStudent().getId());
            out.setStudentName(history.getStudent().getFullName());
        }
        if (history.getLearningStyle() != null) {
            out.setLearningStyleId(history.getLearningStyle().getId());
        }
        return out;
    }

    private Parent resolveParent(Integer parentId) {
        Parent parent = parentRepository.findParentById(parentId);
        if (parent == null) {
            throw new ApiException("Parent with id " + parentId + " not found");
        }
        return parent;
    }

    private Classroom resolveClassroom(Integer classroomId) {
        if (classroomId == null) {
            return null;
        }
        Classroom classroom = classroomRepository.findClassroomById(classroomId);
        if (classroom == null) {
            throw new ApiException("Classroom with id " + classroomId + " not found");
        }
        return classroom;
    }

    private void applyDefaults(Student student) {
        if (student.getTotalPoints() == null) {
            student.setTotalPoints(0);
        }
        if (student.getCompletedMissionsCount() == null) {
            student.setCompletedMissionsCount(0);
        }
    }

    private StudentOutDTO mapStudentToOutDTO(Student student) {
        StudentOutDTO dto = modelMapper.map(student, StudentOutDTO.class);

        if (student.getUser() != null) {
            dto.setUserId(student.getUser().getId());
            dto.setUsername(student.getUser().getUsername());
            dto.setEmail(student.getUser().getEmail());
            dto.setRole(student.getUser().getRole());
        }

        if (student.getClassroom() != null) {
            dto.setClassroomId(student.getClassroom().getId());
            dto.setClassroomName(student.getClassroom().getName());
        }

        if (student.getParent() != null) {
            dto.setParentId(student.getParent().getId());
            dto.setParentName(student.getParent().getFullName());
            dto.setParentPhoneNumber(student.getParent().getPhoneNumber());
            if (student.getParent().getUser() != null) {
                dto.setParentEmail(student.getParent().getUser().getEmail());
            }
        }

        return dto;
    }
}
