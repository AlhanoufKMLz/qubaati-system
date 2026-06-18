package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.CareerWorldInDTO;
import com.example.qubaatisystem.DTO.Out.CareerWorldOutDTO;
import com.example.qubaatisystem.Model.CareerWorld;
import com.example.qubaatisystem.Repository.CareerWorldRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerWorldService {

    private final CareerWorldRepository careerWorldRepository;
    private final ModelMapper modelMapper;

    public List<CareerWorldOutDTO> getAll() {
        return careerWorldRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public CareerWorldOutDTO getById(Integer id) {
        CareerWorld careerWorld = careerWorldRepository.findCareerWorldById(id);
        if (careerWorld == null) {
            throw new ApiException("CareerWorld with id " + id + " not found");
        }
        return toOut(careerWorld);
    }

    public void create(CareerWorldInDTO dto) {
        CareerWorld careerWorld = modelMapper.map(dto, CareerWorld.class);

        careerWorld.setId(null);
        careerWorldRepository.save(careerWorld);
    }

    public void update(Integer id, CareerWorldInDTO dto) {
        CareerWorld careerWorld = careerWorldRepository.findCareerWorldById(id);
        if (careerWorld == null) {
            throw new ApiException("CareerWorld with id " + id + " not found");
        }

        modelMapper.map(dto, careerWorld);
        careerWorld.setId(id);

        careerWorldRepository.save(careerWorld);
    }

    public void delete(Integer id) {
        CareerWorld careerWorld = careerWorldRepository.findCareerWorldById(id);
        if (careerWorld == null) {
            throw new ApiException("CareerWorld with id " + id + " not found");
        }
        careerWorldRepository.delete(careerWorld);
    }

    // ---------- helpers ----------

    private CareerWorldOutDTO toOut(CareerWorld careerWorld) {
        return modelMapper.map(careerWorld, CareerWorldOutDTO.class);
    }
}
