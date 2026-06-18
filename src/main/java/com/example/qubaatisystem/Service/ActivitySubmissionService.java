package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.ActivitySubmissionInDTO;
import com.example.qubaatisystem.DTO.Out.ActivitySubmissionOutDTO;
import com.example.qubaatisystem.DTO.Out.StudentActivityAttemptOutDTO;
import com.example.qubaatisystem.DTO.Out.StudentOptionAttemptOutDTO;
import com.example.qubaatisystem.DTO.Out.StudentQuestionAttemptOutDTO;
import com.example.qubaatisystem.Enum.ActivityAssignmentStatus;
import com.example.qubaatisystem.Enum.ActivitySubmissionStatus;
import com.example.qubaatisystem.Enum.AnswerStatus;
import com.example.qubaatisystem.Model.Activity;
import com.example.qubaatisystem.Model.ActivityAssignment;
import com.example.qubaatisystem.Model.ActivitySubmission;
import com.example.qubaatisystem.Model.Option;
import com.example.qubaatisystem.Model.Question;
import com.example.qubaatisystem.Model.Student;
import com.example.qubaatisystem.Model.StudentAnswer;
import com.example.qubaatisystem.Model.Teacher;
import com.example.qubaatisystem.Repository.ActivityAssignmentRepository;
import com.example.qubaatisystem.Repository.ActivitySubmissionRepository;
import com.example.qubaatisystem.Repository.OptionRepository;
import com.example.qubaatisystem.Repository.QuestionRepository;
import com.example.qubaatisystem.Repository.StudentAnswerRepository;
import com.example.qubaatisystem.Repository.StudentRepository;
import com.example.qubaatisystem.Repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivitySubmissionService {

    private final ActivitySubmissionRepository activitySubmissionRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final SkillProgressHistoryService skillProgressHistoryService;
    private final LearningStyleHistoryService learningStyleHistoryService;
    private final ModelMapper modelMapper;

    public List<ActivitySubmissionOutDTO> getAll() {
        return activitySubmissionRepository.findAll()
                .stream()
                .map(this::toOut)
                .toList();
    }

    public ActivitySubmissionOutDTO getById(Integer id) {
        ActivitySubmission activitySubmission = activitySubmissionRepository.findActivitySubmissionById(id);
        if (activitySubmission == null) {
            throw new ApiException("ActivitySubmission with id " + id + " not found");
        }
        return toOut(activitySubmission);
    }

    public void create(ActivitySubmissionInDTO dto) {
        // Manual scalar mapping: relation-id fields (activityAssignmentId, studentId)
        // are NOT copied here (they confuse ModelMapper's setId matching); they are
        // resolved separately by applyRelationships.
        ActivitySubmission activitySubmission = new ActivitySubmission();
        activitySubmission.setStartedAt(dto.getStartedAt());
        activitySubmission.setSubmittedAt(dto.getSubmittedAt());
        activitySubmission.setScore(dto.getScore());
        activitySubmission.setStatus(dto.getStatus());
        activitySubmission.setAiFeedback(dto.getAiFeedback());
        activitySubmission.setTeacherFeedback(dto.getTeacherFeedback());

        applyRelationships(activitySubmission, dto);

        activitySubmission.setId(null);
        activitySubmissionRepository.save(activitySubmission);
    }

    public void update(Integer id, ActivitySubmissionInDTO dto) {
        ActivitySubmission activitySubmission = activitySubmissionRepository.findActivitySubmissionById(id);
        if (activitySubmission == null) {
            throw new ApiException("ActivitySubmission with id " + id + " not found");
        }

        // Manual scalar mapping onto the managed entity (relations are untouched here
        // and re-resolved by applyRelationships below).
        activitySubmission.setStartedAt(dto.getStartedAt());
        activitySubmission.setSubmittedAt(dto.getSubmittedAt());
        activitySubmission.setScore(dto.getScore());
        activitySubmission.setStatus(dto.getStatus());
        activitySubmission.setAiFeedback(dto.getAiFeedback());
        activitySubmission.setTeacherFeedback(dto.getTeacherFeedback());
        activitySubmission.setId(id);

        applyRelationships(activitySubmission, dto);

        activitySubmissionRepository.save(activitySubmission);
    }

    public void delete(Integer id) {
        ActivitySubmission activitySubmission = activitySubmissionRepository.findActivitySubmissionById(id);
        if (activitySubmission == null) {
            throw new ApiException("ActivitySubmission with id " + id + " not found");
        }
        activitySubmissionRepository.delete(activitySubmission);
    }

    // ====================== FLOW: SUBMISSION ======================

    public StudentActivityAttemptOutDTO startAssignment(Integer assignmentId, Integer studentId) {
        ActivityAssignment assignment = activityAssignmentRepository.findActivityAssignmentById(assignmentId);
        if (assignment == null) {
            throw new ApiException("ActivityAssignment with id " + assignmentId + " not found");
        }
        if (assignment.getStatus() == ActivityAssignmentStatus.CANCELLED
                || assignment.getStatus() == ActivityAssignmentStatus.EXPIRED) {
            throw new ApiException("Assignment is not available (status: " + assignment.getStatus() + ")");
        }

        Student student = studentRepository.findStudentById(studentId);
        if (student == null) {
            throw new ApiException("Student with id " + studentId + " not found");
        }

        boolean directlyAssigned = assignment.getStudent() != null
                && assignment.getStudent().getId().equals(studentId);
        boolean inAssignedClassroom = assignment.getClassroom() != null
                && student.getClassroom() != null
                && assignment.getClassroom().getId().equals(student.getClassroom().getId());
        if (!directlyAssigned && !inAssignedClassroom) {
            throw new ApiException("Student " + studentId + " is not assigned to this activity");
        }

        Activity activity = assignment.getActivity();
        if (activity == null) {
            throw new ApiException("Assignment " + assignmentId + " is not linked to an activity");
        }

        boolean hasActiveSubmission = activitySubmissionRepository
                .findActivitySubmissionsByStudentIdAndActivityAssignmentId(studentId, assignmentId)
                .stream()
                .anyMatch(s -> s.getStatus() == ActivitySubmissionStatus.IN_PROGRESS);
        if (hasActiveSubmission) {
            throw new ApiException("An active (IN_PROGRESS) submission already exists for this student and assignment");
        }

        ActivitySubmission submission = new ActivitySubmission();
        submission.setActivityAssignment(assignment);
        submission.setStudent(student);
        submission.setStatus(ActivitySubmissionStatus.IN_PROGRESS);
        submission.setStartedAt(LocalDateTime.now());
        ActivitySubmission saved = activitySubmissionRepository.save(submission);

        // Return the student-safe activity content to attempt (NO correctAnswer / isCorrect exposed).
        return buildStudentAttempt(saved, assignment, activity, student);
    }

    /**
     * Builds the student-facing attempt response: activity info + each question with its options, but never
     * the question's correctAnswer nor which option isCorrect.
     */
    private StudentActivityAttemptOutDTO buildStudentAttempt(ActivitySubmission submission,
                                                             ActivityAssignment assignment,
                                                             Activity activity,
                                                             Student student) {
        List<StudentQuestionAttemptOutDTO> questionDtos = new ArrayList<>();
        for (Question question : questionRepository.findQuestionsByActivityId(activity.getId())) {
            List<StudentOptionAttemptOutDTO> optionDtos = new ArrayList<>();
            for (Option option : optionRepository.findOptionsByQuestionId(question.getId())) {
                optionDtos.add(new StudentOptionAttemptOutDTO(option.getId(), option.getContent()));
            }
            questionDtos.add(new StudentQuestionAttemptOutDTO(
                    question.getId(),
                    question.getContent(),
                    question.getType(),
                    question.getDifficulty(),
                    question.getPoints(),
                    optionDtos));
        }

        return new StudentActivityAttemptOutDTO(
                submission.getId(),
                assignment.getId(),
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getType(),
                activity.getDifficulty(),
                activity.getMaxScore(),
                submission.getStartedAt(),
                submission.getStatus(),
                student.getId(),
                student.getFullName(),
                questionDtos);
    }

    public ActivitySubmissionOutDTO getCurrentSubmission(Integer submissionId) {
        return toOut(requireSubmission(submissionId));
    }

    public ActivitySubmissionOutDTO submitActivity(Integer submissionId, String language) {
        ActivitySubmission submission = requireSubmission(submissionId);
        if (submission.getStatus() == ActivitySubmissionStatus.SUBMITTED
                || submission.getStatus() == ActivitySubmissionStatus.GRADED) {
            throw new ApiException("Submission has already been submitted (status: " + submission.getStatus() + ")");
        }
        if (submission.getStatus() == ActivitySubmissionStatus.RETURNED) {
            throw new ApiException("Submission was returned; reopen it before submitting again");
        }
        if (submission.getStatus() != ActivitySubmissionStatus.IN_PROGRESS) {
            throw new ApiException("Submission must be IN_PROGRESS to be submitted");
        }

        // All activity questions must be answered before submitting.
        List<Integer> missing = findUnansweredQuestionIds(submission);
        if (!missing.isEmpty()) {
            throw new ApiException("Cannot submit activity. Please answer all questions before submitting. "
                    + "Missing question ids: " + missing);
        }

        ActivityAssignment assignment = submission.getActivityAssignment();
        if (assignment != null && assignment.getDueDate() != null
                && LocalDateTime.now().isAfter(assignment.getDueDate())) {
            throw new ApiException("The assignment deadline has passed; submission is not allowed");
        }

        // Mark the submission SUBMITTED and flip the SAVED answers to SUBMITTED, then auto-evaluate (Issue 3):
        // the normal student flow no longer needs a separate public evaluate call.
        submission.setStatus(ActivitySubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(LocalDateTime.now());
        for (StudentAnswer answer : studentAnswerRepository.findStudentAnswersByActivitySubmissionId(submissionId)) {
            answer.setStatus(AnswerStatus.SUBMITTED);
            studentAnswerRepository.save(answer);
        }
        activitySubmissionRepository.save(submission);

        return gradeAndBuild(submission, language);
    }

    /**
     * Manual (admin/debug) re-grade for the public AI evaluate endpoint. Allowed on SUBMITTED or GRADED
     * submissions; delegates to the same single grading implementation used by automatic submit so there is
     * no duplicated evaluation logic.
     */
    public ActivitySubmissionOutDTO regrade(Integer submissionId, String language) {
        ActivitySubmission submission = requireSubmission(submissionId);
        if (submission.getStatus() != ActivitySubmissionStatus.SUBMITTED
                && submission.getStatus() != ActivitySubmissionStatus.GRADED) {
            throw new ApiException("Only a SUBMITTED or GRADED submission can be (re)evaluated");
        }
        return gradeAndBuild(submission, language);
    }

    /**
     * THE single evaluation implementation. Grades each answer (CORRECT/INCORRECT + earnedPoints), computes
     * the score normalized to the activity's maxScore, writes canonical-English aiFeedback, sets GRADED, and
     * triggers the automatic skill / learning-style history updates. For {@code language=ar} the RESPONSE
     * feedback is localized (stored value stays English). totalQuestions comes from the activity, not the
     * answer count.
     */
    private ActivitySubmissionOutDTO gradeAndBuild(ActivitySubmission submission, String language) {
        boolean arabic = isArabic(language);
        ActivityAssignment assignment = submission.getActivityAssignment();
        Activity activity = assignment != null ? assignment.getActivity() : null;
        if (activity == null) {
            throw new ApiException("Submission is not linked to an activity");
        }
        List<Question> questions = questionRepository.findQuestionsByActivityId(activity.getId());
        if (questions.isEmpty()) {
            throw new ApiException("Activity " + activity.getId() + " has no questions; cannot evaluate");
        }
        List<Integer> missing = findUnansweredQuestionIds(submission);
        if (!missing.isEmpty()) {
            throw new ApiException("Cannot evaluate an incomplete submission. Missing question ids: " + missing);
        }

        List<StudentAnswer> answers = studentAnswerRepository.findStudentAnswersByActivitySubmissionId(submission.getId());
        int totalQuestions = questions.size();
        int rawMax = 0;
        for (Question question : questions) {
            rawMax += question.getPoints() != null ? question.getPoints() : 0;
        }

        int correctCount = 0;
        int rawEarned = 0;
        Set<Integer> answeredQuestionIds = new HashSet<>();
        for (StudentAnswer answer : answers) {
            if (answer.getQuestion() != null) {
                answeredQuestionIds.add(answer.getQuestion().getId());
            }
            boolean correct = isAnswerCorrect(answer);
            answer.setStatus(correct ? AnswerStatus.CORRECT : AnswerStatus.INCORRECT);
            int earned = (correct && answer.getQuestion() != null && answer.getQuestion().getPoints() != null)
                    ? answer.getQuestion().getPoints() : 0;
            answer.setEarnedPoints(earned);
            rawEarned += earned;
            if (correct) {
                correctCount++;
            }
            studentAnswerRepository.save(answer);
        }
        int answeredQuestions = answeredQuestionIds.size();

        int officialMax = (activity.getMaxScore() != null && activity.getMaxScore() > 0)
                ? activity.getMaxScore() : rawMax;
        int finalScore = (rawMax > 0 && officialMax > 0)
                ? Math.round((float) rawEarned * officialMax / rawMax) : rawEarned;

        String englishFeedback = "Total questions: " + totalQuestions
                + ". Answered: " + answeredQuestions + "/" + totalQuestions
                + ". Correct: " + correctCount + "/" + totalQuestions
                + ". Score: " + finalScore + "/" + officialMax + ".";

        submission.setScore(finalScore);
        submission.setAiFeedback(englishFeedback);
        submission.setStatus(ActivitySubmissionStatus.GRADED);
        ActivitySubmission graded = activitySubmissionRepository.save(submission);

        // Automatic, heuristic progress updates after grading (each may skip and document if not applicable).
        Student student = graded.getStudent();
        skillProgressHistoryService.recordAutomaticSkillProgress(student, finalScore, officialMax, activity.getTitle());
        learningStyleHistoryService.recordAutomaticLearningStyleUpdate(student, activity.getType(), activity.getTitle());

        ActivitySubmissionOutDTO out = toOut(graded);
        if (arabic) {
            out.setAiFeedback("إجمالي الأسئلة: " + totalQuestions
                    + ". تمت الإجابة عن: " + answeredQuestions + "/" + totalQuestions
                    + ". الإجابات الصحيحة: " + correctCount + "/" + totalQuestions
                    + ". الدرجة: " + finalScore + "/" + officialMax + ".");
        }
        return out;
    }

    private boolean isArabic(String language) {
        if (language == null || language.isBlank()) {
            return false;
        }
        String normalized = language.trim().toLowerCase();
        if (!normalized.equals("en") && !normalized.equals("ar")) {
            throw new ApiException("Language must be either en or ar");
        }
        return normalized.equals("ar");
    }

    private boolean isAnswerCorrect(StudentAnswer answer) {
        Question question = answer.getQuestion();
        if (question == null) {
            return false;
        }
        String given = answer.getAnswerText() == null ? "" : answer.getAnswerText().trim();
        if (given.isEmpty()) {
            return false;
        }
        if (question.getCorrectAnswer() != null && given.equalsIgnoreCase(question.getCorrectAnswer().trim())) {
            return true;
        }
        for (Option option : optionRepository.findOptionsByQuestionId(question.getId())) {
            if (Boolean.TRUE.equals(option.getIsCorrect())
                    && option.getContent() != null
                    && given.equalsIgnoreCase(option.getContent().trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds teacher feedback to a submission WITHOUT changing its status (Issue 9). Allowed once the work has
     * been submitted (SUBMITTED/GRADED/RETURNED), not while still IN_PROGRESS.
     */
    public ActivitySubmissionOutDTO addTeacherFeedback(Integer submissionId, Integer teacherId, String teacherFeedback) {
        ActivitySubmission submission = requireSubmission(submissionId);
        Teacher teacher = teacherRepository.findTeacherById(teacherId);
        if (teacher == null) {
            throw new ApiException("Teacher with id " + teacherId + " not found");
        }
        if (teacherFeedback == null || teacherFeedback.isBlank()) {
            throw new ApiException("teacherFeedback must not be blank");
        }
        if (submission.getStatus() != ActivitySubmissionStatus.SUBMITTED
                && submission.getStatus() != ActivitySubmissionStatus.GRADED
                && submission.getStatus() != ActivitySubmissionStatus.RETURNED) {
            throw new ApiException("Teacher feedback can only be added to a SUBMITTED, GRADED or RETURNED submission");
        }
        submission.setTeacherFeedback(teacherFeedback);
        return toOut(activitySubmissionRepository.save(submission));
    }

    /**
     * Returns the ids of the activity's questions that have no answer in this submission. Throws if the
     * submission is not linked to an activity, or the activity has zero questions (an invalid activity).
     * Shared by submit (Issue 6) and AI evaluation (Issue 8) so both use the same completeness rule.
     */
    List<Integer> findUnansweredQuestionIds(ActivitySubmission submission) {
        ActivityAssignment assignment = submission.getActivityAssignment();
        Activity activity = assignment != null ? assignment.getActivity() : null;
        if (activity == null) {
            throw new ApiException("Submission is not linked to an activity");
        }
        List<Question> questions = questionRepository.findQuestionsByActivityId(activity.getId());
        if (questions.isEmpty()) {
            throw new ApiException("Activity " + activity.getId() + " has no questions; the activity is invalid");
        }
        Set<Integer> answeredQuestionIds = studentAnswerRepository
                .findStudentAnswersByActivitySubmissionId(submission.getId())
                .stream()
                .map(StudentAnswer::getQuestion)
                .filter(q -> q != null)
                .map(Question::getId)
                .collect(Collectors.toSet());
        return questions.stream()
                .map(Question::getId)
                .filter(id -> !answeredQuestionIds.contains(id))
                .toList();
    }

    public ActivitySubmissionOutDTO getSubmissionResult(Integer submissionId) {
        return toOut(requireSubmission(submissionId));
    }

    public ActivitySubmissionOutDTO getSubmissionFeedback(Integer submissionId) {
        // Returns the submission with aiFeedback, teacherFeedback, score, status and submittedAt.
        return toOut(requireSubmission(submissionId));
    }

    public List<ActivitySubmissionOutDTO> getStudentActivityResults(Integer studentId) {
        if (studentRepository.findStudentById(studentId) == null) {
            throw new ApiException("Student with id " + studentId + " not found");
        }
        return activitySubmissionRepository.findActivitySubmissionsByStudentId(studentId)
                .stream()
                .map(this::toOut)
                .toList();
    }

    public void returnToStudent(Integer submissionId, Integer teacherId, String teacherFeedback) {
        ActivitySubmission submission = requireSubmission(submissionId);

        Teacher teacher = teacherRepository.findTeacherById(teacherId);
        if (teacher == null) {
            throw new ApiException("Teacher with id " + teacherId + " not found");
        }
        ActivityAssignment assignment = submission.getActivityAssignment();
        if (assignment != null && assignment.getAssignedByTeacher() != null
                && !assignment.getAssignedByTeacher().getId().equals(teacherId)) {
            throw new ApiException("Only the assigning teacher can return this submission");
        }
        if (submission.getStatus() != ActivitySubmissionStatus.SUBMITTED
                && submission.getStatus() != ActivitySubmissionStatus.GRADED) {
            throw new ApiException("Only SUBMITTED or GRADED submissions can be returned to the student");
        }
        // teacherFeedback is optional; apply a default when blank (Issue 1).
        String feedback = (teacherFeedback == null || teacherFeedback.isBlank())
                ? "Please review your answers and try again." : teacherFeedback;

        submission.setTeacherFeedback(feedback);
        submission.setStatus(ActivitySubmissionStatus.RETURNED);
        activitySubmissionRepository.save(submission);
    }

    public StudentActivityAttemptOutDTO reopenSubmission(Integer submissionId) {
        ActivitySubmission submission = requireSubmission(submissionId);
        if (submission.getStatus() != ActivitySubmissionStatus.RETURNED) {
            throw new ApiException("Only a RETURNED submission can be reopened");
        }
        ActivityAssignment assignment = submission.getActivityAssignment();
        if (assignment != null && (assignment.getStatus() == ActivityAssignmentStatus.CANCELLED
                || assignment.getStatus() == ActivityAssignmentStatus.EXPIRED)) {
            throw new ApiException("Cannot reopen: the assignment is " + assignment.getStatus());
        }
        Activity activity = assignment != null ? assignment.getActivity() : null;
        if (activity == null) {
            throw new ApiException("Submission is not linked to an activity");
        }

        // Reopen for another attempt (Issue 10): clear grading outputs but KEEP teacherFeedback (it explains
        // what to fix) and KEEP the existing answers so the student can edit them.
        submission.setStatus(ActivitySubmissionStatus.IN_PROGRESS);
        submission.setSubmittedAt(null);
        submission.setScore(null);
        submission.setAiFeedback(null);
        ActivitySubmission reopened = activitySubmissionRepository.save(submission);

        for (StudentAnswer answer : studentAnswerRepository.findStudentAnswersByActivitySubmissionId(submissionId)) {
            answer.setEarnedPoints(null);
            answer.setStatus(AnswerStatus.SAVED);
            studentAnswerRepository.save(answer);
        }

        // Return the student-safe attempt view (never exposes correctAnswer / isCorrect).
        return buildStudentAttempt(reopened, assignment, activity, reopened.getStudent());
    }

    public List<ActivitySubmissionOutDTO> getPendingGradingSubmissions(Integer teacherId) {
        if (teacherRepository.findTeacherById(teacherId) == null) {
            throw new ApiException("Teacher with id " + teacherId + " not found");
        }
        return activitySubmissionRepository.findActivitySubmissionsByStatus(ActivitySubmissionStatus.SUBMITTED)
                .stream()
                .filter(s -> s.getActivityAssignment() != null
                        && s.getActivityAssignment().getAssignedByTeacher() != null
                        && s.getActivityAssignment().getAssignedByTeacher().getId().equals(teacherId))
                .map(this::toOut)
                .toList();
    }

    // ====================== helpers ======================

    private ActivitySubmission requireSubmission(Integer submissionId) {
        ActivitySubmission submission = activitySubmissionRepository.findActivitySubmissionById(submissionId);
        if (submission == null) {
            throw new ApiException("ActivitySubmission with id " + submissionId + " not found");
        }
        return submission;
    }

    // Relationship IDs from the input DTO are resolved manually (ModelMapper maps scalar fields only).
    private void applyRelationships(ActivitySubmission activitySubmission, ActivitySubmissionInDTO dto) {
        ActivityAssignment activityAssignment = activityAssignmentRepository.findActivityAssignmentById(dto.getActivityAssignmentId());
        if (activityAssignment == null) {
            throw new ApiException("ActivityAssignment with id " + dto.getActivityAssignmentId() + " not found");
        }
        activitySubmission.setActivityAssignment(activityAssignment);

        Student student = studentRepository.findStudentById(dto.getStudentId());
        if (student == null) {
            throw new ApiException("Student with id " + dto.getStudentId() + " not found");
        }
        activitySubmission.setStudent(student);
    }

    private ActivitySubmissionOutDTO toOut(ActivitySubmission activitySubmission) {
        ActivitySubmissionOutDTO out = modelMapper.map(activitySubmission, ActivitySubmissionOutDTO.class);
        // Manually set relation-derived fields (activity comes through the assignment).
        if (activitySubmission.getActivityAssignment() != null) {
            out.setActivityAssignmentId(activitySubmission.getActivityAssignment().getId());
            if (activitySubmission.getActivityAssignment().getActivity() != null) {
                out.setActivityId(activitySubmission.getActivityAssignment().getActivity().getId());
                out.setActivityTitle(activitySubmission.getActivityAssignment().getActivity().getTitle());
                out.setActivityMaxScore(activitySubmission.getActivityAssignment().getActivity().getMaxScore());
            }
        }
        if (activitySubmission.getStudent() != null) {
            out.setStudentId(activitySubmission.getStudent().getId());
            out.setStudentName(activitySubmission.getStudent().getFullName());
        }
        return out;
    }
}
