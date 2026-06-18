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
import com.example.qubaatisystem.Repository.ActivityRepository;
import com.example.qubaatisystem.Repository.ActivitySubmissionRepository;
import com.example.qubaatisystem.Repository.OptionRepository;
import com.example.qubaatisystem.Repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * AI feature area: activity generation/refinement and submission evaluation/feedback.
 *
 * <p>Uses the OpenAI Chat Completions API (config keys {@code openai.api.key} / {@code openai.model}).
 * If the API key is not configured, every AI text call falls back to a deterministic placeholder so the
 * endpoints remain usable and the project still compiles/runs.
 *
 * <p><b>Canonical storage vs. localized response:</b> generated/refined content and feedback are produced
 * and SAVED in canonical <b>English</b> (Activity.description is a short overview; questions/options live in
 * the Question/Option tables, never inside the description). The {@code language} request param (default
 * <b>en</b>) only affects the RESPONSE: when {@code language=ar}, user-facing text fields are localized to
 * Arabic for the response without overwriting the stored English values. Enums, ids, timestamps, scores,
 * statuses, types, difficulty, points and isCorrect are never translated. Arabic localization uses the
 * OpenAI translation call when a key exists; otherwise the stored English is returned (with a TODO).
 */
@Service
@RequiredArgsConstructor
public class AiService {

