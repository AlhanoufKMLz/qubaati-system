package com.example.qubaatisystem.Service;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.AiGenerateActivityInDTO;
import com.example.qubaatisystem.DTO.Out.ActivityDetailsOutDTO;
import com.example.qubaatisystem.DTO.Out.ActivitySubmissionOutDTO;
import com.example.qubaatisystem.DTO.Out.OptionDetailsOutDTO;
import com.example.qubaatisystem.DTO.Out.QuestionDetailsOutDTO;
import com.example.qubaatisystem.Enum.ActivityStatus;
import com.example.qubaatisystem.Enum.QuestionType;
import com.example.qubaatisystem.Model.Activity;
import com.example.qubaatisystem.Model.ActivitySubmission;
import com.example.qubaatisystem.Model.Option;
import com.example.qubaatisystem.Model.Question;
import com.example.qubaatisystem.Model.Teacher;
import com.example.qubaatisystem.Repository.ActivityRepository;
import com.example.qubaatisystem.Repository.ActivitySubmissionRepository;
import com.example.qubaatisystem.Repository.OptionRepository;
import com.example.qubaatisystem.Repository.QuestionRepository;
import com.example.qubaatisystem.Repository.TeacherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * AI feature area: activity generation/refinement and submission evaluation/feedback.
 *
 * <p>Uses the OpenAI Chat API via Spring AI {@code ChatClient} (config keys {@code spring.ai.openai.api-key}
 * / {@code spring.ai.openai.chat.options.model}) — the same Spring AI configuration as the mission AiService.
 *
 * <p><b>Explicit AI endpoints fail fast.</b> {@code generateActivity}, {@code refineActivity} and
 * {@code generateFeedback} require a configured provider and a real AI response — they NEVER silently return
 * placeholder/template content. If the provider is unconfigured or the call/parse fails, a clear
 * {@link ApiException} is thrown so the caller knows the AI was unavailable.
 *
 * <p><b>Canonical storage vs. localized response:</b> generated/refined content and feedback are produced and
 * SAVED in canonical <b>English</b>. The {@code language} request param (default <b>en</b>) only affects the
 * RESPONSE: when {@code language=ar}, user-facing text is localized to Arabic for the response without
 * overwriting the stored English. Enums, ids, timestamps, scores, statuses, types, difficulty, points and
 * isCorrect are never translated.
 */
@Slf4j
@Service
public class AiActivityService {

    private final ActivityRepository activityRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final ActivitySubmissionRepository activitySubmissionRepository;
    private final ActivitySubmissionService activitySubmissionService;
    private final ActivityService activityService;
    private final TeacherRepository teacherRepository;
    private final AiProviderHealthService aiProviderHealthService;
    private final ObjectMapper objectMapper;

    // Spring AI client. The API key and model come from spring.ai.openai.* (OPENAI_API_KEY / OPENAI_MODEL)
    // via Spring AI auto-configuration — exactly like Student 3's AiService. No manual key/model handling.
    private final ChatClient chatClient;

    public AiActivityService(ActivityRepository activityRepository,
                             QuestionRepository questionRepository,
                             OptionRepository optionRepository,
                             ActivitySubmissionRepository activitySubmissionRepository,
                             ActivitySubmissionService activitySubmissionService,
                             ActivityService activityService,
                             TeacherRepository teacherRepository,
                             AiProviderHealthService aiProviderHealthService,
                             ObjectMapper objectMapper,
                             ChatClient.Builder chatClientBuilder) {
        this.activityRepository = activityRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.activitySubmissionRepository = activitySubmissionRepository;
        this.activitySubmissionService = activitySubmissionService;
        this.activityService = activityService;
        this.teacherRepository = teacherRepository;
        this.aiProviderHealthService = aiProviderHealthService;
        this.objectMapper = objectMapper;
        this.chatClient = chatClientBuilder.build();
    }

    // ====================== AI ENDPOINTS ======================

