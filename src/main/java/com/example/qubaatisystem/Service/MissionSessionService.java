package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.MissionSessionInDTO;
import com.example.qubaatisystem.DTO.Out.MissionSessionOutDTO;
import com.example.qubaatisystem.Model.Mission;
import com.example.qubaatisystem.Model.MissionSession;
import com.example.qubaatisystem.Repository.MissionRepository;
import com.example.qubaatisystem.Repository.MissionSessionRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionSessionService {

    private final MissionSessionRepository missionSessionRepository;
    private final MissionRepository missionRepository;
    private final ModelMapper modelMapper;

    public List<MissionSessionOutDTO> getAll() {
        return missionSessionRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public MissionSessionOutDTO getById(Integer id) {
        MissionSession missionSession = missionSessionRepository.findMissionSessionById(id);
        if (missionSession == null) {
            throw new ApiException("MissionSession with id " + id + " not found");
        }
        return toOut(missionSession);
    }

    public void create(MissionSessionInDTO dto) {
        MissionSession missionSession = modelMapper.map(dto, MissionSession.class);

        applyRelationships(missionSession, dto);

        missionSession.setId(null);
        missionSessionRepository.save(missionSession);
    }

    public void update(Integer id, MissionSessionInDTO dto) {
        MissionSession missionSession = missionSessionRepository.findMissionSessionById(id);
        if (missionSession == null) {
            throw new ApiException("MissionSession with id " + id + " not found");
        }

        // Clear relationships first so ModelMapper only copies scalar fields
        // (never mutates the ids of the currently-managed related entities).
        missionSession.setMission(null);
        modelMapper.map(dto, missionSession);
        missionSession.setId(id);

        applyRelationships(missionSession, dto);

        missionSessionRepository.save(missionSession);
    }

    public void delete(Integer id) {
        MissionSession missionSession = missionSessionRepository.findMissionSessionById(id);
        if (missionSession == null) {
            throw new ApiException("MissionSession with id " + id + " not found");
        }
        missionSessionRepository.delete(missionSession);
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(MissionSession missionSession, MissionSessionInDTO dto) {
        Mission mission = missionRepository.findMissionById(dto.getMissionId());
        if (mission == null) {
            throw new ApiException("Mission with id " + dto.getMissionId() + " not found");
        }
        missionSession.setMission(mission);
    }

    private MissionSessionOutDTO toOut(MissionSession missionSession) {
        MissionSessionOutDTO out = modelMapper.map(missionSession, MissionSessionOutDTO.class);
        return out;
    }
}
