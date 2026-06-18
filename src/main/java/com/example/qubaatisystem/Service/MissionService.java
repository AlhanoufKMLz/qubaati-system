package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.MissionInDTO;
import com.example.qubaatisystem.DTO.Out.MissionOutDTO;
import com.example.qubaatisystem.Model.CareerWorld;
import com.example.qubaatisystem.Model.Mission;
import com.example.qubaatisystem.Repository.CareerWorldRepository;
import com.example.qubaatisystem.Repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final CareerWorldRepository careerWorldRepository;
    private final ModelMapper modelMapper;

    public List<MissionOutDTO> getAll() {
        return missionRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public MissionOutDTO getById(Integer id) {
        Mission mission = missionRepository.findMissionById(id);
        if (mission == null) {
            throw new ApiException("Mission with id " + id + " not found");
        }
        return toOut(mission);
    }

    public void create(MissionInDTO dto) {
        Mission mission = modelMapper.map(dto, Mission.class);

        applyRelationships(mission, dto);

        mission.setId(null);
        missionRepository.save(mission);
    }

    public void update(Integer id, MissionInDTO dto) {
        Mission mission = missionRepository.findMissionById(id);
        if (mission == null) {
            throw new ApiException("Mission with id " + id + " not found");
        }

        // Clear relationships first so ModelMapper only copies scalar fields
        // (never mutates the ids of the currently-managed related entities).
        mission.setCareerWorld(null);
        modelMapper.map(dto, mission);
        mission.setId(id);

        applyRelationships(mission, dto);

        missionRepository.save(mission);
    }

    public void delete(Integer id) {
        Mission mission = missionRepository.findMissionById(id);
        if (mission == null) {
            throw new ApiException("Mission with id " + id + " not found");
        }
        missionRepository.delete(mission);
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(Mission mission, MissionInDTO dto) {
        CareerWorld careerWorld = careerWorldRepository.findCareerWorldById(dto.getCareerWorldId());
        if (careerWorld == null) {
            throw new ApiException("CareerWorld with id " + dto.getCareerWorldId() + " not found");
        }
        mission.setCareerWorld(careerWorld);
    }

    private MissionOutDTO toOut(Mission mission) {
        MissionOutDTO out = modelMapper.map(mission, MissionOutDTO.class);
        // All OutDTO fields (including careerWorldId <- careerWorld.id and
        // careerWorldTitle <- careerWorld.title) are auto-mapped by ModelMapper.
        return out;
    }
}
