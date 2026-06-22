package com.example.qubaatisystem.Security;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.Enum.UserRole;
import com.example.qubaatisystem.Model.Activity;
import com.example.qubaatisystem.Model.Payment;
import com.example.qubaatisystem.Model.ActivityAssignment;
import com.example.qubaatisystem.Model.ActivitySubmission;
import com.example.qubaatisystem.Model.Classroom;
import com.example.qubaatisystem.Model.Notification;
import com.example.qubaatisystem.Model.Parent;
import com.example.qubaatisystem.Model.Recommendation;
import com.example.qubaatisystem.Model.Student;
import com.example.qubaatisystem.Model.StudentAnswer;
import com.example.qubaatisystem.Model.Teacher;
import com.example.qubaatisystem.Model.User;
import com.example.qubaatisystem.Repository.ActivityRepository;
import com.example.qubaatisystem.Repository.ActivitySubmissionRepository;
import com.example.qubaatisystem.Repository.ClassroomRepository;
import com.example.qubaatisystem.Repository.NotificationRepository;
import com.example.qubaatisystem.Repository.ParentRepository;
import com.example.qubaatisystem.Repository.RecommendationRepository;
import com.example.qubaatisystem.Repository.StudentAnswerRepository;
import com.example.qubaatisystem.Repository.StudentRepository;
import com.example.qubaatisystem.Repository.TeacherRepository;
import com.example.qubaatisystem.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Ownership / current-principal checks layered on top of the coarse role rules in {@code SecurityConfig}.
 * A PARENT may only touch their own {@code parentId}, a TEACHER their own {@code teacherId}, a STUDENT their own
 * {@code studentId}; ADMIN bypasses every check. Denials throw {@link AccessDeniedException} (mapped to HTTP 403
 * by the global advice). No Optional, no JpaRepository.findById — uses the singular project finders.
 */
@Service
@RequiredArgsConstructor
public class SecurityOwnershipService {

    private final UserRepository userRepository;
    private final ParentRepository parentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final NotificationRepository notificationRepository;
    private final ClassroomRepository classroomRepository;
    private final ActivityRepository activityRepository;
    private final ActivitySubmissionRepository activitySubmissionRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final RecommendationRepository recommendationRepository;

    // ---- current principal ----

    public Integer getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        // Fallback (e.g. principal stored as username) — resolve via the repository.
        User user = userRepository.findUserByUsername(auth.getName());
        if (user == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return user;
    }

    public boolean isAdmin() {
        return getCurrentUser().getRole() == UserRole.ADMIN;
    }

    public Integer getCurrentParentId() {
        Parent parent = parentRepository.findParentByUserId(getCurrentUser().getId());
        if (parent == null) {
            throw new AccessDeniedException("Current user is not a parent");
        }
        return parent.getId();
    }

    public Integer getCurrentTeacherId() {
        Teacher teacher = teacherRepository.findTeacherByUserId(getCurrentUser().getId());
        if (teacher == null) {
            throw new AccessDeniedException("Current user is not a teacher");
        }
        return teacher.getId();
    }

    public Integer getCurrentStudentId() {
        Student student = studentRepository.findStudentByUserId(getCurrentUser().getId());
        if (student == null) {
            throw new AccessDeniedException("Current user is not a student");
        }
        return student.getId();
    }

    // ---- assertions (ADMIN always allowed) ----

