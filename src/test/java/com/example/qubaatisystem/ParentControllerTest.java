package com.example.qubaatisystem;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.Controller.ParentController;
import com.example.qubaatisystem.DTO.In.ChildCreateInDTO;
import com.example.qubaatisystem.DTO.In.ChildUpdateProfileInDTO;
import com.example.qubaatisystem.DTO.Out.StudentOutDTO;
import com.example.qubaatisystem.Security.SecurityOwnershipService;
import com.example.qubaatisystem.Service.ParentService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParentController.class)
class ParentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ParentService parentService;
    @MockitoBean SecurityOwnershipService security;

    @Test
    void createChild_returns200WithCreatedStudentDTO() throws Exception {
        StudentOutDTO response = new StudentOutDTO();
        response.setId(1);
        response.setFullName("Child Name");
        response.setParentId(1);
        when(parentService.createChild(eq(1), any(ChildCreateInDTO.class))).thenReturn(response);

        ChildCreateInDTO dto = new ChildCreateInDTO(
                "student_user", "student@test.com", "pass123", "Child Name", 10, "Grade 5", null);

        mockMvc.perform(post("/api/v1/parents/1/children")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Child Name"))
                .andExpect(jsonPath("$.parentId").value(1));
    }

    @Test
    void createChild_returns400_whenParentNotFound() throws Exception {
        when(parentService.createChild(eq(99), any(ChildCreateInDTO.class)))
                .thenThrow(new ApiException("Parent with id 99 not found"));

        ChildCreateInDTO dto = new ChildCreateInDTO(
                "student_user", "student@test.com", "pass123", "Child Name", 10, "Grade 5", null);

        mockMvc.perform(post("/api/v1/parents/99/children")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parent with id 99 not found"));
    }

    @Test
    void getChildren_returns200WithChildList() throws Exception {
        StudentOutDTO c1 = new StudentOutDTO();
        c1.setFullName("Child One");
        StudentOutDTO c2 = new StudentOutDTO();
        c2.setFullName("Child Two");
        when(parentService.getChildren(1)).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/v1/parents/1/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getChildOverview_returns200WithStudentDTO() throws Exception {
        StudentOutDTO overview = new StudentOutDTO();
        overview.setId(5);
        overview.setFullName("Child Name");
        overview.setParentId(1);
        when(parentService.getChildOverview(1, 5)).thenReturn(overview);

        mockMvc.perform(get("/api/v1/parents/1/children/5/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Child Name"))
                .andExpect(jsonPath("$.parentId").value(1));
    }

    @Test
    void updateChildProfile_returns200WithUpdatedStudentDTO() throws Exception {
        StudentOutDTO updated = new StudentOutDTO();
        updated.setFullName("Updated Name");
        updated.setAge(12);
        when(parentService.updateChildProfile(eq(1), eq(5), any(ChildUpdateProfileInDTO.class)))
                .thenReturn(updated);

        ChildUpdateProfileInDTO dto = new ChildUpdateProfileInDTO("Updated Name", 12, "Grade 6");

        mockMvc.perform(patch("/api/v1/parents/1/children/5/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.age").value(12));
    }
}
