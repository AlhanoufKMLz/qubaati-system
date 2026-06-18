package com.example.qubaatisystem;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.Controller.ClassroomController;
import com.example.qubaatisystem.DTO.Out.ClassroomDashboardOutDTO;
import com.example.qubaatisystem.DTO.Out.StudentOutDTO;
import com.example.qubaatisystem.Service.ClassroomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClassroomController.class)
class ClassroomControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ClassroomService classroomService;

    @Test
    void enrollStudent_returns200WithSuccessMessage() throws Exception {
        // enrollStudent is void; default mock does nothing
        mockMvc.perform(post("/api/v1/classrooms/2/students/5/enroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student enrolled in classroom successfully"));
    }

    @Test
    void enrollStudent_returns400_whenClassroomNotFound() throws Exception {
        doThrow(new ApiException("Classroom with id 99 not found"))
                .when(classroomService).enrollStudent(99, 5);

        mockMvc.perform(post("/api/v1/classrooms/99/students/5/enroll"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Classroom with id 99 not found"));
    }

    @Test
    void getStudents_returns200WithStudentList() throws Exception {
        StudentOutDTO student = new StudentOutDTO();
        student.setFullName("Test Student");
        when(classroomService.getStudents(2)).thenReturn(List.of(student));

        mockMvc.perform(get("/api/v1/classrooms/2/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Test Student"));
    }

    @Test
    void removeStudent_returns200WithSuccessMessage() throws Exception {
        // removeStudent is void; default mock does nothing
        mockMvc.perform(delete("/api/v1/classrooms/2/students/5/remove"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student removed from classroom successfully"));
    }

    @Test
    void getDashboard_returns200WithDashboardDTO() throws Exception {
        ClassroomDashboardOutDTO dashboard = new ClassroomDashboardOutDTO();
        dashboard.setClassroomId(2);
        dashboard.setName("Class A");
        dashboard.setStudentCount(3);
        when(classroomService.getDashboard(2)).thenReturn(dashboard);

        mockMvc.perform(get("/api/v1/classrooms/2/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classroomId").value(2))
                .andExpect(jsonPath("$.name").value("Class A"))
                .andExpect(jsonPath("$.studentCount").value(3));
    }
}
