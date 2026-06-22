package com.example.qubaatisystem;

import com.example.qubaatisystem.DTO.In.QuestionInDTO;
import com.example.qubaatisystem.Enum.DifficultyLevel;
import com.example.qubaatisystem.Enum.QuestionType;
import com.example.qubaatisystem.Model.Activity;
import com.example.qubaatisystem.Model.Question;
import com.example.qubaatisystem.Repository.ActivityRepository;
import com.example.qubaatisystem.Repository.QuestionRepository;
import com.example.qubaatisystem.Service.QuestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock QuestionRepository questionRepository;
    @Mock ActivityRepository activityRepository;
    @Mock ModelMapper modelMapper;

    @InjectMocks QuestionService questionService;

    @Test
    void create_doesNotAssignDtoActivityIdToQuestionId() {
        // activityId = 99 in the DTO must never end up as Question.id
        QuestionInDTO dto = new QuestionInDTO(
                "What is 2 + 2?", QuestionType.MULTIPLE_CHOICE, 5,
                DifficultyLevel.EASY, "4", 99);

        Activity activity = new Activity();
        activity.setId(99);
        when(activityRepository.findActivityById(99)).thenReturn(activity);
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        questionService.create(dto);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        Question saved = captor.getValue();

        assertThat(saved.getId()).isNull();
        assertThat(saved.getContent()).isEqualTo("What is 2 + 2?");
        assertThat(saved.getType()).isEqualTo(QuestionType.MULTIPLE_CHOICE);
        assertThat(saved.getPoints()).isEqualTo(5);
        assertThat(saved.getDifficulty()).isEqualTo(DifficultyLevel.EASY);
        assertThat(saved.getCorrectAnswer()).isEqualTo("4");
        assertThat(saved.getActivity()).isSameAs(activity);
    }

    @Test
    void update_preservesExistingIdAndAppliesCorrectActivity() {
        Question existing = new Question();
        existing.setId(5);
        existing.setContent("Old content");

        QuestionInDTO dto = new QuestionInDTO(
                "Updated content", QuestionType.TRUE_FALSE, 10,
                DifficultyLevel.MEDIUM, "True", 99);

        Activity activity = new Activity();
        activity.setId(99);

        when(questionRepository.findQuestionById(5)).thenReturn(existing);
        when(activityRepository.findActivityById(99)).thenReturn(activity);
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        questionService.update(5, dto);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        Question saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo(5);
        assertThat(saved.getContent()).isEqualTo("Updated content");
        assertThat(saved.getType()).isEqualTo(QuestionType.TRUE_FALSE);
        assertThat(saved.getPoints()).isEqualTo(10);
        assertThat(saved.getDifficulty()).isEqualTo(DifficultyLevel.MEDIUM);
        assertThat(saved.getCorrectAnswer()).isEqualTo("True");
        assertThat(saved.getActivity()).isSameAs(activity);
    }
}
