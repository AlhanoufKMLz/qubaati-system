package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.TeacherInDTO;
import com.example.qubaatisystem.DTO.Out.TeacherOutDTO;
import com.example.qubaatisystem.Enum.UserRole;
import com.example.qubaatisystem.Model.Teacher;
import com.example.qubaatisystem.Model.User;
import com.example.qubaatisystem.Repository.TeacherRepository;
import com.example.qubaatisystem.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public List<TeacherOutDTO> getAll() {
        return teacherRepository.findAll()
                .stream()
                .map(this::mapTeacherToOutDTO)
                .toList();
    }

    public TeacherOutDTO getById(Integer id) {
        Teacher teacher = teacherRepository.findTeacherById(id);
        if (teacher == null) {
            throw new ApiException("Teacher with id " + id + " not found");
        }
        return mapTeacherToOutDTO(teacher);
    }

    @Transactional
    public TeacherOutDTO create(TeacherInDTO teacherInDTO) {
        // Create the linked User account (the role is assigned internally, never from the DTO).
        User user = new User();
        user.setUsername(teacherInDTO.getUsername());
        user.setEmail(teacherInDTO.getEmail());
        user.setPassword(teacherInDTO.getPassword());
        user.setRole(UserRole.TEACHER);
        User savedUser = userRepository.save(user);

        // Create the Teacher profile (ModelMapper copies scalar fields only) and link the saved User.
        Teacher teacher = modelMapper.map(teacherInDTO, Teacher.class);
        teacher.setUser(savedUser);

        teacher.setId(null);
        Teacher savedTeacher = teacherRepository.save(teacher);
        return mapTeacherToOutDTO(savedTeacher);
    }

    @Transactional
    public TeacherOutDTO update(Integer id, TeacherInDTO teacherInDTO) {
        Teacher teacher = teacherRepository.findTeacherById(id);
        if (teacher == null) {
            throw new ApiException("Teacher with id " + id + " not found");
        }

        // Update Teacher profile fields (ModelMapper copies scalar fields only).
        modelMapper.map(teacherInDTO, teacher);
        teacher.setId(id);

        // Update the linked User account fields, keeping the role as TEACHER.
        User user = teacher.getUser();
        user.setUsername(teacherInDTO.getUsername());
        user.setEmail(teacherInDTO.getEmail());
        user.setPassword(teacherInDTO.getPassword());
        user.setRole(UserRole.TEACHER);
        userRepository.save(user);

        Teacher savedTeacher = teacherRepository.save(teacher);
        return mapTeacherToOutDTO(savedTeacher);
    }

    public void delete(Integer id) {
        Teacher teacher = teacherRepository.findTeacherById(id);
        if (teacher == null) {
            throw new ApiException("Teacher with id " + id + " not found");
        }
        teacherRepository.delete(teacher);
    }

    // ---------- helpers ----------

    private TeacherOutDTO mapTeacherToOutDTO(Teacher teacher) {
        TeacherOutDTO dto = modelMapper.map(teacher, TeacherOutDTO.class);

        if (teacher.getUser() != null) {
            dto.setUserId(teacher.getUser().getId());
            dto.setUsername(teacher.getUser().getUsername());
            dto.setEmail(teacher.getUser().getEmail());
            dto.setRole(teacher.getUser().getRole());
        }

        return dto;
    }
}
