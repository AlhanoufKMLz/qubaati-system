# Qubaati System — Full Flow Postman Check

End-to-end Postman collection that verifies the complete activity flow: data setup → AI generation/refinement → review/approval → assignment → student submission → AI evaluation/feedback → return/reopen → extra availability/history endpoints.

Collection file: `Qubaati_System_Full_Flow_Check.postman_collection.json`

## 1. How to import
1. Open Postman → **Import** → choose `postman/Qubaati_System_Full_Flow_Check.postman_collection.json`.
2. The collection ships with all its variables (including `baseUrl`) as **collection variables**, so no separate environment is required.

## 2. Required variable
- `baseUrl` (collection variable) — default `http://localhost:8080/api/v1`. Change it only if the app runs on a different host/port/context path.

All other variables (`teacherId`, `parentId`, `studentId`, `student2Id`, `classroomId`, `careerWorldId`, `skillId`, `activityId`, `revisionActivityId`, `thirdActivityId`, `assignmentId`, `submissionId`, `questionId`, `optionId`) are **filled automatically** by the test scripts as the requests run — you don't set them by hand.

## 3. Required app setup
- **MySQL** running and reachable using the credentials in `src/main/resources/application.properties`.
- The Spring Boot app running on **`localhost:8080`** (`./mvnw spring-boot:run`, or run `QubaatiSystemApplication`). Build needs **JDK 17+**.
- **`OPENAI_API_KEY`** environment variable set if you want *real* OpenAI text/translation. The property is `openai.api.key=${OPENAI_API_KEY}` (no default), so the app may fail to start if that env var is unset — set it (any non-empty value to boot, a real key for real AI) or change the property to `${OPENAI_API_KEY:}`.

## 4. AI key note
If `OPENAI_API_KEY` is **missing/blank**, the AI endpoints still work using **deterministic placeholder/fallback** content (English canonical storage; for `language=ar` the responses fall back to stored English since live translation needs the key). With a valid key, AI text and Arabic translation are produced by OpenAI. Either way the flow passes.