    /**
     * EXPLICIT AI endpoint: generates a full activity (description + real questions/options) by calling the AI
     * and parsing strict JSON. Fails fast — never returns placeholder/template content.
     */
    public ActivityDetailsOutDTO generateActivity(AiGenerateActivityInDTO dto, String language) {
        String lang = normalizeLanguage(language);

        int questionCount = dto.getQuestionCount() != null ? dto.getQuestionCount() : 3;
        if (questionCount <= 0 || questionCount > 20) {
            throw new ApiException("questionCount must be between 1 and 20");
        }

        // No silent fallback for this endpoint: require a configured provider, then actually call the AI.
        aiProviderHealthService.requireConfigured();
        GeneratedActivity generated = generateActivityViaAi(dto, questionCount);

        // Persist the AI-generated content as canonical English. cleanPlainText keeps stored text clean.
        Activity activity = new Activity();
        String title = notBlank(generated.title) ? generated.title : "Activity: " + dto.getTopic();
        String description = notBlank(generated.description)
                ? generated.description
                : "This activity helps students understand " + dto.getTopic() + ".";
        activity.setTitle(cleanPlainText(title));
        activity.setDescription(cleanPlainText(description));
        activity.setType(dto.getType());
        activity.setDifficulty(dto.getDifficulty());
        activity.setMaxScore(dto.getMaxScore());
        activity.setStatus(ActivityStatus.DRAFT);
        if (dto.getTeacherId() != null) {
            Teacher owner = teacherRepository.findTeacherById(dto.getTeacherId());
            if (owner == null) {
                throw new ApiException("Teacher with id " + dto.getTeacherId() + " not found");
            }
            activity.setCreatedByTeacher(owner);
        }
        activity.setSkill(activityService.resolveActivitySkill(dto.getSkillId(), dto.getSkillType()));
        Activity savedActivity = activityRepository.save(activity);

        // Points are distributed so they sum to maxScore (e.g. 10 over 3 -> 4,3,3); the AI supplies content.
        int[] questionPoints = distributePoints(dto.getMaxScore(), generated.questions.size());
        for (int i = 0; i < generated.questions.size(); i++) {
            GeneratedQuestion gq = generated.questions.get(i);
            Question question = new Question();
            question.setContent(cleanPlainText(gq.content));
            question.setType(QuestionType.MULTIPLE_CHOICE);
            question.setDifficulty(dto.getDifficulty());
            question.setPoints(questionPoints[i]);
            question.setCorrectAnswer(cleanPlainText(gq.correctAnswer));
            question.setActivity(savedActivity);
            Question savedQuestion = questionRepository.save(question);

            for (GeneratedOption opt : gq.options) {
                Option option = new Option();
                option.setContent(cleanPlainText(opt.content));
                option.setIsCorrect(Boolean.TRUE.equals(opt.isCorrect));
                option.setQuestion(savedQuestion);
                optionRepository.save(option);
            }
        }

        // A freshly generated activity goes straight to the review queue (DRAFT -> PENDING_REVIEW). Still not
        // assignable until a teacher APPROVES it.
        activityService.submitForReview(savedActivity.getId());
        Activity pendingActivity = activityRepository.findActivityById(savedActivity.getId());
        log.info("AI Activity Generation completed successfully. activityId={} questions={}",
                savedActivity.getId(), generated.questions.size());
        return buildActivityDetailsOutDTO(pendingActivity, lang);
    }

