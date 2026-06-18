package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.OptionInDTO;
import com.example.qubaatisystem.DTO.Out.OptionOutDTO;
import com.example.qubaatisystem.Model.Option;
import com.example.qubaatisystem.Model.Question;
import com.example.qubaatisystem.Repository.OptionRepository;
import com.example.qubaatisystem.Repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OptionService {

    private final OptionRepository optionRepository;
    private final QuestionRepository questionRepository;
    private final ModelMapper modelMapper;

    public List<OptionOutDTO> getAll() {
        return optionRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public OptionOutDTO getById(Integer id) {
        Option option = optionRepository.findOptionById(id);
        if (option == null) {
            throw new ApiException("Option with id " + id + " not found");
        }
        return toOut(option);
    }

    public void create(OptionInDTO optionInDTO) {
        Option option = modelMapper.map(optionInDTO, Option.class);

        applyRelationships(option, optionInDTO);

        option.setId(null);
        optionRepository.save(option);
    }

    public void update(Integer id, OptionInDTO optionInDTO) {
        Option option = optionRepository.findOptionById(id);
        if (option == null) {
            throw new ApiException("Option with id " + id + " not found");
        }

        // Clear relationships first so ModelMapper only copies scalar fields
        // (never mutates the ids of the currently-managed related entities).
        option.setQuestion(null);
        modelMapper.map(optionInDTO, option);
        option.setId(id);

        applyRelationships(option, optionInDTO);

        optionRepository.save(option);
    }

    public void delete(Integer id) {
        Option option = optionRepository.findOptionById(id);
        if (option == null) {
            throw new ApiException("Option with id " + id + " not found");
        }
        optionRepository.delete(option);
    }

    // ---------- helpers ----------

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(Option option, OptionInDTO dto) {
        Question question = questionRepository.findQuestionById(dto.getQuestionId());
        if (question == null) {
            throw new ApiException("Question with id " + dto.getQuestionId() + " not found");
        }
        option.setQuestion(question);
    }

    private OptionOutDTO toOut(Option option) {
        OptionOutDTO out = modelMapper.map(option, OptionOutDTO.class);
        return out;
    }
}
