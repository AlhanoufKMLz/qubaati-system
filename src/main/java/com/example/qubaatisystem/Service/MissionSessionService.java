package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.MissionInsightAiResult;
import com.example.qubaatisystem.DTO.In.MissionSessionInDTO;
import com.example.qubaatisystem.DTO.In.RecommendationAiItem;
import com.example.qubaatisystem.DTO.Out.DecisionSubmitOutDTO;
import com.example.qubaatisystem.DTO.Out.InsightOutDTO;
import com.example.qubaatisystem.DTO.Out.MissionCompletionOutDTO;
import com.example.qubaatisystem.DTO.Out.MissionSessionOutDTO;
import com.example.qubaatisystem.DTO.Out.MissionStepOutDTO;
import com.example.qubaatisystem.DTO.Out.RecommendationOutDTO;
import com.example.qubaatisystem.DTO.Out.SkillUpdateOutDTO;
import com.example.qubaatisystem.DTO.Out.StudentMissionAttemptOutDTO;
import com.example.qubaatisystem.DTO.Out.StudentMissionChoiceOutDTO;
import com.example.qubaatisystem.Enum.MissionSessionStatus;
import com.example.qubaatisystem.Enum.MissionSource;
import com.example.qubaatisystem.Enum.NotificationType;
import com.example.qubaatisystem.Enum.RecommendationPriority;
import com.example.qubaatisystem.Enum.RecommendationType;
import com.example.qubaatisystem.Model.CareerWorld;
import com.example.qubaatisystem.Model.Decision;
import com.example.qubaatisystem.Model.Insight;
import com.example.qubaatisystem.Model.LearningStyle;
import com.example.qubaatisystem.Model.Mission;
import com.example.qubaatisystem.Model.MissionChoice;
import com.example.qubaatisystem.Model.MissionSession;
import com.example.qubaatisystem.Model.MissionStep;
import com.example.qubaatisystem.Model.Student;
import com.example.qubaatisystem.Model.StudentSkill;
import com.example.qubaatisystem.Model.User;
import com.example.qubaatisystem.Repository.DecisionRepository;
import com.example.qubaatisystem.Repository.InsightRepository;
import com.example.qubaatisystem.Repository.MissionChoiceRepository;
import com.example.qubaatisystem.Repository.MissionRepository;
import com.example.qubaatisystem.Repository.MissionSessionRepository;
import com.example.qubaatisystem.Repository.MissionStepRepository;
import com.example.qubaatisystem.Repository.StudentRepository;
import com.example.qubaatisystem.Repository.StudentSkillRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionSessionService {

    private final MissionSessionRepository missionSessionRepository;
    private final MissionRepository missionRepository;
    private final MissionChoiceRepository missionChoiceRepository;
    private final MissionStepRepository missionStepRepository;
    private final DecisionRepository decisionRepository;
    private final InsightRepository insightRepository;
    private final StudentRepository studentRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final MissionService missionService;
    private final AiService aiService;
    private final SkillProgressHistoryService skillProgressHistoryService;
    private final LearningStyleHistoryService learningStyleHistoryService;
    private final RecommendationService recommendationService;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;

    // ====================== CRUD ======================

    public List<MissionSessionOutDTO> getAll() {
        return missionSessionRepository.findAll().stream().map(this::toOut).toList();
    }

    public MissionSessionOutDTO getById(Integer id) {
        return toOut(requireSession(id));
    }

    public List<MissionSessionOutDTO> getMissionHistoryByStudentId(Integer studentId) {
        return missionSessionRepository.findMissionSessionsByStudentId(studentId)
                .stream()
                .map(this::toOut)
                .toList();
    }

    // Generic mutation of MissionSession is DISABLED: it would let a client create/advance/relabel a session
    // outside the guarded multi-step flow (start/decision/complete/abandon). Read endpoints stay.
    public void create(MissionSessionInDTO dto) {
        throw new ApiException("Direct MissionSession creation is disabled. Start a session via "
                + "POST /api/v1/missions/{missionId}/sessions/start?studentId=...");
    }

    public void update(Integer id, MissionSessionInDTO dto) {
        throw new ApiException("Direct MissionSession update is disabled (it would bypass step/score/status logic). "
                + "Use submit-decision, complete, or abandon.");
    }

    public void delete(Integer id) {
        throw new ApiException("Direct MissionSession deletion is disabled. Use "
                + "PATCH /api/v1/mission-sessions/{sessionId}/abandon to end a session.");
    }

    // ====================== MISSION SESSION FLOW ======================

    /** Start a session and return the student-safe attempt (no hidden scoring/outcomes). */
    @Transactional
    public StudentMissionAttemptOutDTO startSession(Integer missionId, Integer studentId) {
        Student student = requireStudent(studentId);
        Mission mission = requireMission(missionId);
        if (mission.getSource() == MissionSource.AI_GENERATED
                && (mission.getGeneratedForStudent() == null
                || !mission.getGeneratedForStudent().getId().equals(studentId))) {
            throw new ApiException("This personalized mission does not belong to student " + studentId);
        }
        LocalDateTime now = LocalDateTime.now();
        MissionSession session = new MissionSession();
        session.setMission(mission);
        session.setStudent(student);
        session.setStatus(MissionSessionStatus.STARTED);
        session.setStartTime(now);
        session.setCurrentStepStartedAt(now);
        session.setScore(0);
        session.setMissionCompleteReady(false);

        // Multi-step missions start on their first step; legacy single-step missions have no MissionStep rows
        // (currentStep stays null and the mission's own scenario/choices are used — no data is migrated).
        List<MissionStep> steps = missionStepRepository.findMissionStepsByMissionIdOrderByStepOrderAsc(missionId);
        if (!steps.isEmpty()) {
            MissionStep first = steps.get(0);
            session.setCurrentStep(first);
            session.setCurrentStepOrder(first.getStepOrder());
        } else {
            session.setCurrentStepOrder(1);
        }
        session.setId(null);
        MissionSession saved = missionSessionRepository.save(session);
        return buildAttempt(saved, mission, student);
    }

    /** Current session state for reload (student-safe attempt view). */
    public StudentMissionAttemptOutDTO getCurrentSession(Integer sessionId) {
        MissionSession session = requireSession(sessionId);
        return buildAttempt(session, session.getMission(), session.getStudent());
    }

    /**
     * Submit a decision. responseTimeSeconds is backend-calculated from currentStepStartedAt; the client never
     * supplies it. For multi-step missions the choice must belong to the session's current step; the next step
     * is resolved from the choice's nextStepOrder (or the next step in sequence). When there is no next step the
     * mission becomes complete-ready.
     */
    @Transactional
    public DecisionSubmitOutDTO submitDecision(Integer sessionId, Integer choiceId, String reason) {
        MissionSession session = requireSession(sessionId);
        if (session.getStatus() != MissionSessionStatus.STARTED) {
            throw new ApiException("Decisions can only be submitted while the session is STARTED");
        }
        if (Boolean.TRUE.equals(session.getMissionCompleteReady())) {
            throw new ApiException("All steps are done; complete the mission instead of submitting more decisions");
        }
        Mission mission = session.getMission();
        MissionChoice choice = missionChoiceRepository.findMissionChoiceById(choiceId);
        if (choice == null) {
            throw new ApiException("MissionChoice with id " + choiceId + " not found");
        }

        MissionStep currentStep = session.getCurrentStep();
        if (currentStep != null) {
            // Multi-step: the choice must belong to the current step.
            if (choice.getMissionStep() == null || !choice.getMissionStep().getId().equals(currentStep.getId())) {
                throw new ApiException("Choice " + choiceId + " does not belong to the current step");
            }
        } else {
            // Legacy single-step mission: the choice must belong to this session's mission.
            if (choice.getMission() == null || mission == null || !choice.getMission().getId().equals(mission.getId())) {
                throw new ApiException("Choice " + choiceId + " does not belong to this session's mission");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        double responseTime = session.getCurrentStepStartedAt() != null
                ? Math.max(0.0, Duration.between(session.getCurrentStepStartedAt(), now).toMillis() / 1000.0)
                : 0.0;

        Decision decision = new Decision();
        decision.setChoice(choice.getText());
        decision.setReason(reason);
        decision.setResponseTimeSeconds(responseTime);
        decision.setScoreImpact(choice.getScoreImpact());
        decision.setSubmittedAt(now);
        decision.setMissionSession(session);
        decision.setMissionStep(currentStep);
        decision.setId(null);
        Decision savedDecision = decisionRepository.save(decision);

        int current = session.getScore() != null ? session.getScore() : 0;
        session.setScore(current + (choice.getScoreImpact() != null ? choice.getScoreImpact() : 0));
        session.setCurrentStepStartedAt(now);

        // Resolve the next step (multi-step branching). Legacy single-step missions have no next step.
        MissionStep nextStep = null;
        if (currentStep != null && mission != null) {
            Integer targetOrder = choice.getNextStepOrder() != null
                    ? choice.getNextStepOrder()
                    : currentStep.getStepOrder() + 1;
            nextStep = missionStepRepository.findMissionStepByMissionIdAndStepOrder(mission.getId(), targetOrder);
        }

        boolean completeReady;
        MissionStepOutDTO nextStepView;
        if (nextStep != null) {
            session.setCurrentStep(nextStep);
            session.setCurrentStepOrder(nextStep.getStepOrder());
            session.setMissionCompleteReady(false);
            completeReady = false;
            nextStepView = buildStepView(nextStep);
        } else {
            session.setMissionCompleteReady(true);
            completeReady = true;
            nextStepView = null;
        }
        missionSessionRepository.save(session);

        return new DecisionSubmitOutDTO(savedDecision.getId(), sessionId, choiceId, responseTime, now,
                completeReady, nextStepView);
    }

    /** Complete the session and trigger all internal analytics + personalized-generation rules. */
    @Transactional
    public MissionCompletionOutDTO completeSession(Integer sessionId) {
        MissionSession session = requireSession(sessionId);
        if (session.getStatus() != MissionSessionStatus.STARTED) {
            throw new ApiException("Only a STARTED session can be completed (status: " + session.getStatus() + ")");
        }
        if (!Boolean.TRUE.equals(session.getMissionCompleteReady())) {
            throw new ApiException("Mission is not ready to complete yet; submit decisions until the final step");
        }
        Mission mission = session.getMission();
        Student student = session.getStudent();
        int score = session.getScore() != null ? session.getScore() : 0;

        session.setStatus(MissionSessionStatus.COMPLETED);
        session.setEndTime(LocalDateTime.now());
        MissionSession saved = missionSessionRepository.save(session);

        // Mission progress counters (Student.completedMissionsCount + totalPoints). IDEMPOTENT: only a STARTED
        // session reaches here (it is now COMPLETED), so a repeated complete call fails the status guard above
        // and never double-counts; abandon/not-ready paths never reach here. Applies to BOTH default and
        // AI_GENERATED missions. Earned points = the session score (the sum of the chosen choices' scoreImpacts,
        // accumulated in submitDecision), clamped to [0, mission.maxScore].
        if (student != null) {
            int earned = Math.max(0, score);
            if (mission != null && mission.getMaxScore() != null && mission.getMaxScore() > 0) {
                earned = Math.min(earned, mission.getMaxScore());
            }
            int prevCompleted = student.getCompletedMissionsCount() != null ? student.getCompletedMissionsCount() : 0;
            int prevPoints = student.getTotalPoints() != null ? student.getTotalPoints() : 0;
            student.setCompletedMissionsCount(prevCompleted + 1);
            student.setTotalPoints(prevPoints + earned);
            studentRepository.save(student);
        }

        // 1-2. Insight: AI-first (Spring AI), deterministic rule-based fallback on any failure.
        Insight insight = buildInsight(saved, mission, student, score);

        // 3-5. Skills + skill history, then learning style (heuristic, reuse existing helpers).
        List<SkillUpdateOutDTO> updatedSkills = new ArrayList<>();
        int maxScore = (mission != null && mission.getMaxScore() != null && mission.getMaxScore() > 0)
                ? mission.getMaxScore() : 100;
        if (mission != null && mission.getSkill() != null) {
            SkillUpdateOutDTO update = skillProgressHistoryService
                    .recordMissionSkillProgress(student, mission.getSkill(), score, maxScore,
                            mission.getTitle());
            if (update != null) {
                updatedSkills.add(update);
            }
        }
        learningStyleHistoryService.recordMissionLearningStyleUpdate(student,
                mission != null ? mission.getTitle() : "mission");

        // 6. Recommendations: AI-first (Spring AI), deterministic rule-based fallback on any failure.
        if (student != null) {
            generateRecommendationsForStudent(student, mission);
        }

        // 7. Notification: mission completed.
        User recipient = student != null ? student.getUser() : null;
        notificationService.notify(recipient, NotificationType.MISSION_COMPLETED,
                "Mission completed",
                "You completed the mission: " + (mission != null ? mission.getTitle() : ""));

        // 8. Personalized mission generation rules.
        boolean unlocked = false;
        boolean newGenerated = false;
        if (mission != null && student != null && mission.getCareerWorld() != null) {
            CareerWorld careerWorld = mission.getCareerWorld();
            unlocked = missionService.isUnlocked(student.getId(), careerWorld.getId());
            if (mission.getSource() == MissionSource.AI_GENERATED) {
                // Rule 4: keep completed generated mission as history (inactive); create a replacement if room.
                mission.setActive(false);
                missionRepository.save(mission);
                newGenerated = missionService.createOneGeneratedMissionIfRoom(student, careerWorld);
            } else {
                // DEFAULT completed: ensure up to 2 active generated missions once >= 4 are completed.
                newGenerated = missionService.ensurePersonalizedMissionsUnlocked(student, careerWorld) > 0;
            }
            if (newGenerated) {
                notificationService.notify(recipient, NotificationType.SYSTEM_MESSAGE,
                        "New personalized mission ready",
                        "A new personalized mission is available in " + careerWorld.getTitle());
            }
        }

        return new MissionCompletionOutDTO(
                saved.getId(),
                mission != null ? mission.getId() : null,
                student != null ? student.getId() : null,
                saved.getStatus(),
                score,
                saved.getEndTime(),
                toInsightOut(insight),
                updatedSkills,
                unlocked,
                newGenerated);
    }

    /** Abandon a started session: no insight, no skill/style updates, no personalized generation. */
    public void abandonSession(Integer sessionId) {
        MissionSession session = requireSession(sessionId);
        if (session.getStatus() != MissionSessionStatus.STARTED) {
            throw new ApiException("Only a STARTED session can be abandoned (status: " + session.getStatus() + ")");
        }
        session.setStatus(MissionSessionStatus.ABANDONED);
        session.setEndTime(LocalDateTime.now());
        missionSessionRepository.save(session);
    }

    /** The insight generated when the mission was completed (never a public AI call). */
    public InsightOutDTO getInsight(Integer sessionId) {
        requireSession(sessionId);
        Insight insight = insightRepository.findInsightByMissionSessionId(sessionId);
        if (insight == null) {
            throw new ApiException("Insight has not been generated yet for session " + sessionId);
        }
        return toInsightOut(insight);
    }

    /**
     * OPTIONAL manual re-run of the AI-first insight for a completed session (updates it in place). The normal
     * flow already generates the insight on completion; this is a teacher/admin convenience only.
     */
    @Transactional
    public InsightOutDTO regenerateInsight(Integer sessionId) {
        MissionSession session = requireSession(sessionId);
        if (session.getStatus() != MissionSessionStatus.COMPLETED) {
            throw new ApiException("Insight can only be regenerated for a completed session");
        }
        Insight insight = insightRepository.findInsightByMissionSessionId(sessionId);
        if (insight == null) {
            insight = new Insight();
            insight.setMissionSession(session);
            insight.setId(null);
        }
        populateInsight(insight, session, session.getMission(), session.getStudent(),
                session.getScore() != null ? session.getScore() : 0);
        return toInsightOut(insightRepository.save(insight));
    }

    /**
     * OPTIONAL manual re-run of the AI-first recommendations for a student. The normal flow already creates
     * recommendations on mission completion; this is a teacher/admin convenience only.
     */
    @Transactional
    public List<RecommendationOutDTO> regenerateRecommendations(Integer studentId) {
        Student student = requireStudent(studentId);
        generateRecommendationsForStudent(student, null);
        return recommendationService.getByStudent(studentId);
    }

    // ====================== helpers ======================

    private MissionSession requireSession(Integer id) {
        MissionSession session = missionSessionRepository.findMissionSessionById(id);
        if (session == null) {
            throw new ApiException("MissionSession with id " + id + " not found");
        }
        return session;
    }

    private Student requireStudent(Integer id) {
        Student student = studentRepository.findStudentById(id);
        if (student == null) {
            throw new ApiException("Student with id " + id + " not found");
        }
        return student;
    }

    private Mission requireMission(Integer id) {
        Mission mission = missionRepository.findMissionById(id);
        if (mission == null) {
            throw new ApiException("Mission with id " + id + " not found");
        }
        return mission;
    }

    private StudentMissionAttemptOutDTO buildAttempt(MissionSession session, Mission mission, Student student) {
        Integer careerWorldId = (mission != null && mission.getCareerWorld() != null)
                ? mission.getCareerWorld().getId() : null;
        return new StudentMissionAttemptOutDTO(
                session.getId(),
                mission != null ? mission.getId() : null,
                student != null ? student.getId() : null,
                careerWorldId,
                mission != null ? mission.getTitle() : null,
                mission != null ? mission.getScenario() : null,
                mission != null ? mission.getSource() : null,
                session.getStatus(),
                session.getStartTime(),
                buildCurrentStepView(session, mission));
    }

    /** Student-safe view of the session's current step (multi-step) or the legacy single step. */
    private MissionStepOutDTO buildCurrentStepView(MissionSession session, Mission mission) {
        if (session.getCurrentStep() != null) {
            return buildStepView(session.getCurrentStep());
        }
        // Legacy single-step mission: scenario + the mission's own choices (no MissionStep rows).
        List<StudentMissionChoiceOutDTO> choices = new ArrayList<>();
        if (mission != null && mission.getMissionChoices() != null) {
            mission.getMissionChoices().stream()
                    .sorted(Comparator.comparing(MissionChoice::getChoiceKey,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .forEach(c -> choices.add(new StudentMissionChoiceOutDTO(c.getId(), c.getText())));
        }
        return new MissionStepOutDTO(1, mission != null ? mission.getScenario() : null, choices);
    }

    /** Student-safe view of one persisted MissionStep (never exposes scoreImpact / branch targets). */
    private MissionStepOutDTO buildStepView(MissionStep step) {
        List<StudentMissionChoiceOutDTO> choices = new ArrayList<>();
        for (MissionChoice c : missionChoiceRepository.findMissionChoicesByMissionStepIdOrderByChoiceKeyAsc(step.getId())) {
            choices.add(new StudentMissionChoiceOutDTO(c.getId(), c.getText()));
        }
        return new MissionStepOutDTO(step.getStepOrder(), step.getScenario(), choices);
    }

    // ---------- insight (AI-first, rule-based fallback) ----------

    private Insight buildInsight(MissionSession session, Mission mission, Student student, int score) {
        Insight insight = new Insight();
        insight.setMissionSession(session);
        insight.setId(null);
        populateInsight(insight, session, mission, student, score);
        return insightRepository.save(insight);
    }

    /** Fills an Insight in place (AI-first, rule-based fallback). Used by both completion and regenerate. */
    private void populateInsight(Insight insight, MissionSession session, Mission mission, Student student, int score) {
        List<Decision> decisions = decisionRepository.findDecisionsByMissionSessionId(session.getId());
        List<StudentSkill> currentSkills = student != null
                ? studentSkillRepository.findStudentSkillsByStudentId(student.getId())
                : new ArrayList<>();
        LearningStyle learningStyle = student != null ? student.getLearningStyle() : null;
        int pct = scorePct(mission, score);

        MissionInsightAiResult ai = aiService.generateMissionInsight(student, mission, session, decisions,
                currentSkills, learningStyle);
        if (ai != null) {
            insight.setFocusScore(clampScore(ai.getFocusScore(), pct));
            insight.setEngagementScore(clampScore(ai.getEngagementScore(), Math.min(100, pct + 10)));
            insight.setReasoningScore(clampScore(ai.getReasoningScore(), pct));
            insight.setProblemSolvingScore(clampScore(ai.getProblemSolvingScore(), pct));
            insight.setDecisionMakingScore(clampScore(ai.getDecisionMakingScore(), pct));
            insight.setSummary(ai.getSummary());
            insight.setRecommendation(ai.getRecommendation() != null && !ai.getRecommendation().isBlank()
                    ? ai.getRecommendation()
                    : "Keep practicing similar decision scenarios.");
            return;
        }

        // Rule-based fallback.
        insight.setProblemSolvingScore(pct);
        insight.setDecisionMakingScore(pct);
        insight.setReasoningScore(pct);
        insight.setFocusScore(pct);
        insight.setEngagementScore(Math.min(100, pct + 10));
        String missionTitle = mission != null ? mission.getTitle() : "the mission";
        String level = pct >= 75 ? "strong" : pct >= 40 ? "developing" : "early-stage";
        insight.setSummary("The student showed " + level + " decision-making on \"" + missionTitle
                + "\" (engagement score " + pct + "%).");
        insight.setRecommendation(pct >= 75
                ? "Keep challenging the student with similar decision scenarios."
                : "Encourage the student to weigh the options carefully before deciding.");
    }

    private int scorePct(Mission mission, int score) {
        int maxScore = (mission != null && mission.getMaxScore() != null && mission.getMaxScore() > 0)
                ? mission.getMaxScore() : 100;
        int pct = (int) Math.round((double) score / maxScore * 100.0);
        return Math.max(0, Math.min(100, pct));
    }

    private int clampScore(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(0, Math.min(100, value));
    }

    // ---------- recommendations (AI-first, rule-based fallback) ----------

    private void generateRecommendationsForStudent(Student student, Mission mission) {
        List<StudentSkill> skills = studentSkillRepository.findStudentSkillsByStudentId(student.getId());
        LearningStyle learningStyle = student.getLearningStyle();
        List<MissionSession> completed = missionSessionRepository.findMissionSessionsByStudentId(student.getId())
                .stream().filter(s -> s.getStatus() == MissionSessionStatus.COMPLETED).toList();
        List<Insight> recentInsights = new ArrayList<>();
        for (MissionSession s : completed) {
            Insight in = insightRepository.findInsightByMissionSessionId(s.getId());
            if (in != null) {
                recentInsights.add(in);
            }
        }

        List<RecommendationAiItem> aiItems = aiService.generateRecommendations(student, recentInsights, skills,
                learningStyle, completed);
        if (aiItems != null && !aiItems.isEmpty()) {
            for (RecommendationAiItem item : aiItems) {
                if (item == null || item.getTitle() == null || item.getTitle().isBlank()) {
                    continue;
                }
                RecommendationType type = item.getType() != null ? item.getType() : RecommendationType.MISSION;
                RecommendationPriority priority = item.getPriority() != null
                        ? item.getPriority() : RecommendationPriority.MEDIUM;
                recommendationService.recordAiRecommendation(student, mission, item.getTitle(),
                        item.getDescription(), type, priority,
                        "AI-generated recommendation based on the student's recent missions.");
            }
            return;
        }

        // Fallback: deterministic mission-driven recommendation (reuses the existing helper).
        if (mission != null) {
            String skillName = mission.getSkill() != null ? mission.getSkill().getName() : "your skills";
            recommendationService.recordMissionRecommendation(student, mission, mission.getSkill(),
                    "Keep building " + skillName,
                    "Try another mission to keep improving after completing: " + mission.getTitle(),
                    "Generated automatically after completing mission: " + mission.getTitle());
        }
    }

    private InsightOutDTO toInsightOut(Insight insight) {
        if (insight == null) {
            return null;
        }
        InsightOutDTO out = modelMapper.map(insight, InsightOutDTO.class);
        if (insight.getMissionSession() != null) {
            out.setMissionSessionId(insight.getMissionSession().getId());
        }
        return out;
    }

    private void applyRelationships(MissionSession missionSession, MissionSessionInDTO dto) {
        Mission mission = missionRepository.findMissionById(dto.getMissionId());
        if (mission == null) {
            throw new ApiException("Mission with id " + dto.getMissionId() + " not found");
        }
        missionSession.setMission(mission);
    }

    private MissionSessionOutDTO toOut(MissionSession missionSession) {
        return modelMapper.map(missionSession, MissionSessionOutDTO.class);
    }
}