    /** EXPLICIT AI endpoint: refines the activity description via the AI. Fails fast — no silent fallback. */
    public ActivityDetailsOutDTO refineActivity(Integer activityId, String instruction, String language) {
        String lang = normalizeLanguage(language);

        Activity activity = activityRepository.findActivityById(activityId);
        if (activity == null) {
            throw new ApiException("Activity with id " + activityId + " not found");
        }
        if (activity.getStatus() != ActivityStatus.DRAFT
                && activity.getStatus() != ActivityStatus.REJECTED
                && activity.getStatus() != ActivityStatus.PENDING_REVIEW) {
            throw new ApiException("Only DRAFT, REJECTED or PENDING_REVIEW activities can be refined");
        }

        String effectiveInstruction = (instruction == null || instruction.isBlank())
                ? "Improve the clarity and quality of this activity while keeping the same educational level."
                : instruction;

        aiProviderHealthService.requireConfigured();
        log.info("AI Activity Refinement started. model={} configured=true activityId={}",
                aiProviderHealthService.getModel(), activityId);

        String englishSystem = "You refine educational activities. Respond in English only. " + PLAIN_TEXT_RULES;
        String englishUser = "Activity title: " + activity.getTitle()
                + ". Current description: " + (activity.getDescription() == null ? "" : activity.getDescription())
                + ". Instruction: " + effectiveInstruction
                + ". Return only the refined one-paragraph description.";

        String refinedEnglish = cleanPlainText(callOpenAiOrThrow(englishSystem, englishUser, "AI activity refinement"));
        if (refinedEnglish == null || refinedEnglish.isBlank()) {
            throw new ApiException("AI activity refinement returned empty content.");
        }

        activity.setDescription(refinedEnglish);
        activity.setStatus(ActivityStatus.PENDING_REVIEW);
        activityRepository.save(activity);
        log.info("AI Activity Refinement completed successfully. activityId={}", activityId);

        return buildActivityDetailsOutDTO(activity, lang);
    }

    /**
     * MANUAL / admin re-grade endpoint. This is a GRADING operation, not AI content generation: MULTIPLE_CHOICE
     * / TRUE_FALSE grade deterministically from stored correctness, and only free-text answers use AI grading
     * (with a deterministic comparison fallback). The grading logic lives once in
     * {@link ActivitySubmissionService#regrade}. It is therefore intentionally NOT gated on an AI key.
     */
    public ActivitySubmissionOutDTO evaluateSubmission(Integer submissionId, String language) {
        return activitySubmissionService.regrade(submissionId, language);
    }

    /**
     * Teacher/reviewer view: the full activity details INCLUDING correctAnswer and each option's isCorrect.
     * NOT student-safe — only for teacher/reviewer/admin endpoints.
     */
    public ActivityDetailsOutDTO getActivityDetails(Integer activityId, String language) {
        String lang = normalizeLanguage(language);
        Activity activity = activityRepository.findActivityById(activityId);
        if (activity == null) {
            throw new ApiException("Activity with id " + activityId + " not found");
        }
        return buildActivityDetailsOutDTO(activity, lang);
    }

    /** EXPLICIT AI endpoint: generates submission feedback via the AI. Fails fast — no silent fallback. */
    public ActivitySubmissionOutDTO generateFeedback(Integer submissionId, String audience, String language) {
        String lang = normalizeLanguage(language);
        String targetAudience = normalizeAudience(audience);

        ActivitySubmission submission = activitySubmissionRepository.findActivitySubmissionById(submissionId);
        if (submission == null) {
            throw new ApiException("ActivitySubmission with id " + submissionId + " not found");
        }

        aiProviderHealthService.requireConfigured();
        log.info("AI Feedback Generation started. model={} configured=true submissionId={} audience={}",
                aiProviderHealthService.getModel(), submissionId, targetAudience);

        String englishSystem = "You are an educational assistant. Write feedback addressed to the "
                + targetAudience + " in English. " + PLAIN_TEXT_RULES;
        String englishUser = "Submission score: " + submission.getScore() + ", status: " + submission.getStatus() + ".";
        String englishFeedback = cleanPlainText(callOpenAiOrThrow(englishSystem, englishUser, "AI feedback generation"));
        if (englishFeedback == null || englishFeedback.isBlank()) {
            throw new ApiException("AI feedback generation returned empty content.");
        }

        submission.setAiFeedback(englishFeedback);
        activitySubmissionRepository.save(submission);

        ActivitySubmissionOutDTO out = activitySubmissionService.getById(submissionId);
        if (isArabic(lang)) {
            // Translation is a localization step on REAL AI feedback; keep the English original if it is
            // unavailable (this is the genuine AI output, not fabricated fallback content).
            String arabicFeedback = cleanPlainText(translateToArabic(englishFeedback));
            out.setAiFeedback(arabicFeedback == null || arabicFeedback.isBlank() ? englishFeedback : arabicFeedback);
        }
        log.info("AI Feedback Generation completed successfully. submissionId={}", submissionId);
        return out;
    }