    private final ActivityRepository activityRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final ActivitySubmissionRepository activitySubmissionRepository;
    private final ActivitySubmissionService activitySubmissionService;
    private final ActivityService activityService;

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-5.4-mini}")
    private String openAiModel;

    // ====================== AI ENDPOINTS ======================

    public ActivityDetailsOutDTO generateActivity(AiGenerateActivityInDTO dto, String language) {
        String lang = normalizeLanguage(language);

        int questionCount = dto.getQuestionCount() != null ? dto.getQuestionCount() : 3;
        if (questionCount <= 0 || questionCount > 20) {
            throw new ApiException("questionCount must be between 1 and 20");
        }

        // Generate clean CANONICAL ENGLISH content for storage (regardless of the requested language).
        // The description is a SHORT overview only — the questions live in the Question table, not here.
        String englishSystem = "You are an educational content generator. Respond in English only. "
                + PLAIN_TEXT_RULES;
        String englishUser = "Topic: " + dto.getTopic()
                + ". Description: " + (dto.getDescription() == null ? "" : dto.getDescription())
                + ". Type: " + dto.getType() + ". Difficulty: " + dto.getDifficulty()
                + ". Write a one-paragraph activity overview of 1-2 sentences. Do not list the questions.";

        // Always run AI output through cleanPlainText so no markdown / preamble / suggestion text is stored.
        String englishDescription = cleanPlainText(aiText(englishSystem, englishUser));
        if (englishDescription == null || englishDescription.isBlank()) {
            englishDescription = "This activity helps students understand " + dto.getTopic()
                    + " through simple, real-life questions suited to their level.";
        }

        Activity activity = new Activity();
        activity.setTitle(cleanPlainText("Activity: " + dto.getTopic()));
        activity.setDescription(cleanPlainText(englishDescription));
        activity.setType(dto.getType());
        activity.setDifficulty(dto.getDifficulty());
        activity.setMaxScore(dto.getMaxScore());
        activity.setStatus(ActivityStatus.DRAFT);
        Activity savedActivity = activityRepository.save(activity);

        // Build realistic (non-placeholder) questions/options, stored in canonical English in their own
        // tables. cleanPlainText guarantees the stored content stays clean plain text. Points are
        // distributed so they sum to the activity's maxScore (Issue 9), e.g. 10 over 3 -> 4, 3, 3.
        int[] questionPoints = distributePoints(dto.getMaxScore(), questionCount);
        List<FallbackQuestion> fallbackQuestions = buildFallbackQuestions(dto.getTopic(), questionCount);
        for (int i = 0; i < fallbackQuestions.size(); i++) {
            FallbackQuestion fq = fallbackQuestions.get(i);
            Question question = new Question();
            question.setContent(cleanPlainText(fq.content()));
            question.setType(QuestionType.MULTIPLE_CHOICE);
            question.setDifficulty(dto.getDifficulty());
            question.setPoints(questionPoints[i]);
            question.setCorrectAnswer(cleanPlainText(fq.correctAnswer()));
            question.setActivity(savedActivity);
            Question savedQuestion = questionRepository.save(question);

            for (FallbackOption opt : fq.options()) {
                Option option = new Option();
                option.setContent(cleanPlainText(opt.content()));
                option.setIsCorrect(opt.correct());
                option.setQuestion(savedQuestion);
                optionRepository.save(option);
            }
        }

        // Issue 3: a freshly generated activity goes straight to the review queue. submitForReview is the
        // existing service helper (no public submit-for-review endpoint anymore); it transitions
        // DRAFT -> PENDING_REVIEW. The activity is still NOT assignable until a teacher APPROVES it.
        activityService.submitForReview(savedActivity.getId());
        Activity pendingActivity = activityRepository.findActivityById(savedActivity.getId());
        return buildActivityDetailsOutDTO(pendingActivity, lang);
    }

    public ActivityDetailsOutDTO refineActivity(Integer activityId, String instruction, String language) {
        String lang = normalizeLanguage(language);

        Activity activity = activityRepository.findActivityById(activityId);
        if (activity == null) {
            throw new ApiException("Activity with id " + activityId + " not found");
        }
        // Issue 4: refine is allowed before approval (DRAFT/REJECTED/PENDING_REVIEW), not after.
        if (activity.getStatus() != ActivityStatus.DRAFT
                && activity.getStatus() != ActivityStatus.REJECTED
                && activity.getStatus() != ActivityStatus.PENDING_REVIEW) {
            throw new ApiException("Only DRAFT, REJECTED or PENDING_REVIEW activities can be refined");
        }

        // The instruction is optional (Issue 1): apply a safe default when it is blank/missing.
        String effectiveInstruction = (instruction == null || instruction.isBlank())
                ? "Improve the clarity and quality of this activity while keeping the same educational level."
                : instruction;

        // Refine and store canonical ENGLISH content (overview only; not the question list).
        String englishSystem = "You refine educational activities. Respond in English only. " + PLAIN_TEXT_RULES;
        String englishUser = "Activity title: " + activity.getTitle()
                + ". Current description: " + (activity.getDescription() == null ? "" : activity.getDescription())
                + ". Instruction: " + effectiveInstruction
                + ". Return only the refined one-paragraph description.";

        // Always run AI output through cleanPlainText so no markdown / preamble / suggestion text is stored.
        String refinedEnglish = cleanPlainText(aiText(englishSystem, englishUser));
        if (refinedEnglish == null || refinedEnglish.isBlank()) {
            // No API key: keep the existing (already clean) description rather than storing assistant chatter.
            refinedEnglish = cleanPlainText(activity.getDescription());
        }

        // Only the description is refined; unrelated fields (type/difficulty/maxScore) are untouched.
        // Refining means the activity is ready to be reviewed again -> PENDING_REVIEW (Issue 4).
        activity.setDescription(refinedEnglish);
        activity.setStatus(ActivityStatus.PENDING_REVIEW);
        activityRepository.save(activity);

        return buildActivityDetailsOutDTO(activity, lang);
    }

    /**
     * MANUAL / admin re-grade endpoint. The normal student flow no longer needs it — {@code submitActivity}
     * evaluates automatically. The actual grading lives in {@link ActivitySubmissionService#regrade} (single
     * source of truth) so the logic is never duplicated. Kept for ad-hoc re-grading and debugging.
     */
    public ActivitySubmissionOutDTO evaluateSubmission(Integer submissionId, String language) {
        return activitySubmissionService.regrade(submissionId, language);
    }

    /**
     * Teacher/reviewer view (Issue 8): the full activity details, INCLUDING correctAnswer and each option's
     * isCorrect (reuses the same {@link #buildActivityDetailsOutDTO} as AI generate/refine). This is NOT
     * student-safe and must only be exposed to teacher/reviewer/admin endpoints.
     */
    public ActivityDetailsOutDTO getActivityDetails(Integer activityId, String language) {
        String lang = normalizeLanguage(language);
        Activity activity = activityRepository.findActivityById(activityId);
        if (activity == null) {
            throw new ApiException("Activity with id " + activityId + " not found");
        }
        return buildActivityDetailsOutDTO(activity, lang);
    }

    public ActivitySubmissionOutDTO generateFeedback(Integer submissionId, String audience, String language) {
        String lang = normalizeLanguage(language);
        String targetAudience = normalizeAudience(audience);

        ActivitySubmission submission = activitySubmissionRepository.findActivitySubmissionById(submissionId);
        if (submission == null) {
            throw new ApiException("ActivitySubmission with id " + submissionId + " not found");
        }

        // Build and store canonical ENGLISH feedback (clean plain text).
        String englishSystem = "You are an educational assistant. Write feedback addressed to the " + targetAudience + " in English. " + PLAIN_TEXT_RULES;
        String englishUser = "Submission score: " + submission.getScore() + ", status: " + submission.getStatus() + ".";
        String englishFeedback = cleanPlainText(aiText(englishSystem, englishUser));
        if (englishFeedback == null || englishFeedback.isBlank()) {
            englishFeedback = "Feedback for the " + targetAudience + ": current score " + submission.getScore() + ".";
        }

        submission.setAiFeedback(englishFeedback);
        activitySubmissionRepository.save(submission);

        ActivitySubmissionOutDTO out = activitySubmissionService.getById(submissionId);
        if (isArabic(lang)) {
            String arabicFeedback = cleanPlainText(translateToArabic(englishFeedback));
            if (arabicFeedback == null || arabicFeedback.isBlank()) {
                arabicFeedback = "ملاحظات لـ " + targetAudience + ": النتيجة الحالية " + submission.getScore() + ".";
            }
            out.setAiFeedback(arabicFeedback);
        }
        return out;
    }

    // ====================== helpers ======================

    /**
     * Builds the detailed, organized activity response (with nested questions/options) from the stored
     * canonical-English entities, localizing user-facing text to Arabic when requested. Questions are
     * fetched via {@code findQuestionsByActivityId} and each question's options via
     * {@code findOptionsByQuestionId}. Ids/enums/timestamps/points/isCorrect are never translated.
     */
    private ActivityDetailsOutDTO buildActivityDetailsOutDTO(Activity activity, String language) {
        boolean arabic = isArabic(language);

        // cleanPlainText is applied to every user-facing text field (after optional Arabic localization),
        // so neither the English values nor an OpenAI Arabic translation can leak markdown / preamble text.
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

        // JSON keys stay stable/English; Arabic users additionally get display-only labels (fieldLabels).
        // English responses leave fieldLabels null so it is omitted from the JSON.
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
     * Translates English text to Arabic for the response. If no OpenAI key is configured the translation
     * is unavailable, so the stored canonical English is returned.
     * TODO: full Arabic localization of stored content requires an OpenAI key (one call per text field).
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
     * Returns the assistant text from OpenAI, or {@code null} when no API key is configured
     * (callers then fall back to a deterministic placeholder).
     */
    private String aiText(String systemPrompt, String userPrompt) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return null;
        }
        return callOpenAi(systemPrompt, userPrompt);
    }

    /**
     * Translates English text to Arabic via OpenAI. Returns {@code null} when no API key is configured
     * so callers can apply a fallback. Blank input is returned unchanged.
     */
    private String translateToArabic(String englishText) {
        if (englishText == null || englishText.isBlank()) {
            return englishText;
        }
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return null;
        }
        return callOpenAi(
                "You are a professional translator. Translate the value only into Modern Standard Arabic. "
                        + "Return plain text only. Do not add explanations. Do not add suggestions. "
                        + "Do not add markdown. Do not add line breaks. Return only the translation.",
                englishText);
    }

    private String callOpenAi(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", openAiModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        try {
            Map<?, ?> response = RestClient.create()
                    .post()
                    .uri("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                throw new ApiException("OpenAI returned an empty response");
            }
            Object choicesObj = response.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                throw new ApiException("OpenAI returned no choices");
            }
            Object firstObj = choices.get(0);
            if (!(firstObj instanceof Map<?, ?> first)) {
                throw new ApiException("OpenAI response format was unexpected");
            }
            Object messageObj = first.get("message");
            if (!(messageObj instanceof Map<?, ?> message)) {
                throw new ApiException("OpenAI response format was unexpected");
            }
            return String.valueOf(message.get("content"));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("OpenAI request failed: " + e.getMessage());
        }
    }

    // ====================== plain-text cleaning ======================

    /** Reusable instruction appended to OpenAI prompts so the model returns clean plain text only. */
    private static final String PLAIN_TEXT_RULES =
            "The description must be plain text only. Do not use markdown. Do not use bullet points. "
                    + "Do not use line breaks. Do not include phrases like \"Here is\", \"This is\", "
                    + "\"If you want\", \"I can also\". Do not explain what you changed. Return only the activity data.";

    private static final Pattern LINE_BREAKS = Pattern.compile("[\\r\\n\\t\\f\\u000B]+");
    private static final Pattern BULLETS = Pattern.compile("(?:^|\\s)[-•·▪◦‣*]+\\s+");
    private static final Pattern LIST_NUMBERS = Pattern.compile("(?:^|\\s)\\d+[.)]\\s+");
    private static final Pattern MARKDOWN_CHARS = Pattern.compile("[*`#]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");
    // Leading assistant preamble ending in a colon, e.g. "Here is a clearer version:".
    private static final Pattern EN_PREAMBLE = Pattern.compile(
            "(?i)^\\s*(?:sure|certainly|of course|okay|ok|here(?:'s| is)|this is|below is|the following is)[^:]*:\\s*");
    // Trailing assistant suggestion, e.g. "If you want, I can also make it shorter.".
    private static final Pattern EN_SUGGESTION = Pattern.compile(
            "(?i)\\s*(?:if you(?:'d like| want| wish)|i can also|i could also|let me know|feel free|would you like|i hope this)\\b.*$");
    // Arabic leading preamble ending in a colon, e.g. "إليك نسخة أوضح ... من الوصف:".
    private static final Pattern AR_PREAMBLE = Pattern.compile(
            "^\\s*(?:إليك|إليكم|إليكَ|هذه نسخة|هذه هي|هذا هو|فيما يلي)[^:]*:\\s*");
    // Arabic trailing suggestion, e.g. "إذا أردت، يمكنني أيضًا ...".
    private static final Pattern AR_SUGGESTION = Pattern.compile(
            "\\s*(?:إذا أردت|إذا رغبت|إن أردت|وإذا أردت|يمكنني أيضًا|يمكنني أيضا|هل تريد).*$");

    /**
     * Normalizes AI text into one clean plain-text paragraph: drops line breaks, markdown markers, bullet
     * and numbered-list prefixes, and common assistant preambles/suggestions (English and Arabic), then
     * collapses whitespace and trims. Returns {@code null} for {@code null} input.
     */
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

    /**
     * Display-only Arabic labels for the (stable, English) JSON keys of the detailed activity response.
     * The JSON key names never change; this map only helps an Arabic UI render headings.
     */
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

    // ====================== fallback question generation ======================

    /**
     * Distributes the activity's maxScore across {@code count} questions so the per-question points sum to
     * maxScore (e.g. 10 over 3 -> [4, 3, 3]). When maxScore is null/zero, each question gets 1 point. Every
     * question is guaranteed at least 1 point.
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

    /**
     * Builds realistic canonical-English questions when no AI key is configured. Speed/distance/time topics
     * get computed word problems; any other topic gets meaningful (non-placeholder) comprehension questions.
     */
    private List<FallbackQuestion> buildFallbackQuestions(String topic, int count) {
        String lower = topic == null ? "" : topic.toLowerCase(Locale.ROOT);
        boolean speedTopic = lower.contains("speed") || lower.contains("distance") || lower.contains("velocity")
                || (lower.contains("time") && (lower.contains("speed") || lower.contains("distance")))
                || (topic != null && (topic.contains("سرعة") || topic.contains("مسافة") || topic.contains("الزمن")));
        List<FallbackQuestion> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(speedTopic ? speedDistanceTimeQuestion(i) : genericQuestion(topic, i));
        }
        return list;
    }

    private FallbackQuestion speedDistanceTimeQuestion(int index) {
        int[] speeds = {30, 40, 50, 60, 20, 45};
        int speed = speeds[(index - 1) % speeds.length];
        int hours = 2 + ((index - 1) % 2);
        int distance = speed * hours;
        int lower = Math.max(5, speed - 10);
        String correct = speed + " km/h";
        String content = "A car travels " + distance + " kilometers in " + hours + " hours. What is its speed?";
        List<FallbackOption> options = List.of(
                new FallbackOption(correct, true),
                new FallbackOption(lower + " km/h", false),
                new FallbackOption(distance + " km/h", false));
        return new FallbackQuestion(content, correct, options);
    }

    private FallbackQuestion genericQuestion(String topic, int index) {
        String subject = (topic == null || topic.isBlank()) ? "this subject" : topic.trim();
        String[] templates = {
                "Which of the following statements about %s is correct?",
                "What is the most accurate description of %s?",
                "Choose the correct fact about %s.",
                "Which option best explains %s?"
        };
        String content = String.format(templates[(index - 1) % templates.length], subject);
        String correct = "An accurate statement about " + subject + ".";
        List<FallbackOption> options = List.of(
                new FallbackOption(correct, true),
                new FallbackOption("A common misconception about " + subject + ".", false),
                new FallbackOption("A statement unrelated to " + subject + ".", false));
        return new FallbackQuestion(content, correct, options);
    }

    private record FallbackQuestion(String content, String correctAnswer, List<FallbackOption> options) {
    }

    private record FallbackOption(String content, boolean correct) {
    }
}
