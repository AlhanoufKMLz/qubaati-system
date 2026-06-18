package com.example.qubaatisystem;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.Out.StudentOutDTO;
import com.example.qubaatisystem.DTO.Out.StudentProgressOutDTO;
import com.example.qubaatisystem.Model.Classroom;
import com.example.qubaatisystem.Repository.ClassroomRepository;
import com.example.qubaatisystem.Repository.TeacherRepository;
import com.example.qubaatisystem.Service.ClassroomService;
import com.example.qubaatisystem.Service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {

    @Mock ClassroomRepository classroomRepository;
    @Mock TeacherRepository teacherRepository;
    @Mock StudentService studentService;
    @Mock ModelMapper modelMapper;

    @InjectMocks ClassroomService classroomService;

    // ── enrollStudent ─────────────────────────────────────────────────────────

    @Test
    void enrollStudent_throwsApiException_whenClassroomNotFound() {
        when(classroomRepository.findClassroomById(99)).thenReturn(null);

        assertThatThrownBy(() -> classroomService.enrollStudent(99, 5))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void enrollStudent_delegatesEnrollmentToStudentService() {
        Classroom classroom = new Classroom();
        classroom.setId(2);
        when(classroomRepository.findClassroomById(2)).thenReturn(classroom);

        classroomService.enrollStudent(2, 5);

        verify(studentService).enrollInClassroom(5, 2);
    }

    // ── removeStudent ─────────────────────────────────────────────────────────

    @Test
    void removeStudent_throwsApiException_whenClassroomNotFound() {
        when(classroomRepository.findClassroomById(99)).thenReturn(null);

        assertThatThrownBy(() -> classroomService.removeStudent(99, 5))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void removeStudent_delegatesRemovalToStudentService() {
        Classroom classroom = new Classroom();
        classroom.setId(2);
        when(classroomRepository.findClassroomById(2)).thenReturn(classroom);

        classroomService.removeStudent(2, 5);

        verify(studentService).removeFromClassroom(5, 2);
    }

    // ── getProgress ───────────────────────────────────────────────────────────

    @Test
    void getProgress_mapsStudentFieldsIntoProgressDTO() {
        Classroom classroom = new Classroom();
        classroom.setId(2);
        when(classroomRepository.findClassroomById(2)).thenReturn(classroom);

        StudentOutDTO student = new StudentOutDTO();
        student.setId(7);
        student.setFullName("Test Student");
        student.setTotalPoints(50);
        student.setCompletedMissionsCount(3);
        when(studentService.getByClassroomId(2)).thenReturn(List.of(student));

        List<StudentProgressOutDTO> progress = classroomService.getProgress(2);

        assertThat(progress).hasSize(1);
        assertThat(progress.get(0).getStudentId()).isEqualTo(7);
        assertThat(progress.get(0).getTotalPoints()).isEqualTo(50);
        assertThat(progress.get(0).getCompletedMissionsCount()).isEqualTo(3);
    }
}