    // ====================== AI activity JSON generation ======================

    /**
     * Calls the AI for the full activity JSON, retrying ONCE with a stricter prompt if the first response is
     * not valid JSON, then validates the parsed content (rejecting placeholder/template text). Throws a clear
     * {@link ApiException} on provider failure, unparseable output, or placeholder content.
     */
    private GeneratedActivity generateActivityViaAi(AiGenerateActivityInDTO dto, int questionCount) {
        log.info("AI Activity Generation started. model={} configured=true topic=\"{}\" questions={}",
                aiProviderHealthService.getModel(), dto.getTopic(), questionCount);

        String raw = callOpenAiOrThrow(activitySystemPrompt(false), activityUserPrompt(dto, questionCount),
                "AI activity generation");
        GeneratedActivity parsed = tryParseActivity(raw);

        if (parsed == null) {
            log.warn("AI Activity Generation: first response was not valid JSON — retrying once with a stricter prompt.");
            String retry = callOpenAiOrThrow(activitySystemPrompt(true), activityUserPrompt(dto, questionCount),
                    "AI activity generation");
            parsed = tryParseActivity(retry);
        }

        if (parsed == null) {
            log.warn("AI Activity Generation failed: invalid JSON response after retry.");
            throw new ApiException("AI activity generation returned invalid JSON.");
        }

        validateGenerated(parsed);
        return parsed;
    }

    private String activitySystemPrompt(boolean strict) {
        String base = "You are an educational content generator for school students. Generate REAL, "
                + "grade-appropriate multiple-choice questions with genuine, specific answer options. NEVER use "
                + "placeholder phrases such as \"An accurate statement about\", \"A common misconception about\", "
                + "or \"A statement unrelated to\". Return STRICT JSON ONLY — a single JSON object, no markdown, "
                + "no code fences, and no commentary before or after the JSON.";
        if (strict) {
            base += " CRITICAL: your previous output was not valid JSON. Output MUST be exactly one valid JSON "
                    + "object and NOTHING else.";
        }
        return base;
    }

    private String activityUserPrompt(AiGenerateActivityInDTO dto, int questionCount) {
        String context = (dto.getDescription() == null || dto.getDescription().isBlank())
                ? "" : "Context: " + dto.getDescription() + ". ";
        return "Topic: " + dto.getTopic() + ". " + context
                + "Activity type: " + dto.getType() + ". Difficulty: " + dto.getDifficulty() + ". "
                + "Generate exactly " + questionCount + " multiple-choice question(s). Each question must have a "
                + "clear question 'content', a 'correctAnswer' string, and 3 to 4 'options', each option having "
                + "'content' and a boolean 'isCorrect', with EXACTLY ONE correct option. Also write a "
                + "one-sentence 'description' overview. Return JSON exactly in this shape: "
                + "{\"title\":\"...\",\"description\":\"...\",\"questions\":[{\"content\":\"...\","
                + "\"correctAnswer\":\"...\",\"options\":[{\"content\":\"...\",\"isCorrect\":true},"
                + "{\"content\":\"...\",\"isCorrect\":false}]}]}";
    }

