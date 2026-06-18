package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.DecisionInDTO;
import com.example.qubaatisystem.DTO.Out.DecisionOutDTO;
import com.example.qubaatisystem.Model.Decision;
import com.example.qubaatisystem.Model.MissionSession;
import com.example.qubaatisystem.Repository.DecisionRepository;
import com.example.qubaatisystem.Repository.MissionSessionRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DecisionService {

    private final DecisionRepository decisionRepository;
    private final MissionSessionRepository missionSessionRepository;
    private final ModelMapper modelMapper;

    public List<DecisionOutDTO> getAll() {
        return decisionRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public DecisionOutDTO getById(Integer id) {
        Decision decision = decisionRepository.findDecisionById(id);
        if (decision == null) {
            throw new ApiException("Decision with id " + id + " not found");
        }
        return toOut(decision);
    }

    public void create(DecisionInDTO decisionInDTO) {
        Decision decision = modelMapper.map(decisionInDTO, Decision.class);

        applyRelationships(decision, decisionInDTO);

        decision.setId(null);
        decisionRepository.save(decision);
    }

    public void update(Integer id, DecisionInDTO decisionInDTO) {
        Decision decision = decisionRepository.findDecisionById(id);
        if (decision == null) {
            throw new ApiException("Decision with id " + id + " not found");
        }

        // Clear relationships first so ModelMapper only copies scalar fields
        // (never mutates the id of the currently-managed related entity).
        decision.setMissionSession(null);
        modelMapper.map(decisionInDTO, decision);
        decision.setId(id);

        applyRelationships(decision, decisionInDTO);

        decisionRepository.save(decision);
    }

    public void delete(Integer id) {
        Decision decision = decisionRepository.findDecisionById(id);
        if (decision == null) {
            throw new ApiException("Decision with id " + id + " not found");
        }
        decisionRepository.delete(decision);
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(Decision decision, DecisionInDTO dto) {
        MissionSession missionSession = missionSessionRepository.findMissionSessionById(dto.getMissionSessionId());
        if (missionSession == null) {
            throw new ApiException("MissionSession with id " + dto.getMissionSessionId() + " not found");
        }
        decision.setMissionSession(missionSession);
    }

    private DecisionOutDTO toOut(Decision decision) {
        DecisionOutDTO out = modelMapper.map(decision, DecisionOutDTO.class);
        // No derived display fields for this model (ModelMapper auto-maps missionSessionId <- missionSession.id).
        return out;
    }
}