## 5. Recommended run order
Run the folders **top to bottom** (use Postman's **Collection Runner** for the whole collection, or run each folder in order). Order matters because IDs created earlier are reused later:
1. **Setup / Data Injection** — creates teacher, parent, classroom, 2 students, career world, a **PROBLEM_SOLVING** skill, a **LearningStyle** for the first student, and one seeded skill-history + learning-style-history row.
2. **AI Activity Generation** — generates the main activity (auto-submitted to **PENDING_REVIEW**) and refines it (Arabic; the **instruction is sent in the request body**, not a query param).
3. **Approval Flow** — proves a non-APPROVED activity can't be assigned, then review-queue → **approve (teacherId/reviewComment in the body)** → review-history. There is **no public submit-for-review** request: AI generation already placed the activity in the review queue.
4. **Request Revision / Reject Flow** — second activity → revision; third activity → reject. Both send `teacherId`/`reviewComment` **in the body** (no submit-for-review step, since generation auto-queues).
5. **Assignment Flow** — assign the now-APPROVED activity, list assignments, extend deadline, bulk-assign the 2nd student.
6. **Student Submission Flow** — start (returns **student-safe** questions/options) → save one answer (status **SAVED**) → **incomplete submit fails with 400** → save all answers → **submit (auto-evaluates → GRADED with score + activityMaxScore + aiFeedback)** → result.
7. **AI Evaluation and Feedback (manual / optional)** — `evaluate` and `generate-feedback` are **not** part of the normal flow (submit already grades); kept for manual re-grade / audience-specific feedback. Also has get-feedback, results, pending-grading.
8. **Teacher Feedback / Return / Reopen Flow** — add teacher-feedback (body, no status change) → return-to-student (**body** → RETURNED) → reopen (→ IN_PROGRESS, clears score/aiFeedback, keeps teacherFeedback, returns the student-safe view) → re-save answers → resubmit (→ GRADED).
9. **Extra Missing Endpoint Checks** — available career worlds, skill history, learning-style history, and an invalid-language failure check.

## 6. IDs saved automatically
- `teacherId`, `parentId`, `studentId`, `student2Id` — from the create responses (Teacher/Parent/Student return their full DTO with `id`).
- `classroomId`, `careerWorldId`, `skillId` — these create endpoints return only an `ApiResponse` message (no body id), so the collection issues a follow-up `GET` list request and saves the id by matching the unique name/title.
- `activityId`, `revisionActivityId`, `thirdActivityId` — from the AI generate response (`ActivityDetailsOutDTO.id`).
- `questionId`, `optionId` — from the **nested** `questions[0]` / its correct option in the AI generate response.
- `assignmentId` — from the assign-to-student response (`ActivityAssignmentOutDTO.id`).
- `submissionId` — from the start-assignment response (`StudentActivityAttemptOutDTO.submissionId`).
- `questionId1`–`questionId3`, `optionId1`–`optionId3` — from the start-assignment **student-safe** `questions[]` (each question's `questionId` and its first `options[0].optionId`). These build the answer-batch requests.

## 7. Security note
**Security is NOT enabled.** There is no login/token. Every actor id is passed **explicitly** — `assignedByTeacherId`/`teacherId` in bodies or query params, `studentId` as a query param on `start`, etc. When auth is added later, these explicit ids will be replaced by the authenticated principal.

## 8. Expected failures (these are correct, tested as 400)
- **Assigning a DRAFT activity** (`Approval Flow → Assign DRAFT activity`) → **400**: an activity must be `APPROVED` before assignment.
- **Invalid language** (`Extra → Invalid language fails`) → **400**: AI endpoints accept only `en`/`ar`; `language=fr` throws `ApiException("Language must be either en or ar")`.

All errors in this project are returned as HTTP **400** with `{"message": "..."}` (the global `ControllerAdvice` maps everything to 400).

## 9. Endpoints that may return empty lists
- `GET /students/{studentId}/skills/history` and `GET /students/{studentId}/learning-style/history` return rows from both the seeded Setup data **and** the **automatic** updates written after grading (see §12). `changedAt` is **server-assigned** (`LocalDateTime.now()`), never taken from the request body. Run standalone without Setup → expect empty arrays.
- `GET /teachers/{teacherId}/activity-submissions/pending-grading` returns only **SUBMITTED** submissions. Because **submit now auto-evaluates to GRADED**, the submission does not linger in pending-grading — this is expected.

## 10. Data / DTO notes
- `Classroom.gradeLevel` is an **integer** (`6`), not a string — the DTO requires `@NotNull @Positive Integer`.
- `Student.grade` is a **string** (`"Grade 6"`).
- Future dates (`dueDate`) use fixed ISO values in 2027 so they remain valid `@Future` values during the project window.
- Realistic names are used throughout (Noura Alharbi, Faisal Alqahtani, Sarah Alqahtani, Lina Alqahtani, "Grade 6 Physics Explorers", "Speed, Distance, and Time for Grade 6").

## 11. API design notes (recent changes)
- **AI refine instruction is in the body.** `POST /ai/activities/{activityId}/refine?language=ar` with body `{"instruction":"..."}`. The instruction is **optional** — an empty body `{}` or no body at all is valid (a safe default instruction is applied). It is no longer a query parameter.
- **Review comments are in the body.** `approve`, `reject`, and `request-revision` take `{"teacherId":..,"reviewComment":".."}`. `teacherId` is required; `reviewComment` is optional and defaults to `"Approved"` / `"Rejected"` / `"Revision requested"` respectively.
- **submit-for-review was removed from the public API.** AI generation now **automatically** sends the new activity to review, so its status is **PENDING_REVIEW** straight after `generate` (not DRAFT). It is still not assignable until **APPROVED**. (`ActivityService.submitForReview(...)` remains an internal helper.)
- **Refine returns the activity to PENDING_REVIEW.** Refine is allowed for DRAFT/REJECTED/PENDING_REVIEW and rejected for APPROVED/ARCHIVED.
- **Start assignment returns student-safe content.** The response (`StudentActivityAttemptOutDTO`) includes the activity info and each question with its options, but **never** `correctAnswer` nor which option `isCorrect`.
- **Submission requires all questions answered.** Submitting before answering every question fails with **400** and lists the missing question ids. Submit is only allowed from IN_PROGRESS (a RETURNED submission must be reopened first).
- **Evaluation uses the activity's total questions** (not the number of submitted answers) and reports the score out of the activity's `maxScore`, e.g. `Total questions: 3. Answered: 3/3. Correct: 1/3. Score: 3/10.`
- **`ActivitySubmissionOutDTO` now includes `activityMaxScore`** so the frontend can show `score / activityMaxScore`.

## 12. API design notes (this batch)
- **return-to-student uses a body.** `PATCH /activity-submissions/{id}/return-to-student` with `{"teacherId":..,"teacherFeedback":".."}`. `teacherId` required; `teacherFeedback` optional (defaults to *"Please review your answers and try again."*). No more free-text query params anywhere (refine/approve/reject/request-revision/return were all moved to bodies).
- **Submit auto-evaluates.** `POST /activity-submissions/{id}/submit?language=en` validates all questions are answered, marks answers SUBMITTED, grades, computes the score out of `maxScore`, writes `aiFeedback`, sets **GRADED**, and returns the full DTO. The **normal flow no longer calls** the public AI evaluate endpoint.
- **Public `evaluate` / `generate-feedback` remain but are manual/optional.** `POST /ai/activity-submissions/{id}/evaluate` is a manual **re-grade** (delegates to the same single grading method; allowed on SUBMITTED/GRADED). `generate-feedback` regenerates audience-specific feedback. Neither is required by the normal flow; both live in folder 7 labelled *manual / optional*. Grading logic is **not** duplicated — it lives once in `ActivitySubmissionService`.
- **Answer status lifecycle:** save → **SAVED**; submit → **SUBMITTED**; evaluation → **CORRECT/INCORRECT** (per answer). Saved draft answers are SAVED so the student can still change them before final submit.
- **Teacher full activity details:** `GET /activities/{id}/details?language=en` → `ActivityDetailsOutDTO` **including** `correctAnswer` and each option's `isCorrect`. This is a teacher/reviewer view and is intentionally NOT student-safe (the student start/reopen views hide those).
- **Teacher feedback endpoint:** `PATCH /activity-submissions/{id}/teacher-feedback` with `{"teacherId":..,"teacherFeedback":".."}` (both required). Adds feedback only — it does **not** change status. Allowed on SUBMITTED/GRADED/RETURNED.
- **Reopen enables resubmission:** `PATCH /activity-submissions/{id}/reopen` (only on RETURNED) sets IN_PROGRESS, clears `submittedAt`/`score`/`aiFeedback`, **keeps** `teacherFeedback`, resets each answer to `earnedPoints=null` + status `SAVED`, and returns the student-safe `StudentActivityAttemptOutDTO` (no correct answers). The student can then re-save and resubmit.
- **Skill / learning-style history `changedAt` is server-assigned** and any client value is ignored. **Automatic, heuristic, documented updates after grading:** (1) skill progress is recorded against the first `PROBLEM_SOLVING` skill (no Activity→Skill mapping exists yet) using the activity percentage as a 0–100 score and a 1–5 level band — **skipped** (no fake data) if no such skill exists; (2) if the student has a `LearningStyle` record and completed a `QUIZ`, its confidence is nudged +0.05 (capped at 1.0) and a history row is written — **skipped** if there is no learning-style record (full detection needs mission/session behavioural signals). Auto rows have a `reason` containing *"automatically"*.
