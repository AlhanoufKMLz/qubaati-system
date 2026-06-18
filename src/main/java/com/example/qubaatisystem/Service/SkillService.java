package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.SkillInDTO;
import com.example.qubaatisystem.DTO.Out.SkillOutDTO;
import com.example.qubaatisystem.Model.Skill;
import com.example.qubaatisystem.Repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final ModelMapper modelMapper;

    public List<SkillOutDTO> getAll() {
        return skillRepository.findAll().stream().map(this::toOut).toList();
    }

    public SkillOutDTO getById(Integer id) {
        Skill skill = skillRepository.findSkillById(id);
        if (skill == null) {
            throw new ApiException("Skill with id " + id + " not found");
        }
        return toOut(skill);
    }

    public void create(SkillInDTO dto) {
        Skill skill = modelMapper.map(dto, Skill.class);

        skill.setId(null);
        skillRepository.save(skill);
    }

    public void update(Integer id, SkillInDTO dto) {
        Skill skill = skillRepository.findSkillById(id);
        if (skill == null) {
            throw new ApiException("Skill with id " + id + " not found");
        }

        modelMapper.map(dto, skill);
        skill.setId(id);

        skillRepository.save(skill);
    }

    public void delete(Integer id) {
        Skill skill = skillRepository.findSkillById(id);
        if (skill == null) {
            throw new ApiException("Skill with id " + id + " not found");
        }
        skillRepository.delete(skill);
    }

    // ---------- helpers ----------

    private SkillOutDTO toOut(Skill skill) {
        return modelMapper.map(skill, SkillOutDTO.class);
    }
}