    public void assertAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Admin privileges are required for this operation");
        }
    }

    /**
     * Resolves the owning teacher for a teacher-owned resource (e.g. a classroom or activity) from the
     * authenticated principal: a TEACHER always owns it themselves (any {@code requestedTeacherId} in the body is
     * ignored — they can never set another teacher); an ADMIN may act on a teacher's behalf but must supply the
     * teacherId. Any other role is forbidden.
     */
    public Integer resolveOwningTeacherId(Integer requestedTeacherId) {
        User current = getCurrentUser();
        if (current.getRole() == UserRole.TEACHER) {
            return getCurrentTeacherId();
        }
        if (current.getRole() == UserRole.ADMIN) {
            if (requestedTeacherId == null) {
                throw new ApiException("An admin must specify teacherId for this operation");
            }
            return requestedTeacherId;
        }
        throw new AccessDeniedException("Only a teacher or admin may perform this operation");
    }

    /** The authenticated user must own this Payment through their Parent or Teacher record (ADMIN bypasses). */
    public void assertCurrentOwnsPaymentOrAdmin(Payment payment) {
        if (payment.getParent() != null) {
            assertCurrentParentOrAdmin(payment.getParent().getId());
        } else if (payment.getTeacher() != null) {
            assertCurrentTeacherOrAdmin(payment.getTeacher().getId());
        } else {
            throw new ApiException("Payment has no subscriber owner");
        }
    }

    public void assertCurrentParentOrAdmin(Integer parentId) {
        if (isAdmin()) {
            return;
        }
        Parent parent = parentRepository.findParentByUserId(getCurrentUser().getId());
        if (parent == null || parentId == null || !parent.getId().equals(parentId)) {
            throw new AccessDeniedException("You may only access your own parent data");
        }
    }

    public void assertCurrentTeacherOrAdmin(Integer teacherId) {
        if (isAdmin()) {
            return;
        }
        Teacher teacher = teacherRepository.findTeacherByUserId(getCurrentUser().getId());
        if (teacher == null || teacherId == null || !teacher.getId().equals(teacherId)) {
            throw new AccessDeniedException("You may only access your own teacher data");
        }
    }

    public void assertCurrentStudentOrAdmin(Integer studentId) {
        if (isAdmin()) {
            return;
        }
        Student student = studentRepository.findStudentByUserId(getCurrentUser().getId());
        if (student == null || studentId == null || !student.getId().equals(studentId)) {
            throw new AccessDeniedException("You may only access your own student data");
        }
    }

    /** A parent may only act on a child that belongs to them (ADMIN bypasses). */
    public void assertParentOwnsStudentOrAdmin(Integer parentId, Integer studentId) {
        assertCurrentParentOrAdmin(parentId);
        if (isAdmin()) {
            return;
        }
        Student student = studentRepository.findStudentById(studentId);
        if (student == null || student.getParent() == null || !student.getParent().getId().equals(parentId)) {
            throw new AccessDeniedException("That child does not belong to this parent");
        }
    }

    /** Convenience: the CURRENT parent (from Basic Auth) must own the given child (ADMIN bypasses). */
    public void assertCurrentParentOwnsChildOrAdmin(Integer studentId) {
        if (isAdmin()) {
            return;
        }
        assertParentOwnsStudentOrAdmin(getCurrentParentId(), studentId);
    }

    /** The given userId must be the authenticated user (ADMIN bypasses) — userId ownership, not profile id. */
    public void assertCurrentUserOrAdmin(Integer userId) {
        if (isAdmin()) {
            return;
        }
        if (userId == null || !getCurrentUser().getId().equals(userId)) {
            throw new AccessDeniedException("You may only access your own user data");
        }
    }

    /** The notification's recipient must be the authenticated user (ADMIN bypasses). */
    public void assertCurrentUserOwnsNotificationOrAdmin(Integer notificationId) {
        if (isAdmin()) {
            return;
        }
        Notification notification = notificationRepository.findNotificationById(notificationId);
        if (notification == null || notification.getRecipient() == null
                || !notification.getRecipient().getId().equals(getCurrentUser().getId())) {
            throw new AccessDeniedException("That notification does not belong to you");
        }
    }

    /** The classroom must be owned by the current teacher (ADMIN bypasses). */
    public void assertCurrentTeacherOwnsClassroomOrAdmin(Integer classroomId) {
        if (isAdmin()) {
            return;
        }
        Integer teacherId = getCurrentTeacherId();
        Classroom classroom = classroomRepository.findClassroomById(classroomId);
        if (classroom == null || classroom.getTeacher() == null || !classroom.getTeacher().getId().equals(teacherId)) {
            throw new AccessDeniedException("That classroom does not belong to you");
        }
    }

    /** The activity must be owned (createdByTeacher) by the current teacher (ADMIN bypasses). */
    public void assertCurrentTeacherOwnsActivityOrAdmin(Integer activityId) {
        if (isAdmin()) {
            return;
        }
        Integer teacherId = getCurrentTeacherId();
        Activity activity = activityRepository.findActivityById(activityId);
        if (activity == null || activity.getCreatedByTeacher() == null
                || !activity.getCreatedByTeacher().getId().equals(teacherId)) {
            throw new AccessDeniedException("That activity does not belong to you");
        }
    }

    /**
     * The submission must belong to a teacher's activity/classroom (ADMIN bypasses). Ownership matches if the
     * current teacher is the activity's createdByTeacher, the assignment's classroom teacher, OR the
     * assignedByTeacher — accepting whichever relation is present.
     */
    public void assertCurrentTeacherOwnsSubmissionOrAdmin(Integer submissionId) {
        if (isAdmin()) {
            return;
        }
        Integer teacherId = getCurrentTeacherId();
        ActivitySubmission submission = activitySubmissionRepository.findActivitySubmissionById(submissionId);
        if (submission == null || submission.getActivityAssignment() == null) {
            throw new AccessDeniedException("That submission does not belong to you");
        }
        ActivityAssignment assignment = submission.getActivityAssignment();
        Teacher activityOwner = (assignment.getActivity() != null) ? assignment.getActivity().getCreatedByTeacher() : null;
        Teacher classroomTeacher = (assignment.getClassroom() != null) ? assignment.getClassroom().getTeacher() : null;
        boolean owns = teacherMatches(activityOwner, teacherId)
                || teacherMatches(classroomTeacher, teacherId)
                || teacherMatches(assignment.getAssignedByTeacher(), teacherId);
        if (!owns) {
            throw new AccessDeniedException("That submission does not belong to you");
        }
    }

    /** Manual-grade path: the answer's submission must belong to the current teacher (ADMIN bypasses). */
    public void assertCurrentTeacherOwnsAnswerSubmissionOrAdmin(Integer answerId) {
        if (isAdmin()) {
            return;
        }
        StudentAnswer answer = studentAnswerRepository.findStudentAnswerById(answerId);
        if (answer == null || answer.getActivitySubmission() == null) {
            throw new AccessDeniedException("That answer does not belong to you");
        }
        assertCurrentTeacherOwnsSubmissionOrAdmin(answer.getActivitySubmission().getId());
    }

    /** Recommendation accept/dismiss/complete: the recommendation must belong to the current student (ADMIN bypasses). */
    public void assertCurrentStudentOwnsRecommendationOrAdmin(Integer recommendationId) {
        if (isAdmin()) {
            return;
        }
        Integer studentId = getCurrentStudentId();
        Recommendation recommendation = recommendationRepository.findRecommendationById(recommendationId);
        if (recommendation == null || recommendation.getStudent() == null
                || !recommendation.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("That recommendation does not belong to you");
        }
    }

    /**
     * The current teacher may assign to a student only if that student is in one of the teacher's classrooms
     * (Student → classroom → teacher). ADMIN bypasses. If the student has no classroom link, a non-admin teacher
     * is denied (the strongest available check given the model).
     */
    public void assertCurrentTeacherCanAssignToStudentOrAdmin(Integer studentId) {
        if (isAdmin()) {
            return;
        }
        Integer teacherId = getCurrentTeacherId();
        Student student = studentRepository.findStudentById(studentId);
        if (student == null || student.getClassroom() == null || student.getClassroom().getTeacher() == null
                || !student.getClassroom().getTeacher().getId().equals(teacherId)) {
            throw new AccessDeniedException("You may only assign to students in your own classroom");
        }
    }

    private boolean teacherMatches(Teacher teacher, Integer teacherId) {
        return teacher != null && teacher.getId() != null && teacher.getId().equals(teacherId);
    }
}
