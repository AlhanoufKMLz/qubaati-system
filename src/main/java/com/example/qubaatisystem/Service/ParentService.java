package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.ParentInDTO;
import com.example.qubaatisystem.DTO.Out.ParentOutDTO;
import com.example.qubaatisystem.Enum.UserRole;
import com.example.qubaatisystem.Model.Parent;
import com.example.qubaatisystem.Model.User;
import com.example.qubaatisystem.Repository.ParentRepository;
import com.example.qubaatisystem.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public List<ParentOutDTO> getAll() {
        return parentRepository.findAll()
                .stream()
                .map(this::mapParentToOutDTO)
                .toList();
    }

    public ParentOutDTO getById(Integer id) {
        Parent parent = parentRepository.findParentById(id);
        if (parent == null) {
            throw new ApiException("Parent with id " + id + " not found");
        }
        return mapParentToOutDTO(parent);
    }

    @Transactional
    public ParentOutDTO create(ParentInDTO parentInDTO) {
        // Create the linked User account (the role is assigned internally, never from the DTO).
        User user = new User();
        user.setUsername(parentInDTO.getUsername());
        user.setEmail(parentInDTO.getEmail());
        user.setPassword(parentInDTO.getPassword());
        user.setRole(UserRole.PARENT);
        User savedUser = userRepository.save(user);

        // Create the Parent profile (ModelMapper copies scalar fields only) and link the saved User.
        Parent parent = modelMapper.map(parentInDTO, Parent.class);
        parent.setUser(savedUser);

        parent.setId(null);
        Parent savedParent = parentRepository.save(parent);
        return mapParentToOutDTO(savedParent);
    }

    @Transactional
    public ParentOutDTO update(Integer id, ParentInDTO parentInDTO) {
        Parent parent = parentRepository.findParentById(id);
        if (parent == null) {
            throw new ApiException("Parent with id " + id + " not found");
        }

        // Update Parent profile fields (ModelMapper copies scalar fields only).
        modelMapper.map(parentInDTO, parent);
        parent.setId(id);

        // Update the linked User account fields, keeping the role as PARENT.
        User user = parent.getUser();
        user.setUsername(parentInDTO.getUsername());
        user.setEmail(parentInDTO.getEmail());
        user.setPassword(parentInDTO.getPassword());
        user.setRole(UserRole.PARENT);
        userRepository.save(user);

        Parent savedParent = parentRepository.save(parent);
        return mapParentToOutDTO(savedParent);
    }

    public void delete(Integer id) {
        Parent parent = parentRepository.findParentById(id);
        if (parent == null) {
            throw new ApiException("Parent with id " + id + " not found");
        }
        // If the parent still has children, the FK constraint will reject this deletion (no cascade, no orphaning).
        parentRepository.delete(parent);
    }

    // ---------- helpers ----------

    private ParentOutDTO mapParentToOutDTO(Parent parent) {
        ParentOutDTO dto = modelMapper.map(parent, ParentOutDTO.class);

        if (parent.getUser() != null) {
            dto.setUserId(parent.getUser().getId());
            dto.setUsername(parent.getUser().getUsername());
            dto.setEmail(parent.getUser().getEmail());
            dto.setRole(parent.getUser().getRole());
        }

        return dto;
    }
}
