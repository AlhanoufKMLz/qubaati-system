package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.InsightInDTO;
import com.example.qubaatisystem.DTO.Out.InsightOutDTO;
import com.example.qubaatisystem.Model.Insight;
import com.example.qubaatisystem.Model.MissionSession;
import com.example.qubaatisystem.Repository.InsightRepository;
import com.example.qubaatisystem.Repository.MissionSessionRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InsightService {

    private final InsightRepository insightRepository;
    private final MissionSessionRepository missionSessionRepository;
    private final ModelMapper modelMapper;

    public List<InsightOutDTO> getAll() {
        return insightRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public InsightOutDTO getById(Integer id) {
        Insight insight = insightRepository.findInsightById(id);
        if (insight == null) {
            throw new ApiException("Insight with id " + id + " not found");
        }
        return toOut(insight);
    }

    public void create(InsightInDTO dto) {
        Insight insight = modelMapper.map(dto, Insight.class);

        applyRelationships(insight, dto);

        insight.setId(null);
        insightRepository.save(insight);
    }

    public void update(Integer id, InsightInDTO dto) {
        Insight insight = insightRepository.findInsightById(id);
        if (insight == null) {
            throw new ApiException("Insight with id " + id + " not found");
        }

        // Clear relationships first so ModelMapper only copies scalar fields
        // (never mutates the ids of the currently-managed related entities).
        insight.setMissionSession(null);
        modelMapper.map(dto, insight);
        insight.setId(id);

        applyRelationships(insight, dto);

        insightRepository.save(insight);
    }

    public void delete(Integer id) {
        Insight insight = insightRepository.findInsightById(id);
        if (insight == null) {
            throw new ApiException("Insight with id " + id + " not found");
        }
        insightRepository.delete(insight);
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(Insight insight, InsightInDTO dto) {
        MissionSession missionSession = missionSessionRepository.findMissionSessionById(dto.getMissionSessionId());
        if (missionSession == null) {
            throw new ApiException("MissionSession with id " + dto.getMissionSessionId() + " not found");
        }
        insight.setMissionSession(missionSession);
    }

    private InsightOutDTO toOut(Insight insight) {
        InsightOutDTO out = modelMapper.map(insight, InsightOutDTO.class);
        // ModelMapper auto-maps all OutDTO fields for this model (including missionSessionId <- missionSession.id).
        return out;
    }
}
