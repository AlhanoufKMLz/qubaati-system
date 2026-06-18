package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.BatchStudentAnswerInDTO;
import com.example.qubaatisystem.DTO.In.SingleStudentAnswerInDTO;
import com.example.qubaatisystem.DTO.In.StudentAnswerInDTO;
import com.example.qubaatisystem.DTO.Out.StudentAnswerOutDTO;
import com.example.qubaatisystem.Enum.AnswerStatus;
import com.example.qubaatisystem.Enum.ActivitySubmissionStatus;
import com.example.qubaatisystem.Model.ActivitySubmission;
import com.example.qubaatisystem.Model.Option;
import com.example.qubaatisystem.Model.Question;
import com.example.qubaatisystem.Model.Student;
import com.example.qubaatisystem.Model.StudentAnswer;
import com.example.qubaatisystem.Repository.ActivitySubmissionRepository;
import com.example.qubaatisystem.Repository.OptionRepository;
import com.example.qubaatisystem.Repository.QuestionRepository;
import com.example.qubaatisystem.Repository.StudentAnswerRepository;
import com.example.qubaatisystem.Repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentAnswerService {

    private final StudentAnswerRepository studentAnswerRepository;
    private final QuestionRepository questionRepository;
    private final StudentRepository studentRepository;
    private final ActivitySubmissionRepository activitySubmissionRepository;
    private final OptionRepository optionRepository;
    private final ModelMapper modelMapper;

    public List<StudentAnswerOutDTO> getAll() {
        return studentAnswerRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public StudentAnswerOutDTO getById(Integer id) {
        StudentAnswer studentAnswer = studentAnswerRepository.findStudentAnswerById(id);
        if (studentAnswer == null) {
            throw new ApiException("StudentAnswer with id " + id + " not found");
        }
        return toOut(studentAnswer);
    }

    public void create(StudentAnswerInDTO dto) {
        // Map scalar fields manually; relation-id fields (questionId/studentId/activitySubmissionId)
        // are resolved by applyRelationships, avoiding ModelMapper's ambiguous setId() matching.
        StudentAnswer studentAnswer = new StudentAnswer();
        studentAnswer.setAnswerText(dto.getAnswerText());
        studentAnswer.setEarnedPoints(dto.getEarnedPoints());
        studentAnswer.setStatus(dto.getStatus());

        applyRelationships(studentAnswer, dto);

        studentAnswer.setId(null);
        studentAnswerRepository.save(studentAnswer);
    }

    public void update(Integer id, StudentAnswerInDTO dto) {
        StudentAnswer studentAnswer = studentAnswerRepository.findStudentAnswerById(id);
        if (studentAnswer == null) {
            throw new ApiException("StudentAnswer with id " + id + " not found");
        }

        // Map scalar fields manually; relations are re-resolved by applyRelationships.
        studentAnswer.setAnswerText(dto.getAnswerText());
        studentAnswer.setEarnedPoints(dto.getEarnedPoints());
        studentAnswer.setStatus(dto.getStatus());
        studentAnswer.setId(id);

        applyRelationships(studentAnswer, dto);

        studentAnswerRepository.save(studentAnswer);
    }

    public void delete(Integer id) {
        StudentAnswer studentAnswer = studentAnswerRepository.findStudentAnswerById(id);
        if (studentAnswer == null) {
            throw new ApiException("StudentAnswer with id " + id + " not found");
        }
        studentAnswerRepository.delete(studentAnswer);
    }

    // ====================== FLOW: BATCH ANSWERS ======================

    public List<StudentAnswerOutDTO> saveBatchAnswers(Integer submissionId, BatchStudentAnswerInDTO dto) {
        ActivitySubmission submission = activitySubmissionRepository.findActivitySubmissionById(submissionId);
        if (submission == null) {
            throw new ApiException("ActivitySubmission with id " + submissionId + " not found");
        }
        if (submission.getStatus() != ActivitySubmissionStatus.IN_PROGRESS) {
            throw new ApiException("Answers can only be saved while the submission is IN_PROGRESS");
        }
        if (dto.getAnswers() == null || dto.getAnswers().isEmpty()) {
            throw new ApiException("answers must not be empty");
        }
        if (submission.getActivityAssignment() == null || submission.getActivityAssignment().getActivity() == null) {
            throw new ApiException("Submission is not linked to an activity");
        }

        Integer activityId = submission.getActivityAssignment().getActivity().getId();
        Student student = submission.getStudent();

        List<StudentAnswerOutDTO> results = new ArrayList<>();
        for (SingleStudentAnswerInDTO answer : dto.getAnswers()) {
            Question question = questionRepository.findQuestionById(answer.getQuestionId());
            if (question == null) {
                throw new ApiException("Question with id " + answer.getQuestionId() + " not found");
            }
            if (question.getActivity() == null || !question.getActivity().getId().equals(activityId)) {
                throw new ApiException("Question " + answer.getQuestionId() + " does not belong to this submission's activity");
            }

            boolean hasText = answer.getAnswerText() != null && !answer.getAnswerText().isBlank();
            if (!hasText && answer.getSelectedOptionId() == null) {
                throw new ApiException("Each answer must provide answerText or selectedOptionId (question " + answer.getQuestionId() + ")");
            }

            String resolvedAnswerText = answer.getAnswerText();
            if (answer.getSelectedOptionId() != null) {
                Option option = optionRepository.findOptionById(answer.getSelectedOptionId());
                if (option == null) {
                    throw new ApiException("Option with id " + answer.getSelectedOptionId() + " not found");
                }
                if (option.getQuestion() == null || !option.getQuestion().getId().equals(question.getId())) {
                    throw new ApiException("selectedOptionId " + answer.getSelectedOptionId() + " does not belong to question " + question.getId());
                }
                // StudentAnswer has no selected-option relation, so the chosen option's content is recorded
                // as the answer text. TODO: add a selectedOption relation if a stronger link is needed.
                if (!hasText) {
                    resolvedAnswerText = option.getContent();
                }
            }

            StudentAnswer existing = studentAnswerRepository
                    .findStudentAnswerByActivitySubmissionIdAndQuestionId(submissionId, question.getId());
            StudentAnswer studentAnswer = existing != null ? existing : new StudentAnswer();
            studentAnswer.setActivitySubmission(submission);
            studentAnswer.setQuestion(question);
            if (student != null) {
                studentAnswer.setStudent(student);
            }
            studentAnswer.setAnswerText(resolvedAnswerText);
            // Saved draft answers are SAVED, not SUBMITTED — the student can still change them until final
            // submit (which flips them to SUBMITTED and then evaluation sets CORRECT/INCORRECT).
            studentAnswer.setStatus(AnswerStatus.SAVED);

            results.add(toOut(studentAnswerRepository.save(studentAnswer)));
        }
        return results;
    }

    // ====================== helpers ======================

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(StudentAnswer studentAnswer, StudentAnswerInDTO dto) {
        Question question = questionRepository.findQuestionById(dto.getQuestionId());
        if (question == null) {
            throw new ApiException("Question with id " + dto.getQuestionId() + " not found");
        }
        studentAnswer.setQuestion(question);

        Student student = studentRepository.findStudentById(dto.getStudentId());
        if (student == null) {
            throw new ApiException("Student with id " + dto.getStudentId() + " not found");
        }
        studentAnswer.setStudent(student);

        ActivitySubmission submission = activitySubmissionRepository.findActivitySubmissionById(dto.getActivitySubmissionId());
        if (submission == null) {
            throw new ApiException("ActivitySubmission with id " + dto.getActivitySubmissionId() + " not found");
        }
        studentAnswer.setActivitySubmission(submission);
    }

    private StudentAnswerOutDTO toOut(StudentAnswer studentAnswer) {
        StudentAnswerOutDTO out = modelMapper.map(studentAnswer, StudentAnswerOutDTO.class);
        if (studentAnswer.getQuestion() != null) {
            out.setQuestionId(studentAnswer.getQuestion().getId());
        }
        if (studentAnswer.getStudent() != null) {
            out.setStudentId(studentAnswer.getStudent().getId());
            out.setStudentName(studentAnswer.getStudent().getFullName());
        }
        if (studentAnswer.getActivitySubmission() != null) {
            out.setActivitySubmissionId(studentAnswer.getActivitySubmission().getId());
        }
        return out;
    }
}