    private GeneratedActivity tryParseActivity(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return parseActivityJson(stripJsonFences(raw));
        } catch (Exception e) {
            return null; // signals "retry with stricter prompt" / "fail clearly"
        }
    }

    @SuppressWarnings("unchecked")
    private GeneratedActivity parseActivityJson(String json) {
        Map<String, Object> root = objectMapper.readValue(json, Map.class);
        GeneratedActivity g = new GeneratedActivity();
        g.title = asString(root.get("title"));
        g.description = asString(root.get("description"));
        g.questions = new ArrayList<>();

        Object questionsObj = root.get("questions");
        if (questionsObj instanceof List<?> questionList) {
            for (Object qo : questionList) {
                if (!(qo instanceof Map)) {
                    continue;
                }
                Map<String, Object> qm = (Map<String, Object>) qo;
                GeneratedQuestion gq = new GeneratedQuestion();
                gq.content = asString(qm.get("content"));
                gq.correctAnswer = asString(qm.get("correctAnswer"));
                gq.options = new ArrayList<>();

                Object optionsObj = qm.get("options");
                if (optionsObj instanceof List<?> optionList) {
                    for (Object oo : optionList) {
                        if (!(oo instanceof Map)) {
                            continue;
                        }
                        Map<String, Object> om = (Map<String, Object>) oo;
                        GeneratedOption go = new GeneratedOption();
                        go.content = asString(om.get("content"));
                        Object ic = om.get("isCorrect");
                        go.isCorrect = (ic instanceof Boolean) ? (Boolean) ic
                                : Boolean.parseBoolean(String.valueOf(ic));
                        gq.options.add(go);
                    }
                }
                g.questions.add(gq);
            }
        }
        return g;
    }

    private static final List<String> PLACEHOLDER_MARKERS = List.of(
            "an accurate statement about",
            "a common misconception about",
            "a statement unrelated to");

    private void validateGenerated(GeneratedActivity g) {
        if (g.questions == null || g.questions.isEmpty()) {
            throw new ApiException("AI activity generation returned no questions.");
        }
        for (GeneratedQuestion q : g.questions) {
            if (q.content == null || q.content.isBlank() || q.options == null || q.options.size() < 2) {
                throw new ApiException("AI activity generation returned an incomplete question.");
            }
            boolean anyCorrect = false;
            for (GeneratedOption o : q.options) {
                if (o.content == null || o.content.isBlank()) {
                    throw new ApiException("AI activity generation returned an empty option.");
                }
                if (Boolean.TRUE.equals(o.isCorrect)) {
                    anyCorrect = true;
                }
            }
            if (!anyCorrect) {
                throw new ApiException("AI activity generation returned a question with no correct option.");
            }
        }
        if (containsPlaceholder(g)) {
            log.warn("AI Activity Generation failed: response contained placeholder/template text.");
            throw new ApiException("AI activity generation produced placeholder content; treated as a generation failure.");
        }
    }

    private boolean containsPlaceholder(GeneratedActivity g) {
        StringBuilder sb = new StringBuilder();
        if (g.description != null) {
            sb.append(g.description).append(' ');
        }
        for (GeneratedQuestion q : g.questions) {
            if (q.content != null) {
                sb.append(q.content).append(' ');
            }
            if (q.correctAnswer != null) {
                sb.append(q.correctAnswer).append(' ');
            }
            for (GeneratedOption o : q.options) {
                if (o.content != null) {
                    sb.append(o.content).append(' ');
                }
            }
        }
        String lower = sb.toString().toLowerCase(Locale.ROOT);
        for (String marker : PLACEHOLDER_MARKERS) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    /** Strips ```json / ``` fences and narrows to the first {...} object before JSON parsing. */
    private String stripJsonFences(String content) {
        if (content == null) {
            return null;
        }
        String cleaned = content.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    // ====================== detailed response building ======================

    private ActivityDetailsOutDTO buildActivityDetailsOutDTO(Activity activity, String language) {
        boolean arabic = isArabic(language);

        String title = cleanPlainText(arabic ? localizeOrKeep(activity.getTitle()) : activity.getTitle());
        String description = cleanPlainText(arabic ? localizeOrKeep(activity.getDescription()) : activity.getDescription());

        List<QuestionDetailsOutDTO> questionDtos = new ArrayList<>();
        for (Question question : questionRepository.findQuestionsByActivityId(activity.getId())) {
            String content = cleanPlainText(arabic ? localizeOrKeep(question.getContent()) : question.getContent());
            String correctAnswer = cleanPlainText(arabic ? localizeOrKeep(question.getCorrectAnswer()) : question.getCorrectAnswer());

            List<OptionDetailsOutDTO> optionDtos = new ArrayList<>();
            for (Option option : optionRepository.findOptionsByQuestionId(question.getId())) {
                String optionContent = cleanPlainText(arabic ? localizeOrKeep(option.getContent()) : option.getContent());
                optionDtos.add(new OptionDetailsOutDTO(option.getId(), optionContent, option.getIsCorrect()));
            }

            questionDtos.add(new QuestionDetailsOutDTO(
                    question.getId(),
                    content,
                    question.getType(),
                    question.getDifficulty(),
                    question.getPoints(),
                    correctAnswer,
                    optionDtos));
        }

        return new ActivityDetailsOutDTO(
                activity.getId(),
                title,
                description,
                activity.getType(),
                activity.getStatus(),
                activity.getDifficulty(),
                activity.getMaxScore(),
                activity.getCreatedAt(),
                questionDtos,
                arabic ? arabicFieldLabels() : null);
    }

    /**
     * Translates English text to Arabic for the response. If translation is unavailable the stored canonical
     * English is returned (this is a localization step, not a content-generation fallback).
     */
    private String localizeOrKeep(String englishText) {
        String arabic = translateToArabic(englishText);
        return arabic != null ? arabic : englishText;
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }
        String normalized = language.trim().toLowerCase();
        if (!normalized.equals("en") && !normalized.equals("ar")) {
            throw new ApiException("Language must be either en or ar");
        }
        return normalized;
    }

    private boolean isArabic(String language) {
        return "ar".equals(normalizeLanguage(language));
    }

    private String normalizeAudience(String audience) {
        if (audience == null || audience.isBlank()) {
            return "student";
        }
        String normalized = audience.trim().toLowerCase();
        if (!normalized.equals("student") && !normalized.equals("teacher") && !normalized.equals("parent")) {
            throw new ApiException("Audience must be student, teacher, or parent");
        }
        return normalized;
    }

    /**
     * Translates English text to Arabic via OpenAI. Returns {@code null} when the call fails so callers keep the
     * English original (a localization fallback — NOT fabricated content). Blank input is returned unchanged.
     */
    private String translateToArabic(String englishText) {
        if (englishText == null || englishText.isBlank()) {
            return englishText;
        }
        return callOpenAiQuietly(
                "You are a professional translator. Translate the value only into Modern Standard Arabic. "
                        + "Return plain text only. Do not add explanations. Do not add suggestions. "
                        + "Do not add markdown. Do not add line breaks. Return only the translation.",
                englishText);
    }

    /**
     * Spring AI {@code ChatClient} call for EXPLICIT endpoints — throws a clear {@link ApiException} on failure
     * (never returns silent fallback). {@code op} labels the operation in the error/logs; credentials are never
     * read or logged here (Spring AI supplies the key/model from config).
     */
    private String callOpenAiOrThrow(String systemPrompt, String userPrompt, String op) {
        try {
            return chatClient.prompt().system(systemPrompt).user(userPrompt).call().content();
        } catch (Exception e) {
            log.warn("{} failed: provider call error: {}", op, e.getMessage());
            throw new ApiException(op + " failed: " + safeReason(e));
        }
    }

    /**
     * Spring AI {@code ChatClient} call for the localization (translate) step only. Returns {@code null} on
     * failure so the caller keeps the real English text — used by {@link #translateToArabic}.
     */
    private String callOpenAiQuietly(String systemPrompt, String userPrompt) {
        try {
            return chatClient.prompt().system(systemPrompt).user(userPrompt).call().content();
        } catch (Exception e) {
            log.warn("AI translation call failed (keeping English): {}", e.getMessage());
            return null;
        }
    }

    private String safeReason(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }

    // ====================== plain-text cleaning ======================

    private static final String PLAIN_TEXT_RULES =
            "The description must be plain text only. Do not use markdown. Do not use bullet points. "
                    + "Do not use line breaks. Do not include phrases like \"Here is\", \"This is\", "
                    + "\"If you want\", \"I can also\". Do not explain what you changed. Return only the activity data.";

    private static final Pattern LINE_BREAKS = Pattern.compile("[\\r\\n\\t\\f\\u000B]+");
    private static final Pattern BULLETS = Pattern.compile("(?:^|\\s)[-•·▪◦‣*]+\\s+");
    private static final Pattern LIST_NUMBERS = Pattern.compile("(?:^|\\s)\\d+[.)]\\s+");
    private static final Pattern MARKDOWN_CHARS = Pattern.compile("[*`#]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");
    private static final Pattern EN_PREAMBLE = Pattern.compile(
            "(?i)^\\s*(?:sure|certainly|of course|okay|ok|here(?:'s| is)|this is|below is|the following is)[^:]*:\\s*");
    private static final Pattern EN_SUGGESTION = Pattern.compile(
            "(?i)\\s*(?:if you(?:'d like| want| wish)|i can also|i could also|let me know|feel free|would you like|i hope this)\\b.*$");
    private static final Pattern AR_PREAMBLE = Pattern.compile(
            "^\\s*(?:إليك|إليكم|إليكَ|هذه نسخة|هذه هي|هذا هو|فيما يلي)[^:]*:\\s*");
    private static final Pattern AR_SUGGESTION = Pattern.compile(
            "\\s*(?:إذا أردت|إذا رغبت|إن أردت|وإذا أردت|يمكنني أيضًا|يمكنني أيضا|هل تريد).*$");

    private String cleanPlainText(String value) {
        if (value == null) {
            return null;
        }
        String text = LINE_BREAKS.matcher(value).replaceAll(" ");
        text = text.replace("**", "").replace("__", "");
        text = BULLETS.matcher(text).replaceAll(" ");
        text = LIST_NUMBERS.matcher(text).replaceAll(" ");
        text = MARKDOWN_CHARS.matcher(text).replaceAll("");
        text = EN_PREAMBLE.matcher(text).replaceAll("");
        text = AR_PREAMBLE.matcher(text).replaceAll("");
        text = EN_SUGGESTION.matcher(text).replaceAll("");
        text = AR_SUGGESTION.matcher(text).replaceAll("");
        text = MULTI_SPACE.matcher(text).replaceAll(" ").trim();
        return text;
    }

    private Map<String, String> arabicFieldLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("id", "المعرّف");
        labels.put("title", "العنوان");
        labels.put("description", "الوصف");
        labels.put("type", "نوع النشاط");
        labels.put("status", "الحالة");
        labels.put("difficulty", "الصعوبة");
        labels.put("maxScore", "الدرجة الكاملة");
        labels.put("createdAt", "تاريخ الإنشاء");
        labels.put("questions", "الأسئلة");
        labels.put("content", "نص السؤال");
        labels.put("points", "النقاط");
        labels.put("correctAnswer", "الإجابة الصحيحة");
        labels.put("options", "الخيارات");
        labels.put("isCorrect", "هل الإجابة صحيحة");
        return labels;
    }

    // ====================== misc helpers + parse holders ======================

    /**
     * Distributes the activity's maxScore across {@code count} questions so the per-question points sum to
     * maxScore (e.g. 10 over 3 -> [4, 3, 3]); each question is guaranteed at least 1 point.
     */
    private int[] distributePoints(Integer maxScore, int count) {
        int[] points = new int[count];
        if (maxScore != null && maxScore > 0) {
            int base = maxScore / count;
            int remainder = maxScore % count;
            for (int i = 0; i < count; i++) {
                points[i] = Math.max(1, base + (i < remainder ? 1 : 0));
            }
        } else {
            for (int i = 0; i < count; i++) {
                points[i] = 1;
            }
        }
        return points;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    // Plain data holders for the AI JSON (populated manually from the parsed Map — no Jackson binding).
    private static final class GeneratedActivity {
        private String title;
        private String description;
        private List<GeneratedQuestion> questions;
    }

    private static final class GeneratedQuestion {
        private String content;
        private String correctAnswer;
        private List<GeneratedOption> options;
    }

    private static final class GeneratedOption {
        private String content;
        private Boolean isCorrect;
    }
}
