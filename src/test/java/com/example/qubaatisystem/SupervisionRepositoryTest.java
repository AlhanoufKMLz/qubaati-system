package com.example.qubaatisystem;

import com.example.qubaatisystem.Enum.UserRole;
import com.example.qubaatisystem.Model.Classroom;
import com.example.qubaatisystem.Model.Parent;
import com.example.qubaatisystem.Model.Student;
import com.example.qubaatisystem.Model.Teacher;
import com.example.qubaatisystem.Model.User;
import com.example.qubaatisystem.Repository.ClassroomRepository;
import com.example.qubaatisystem.Repository.ParentRepository;
import com.example.qubaatisystem.Repository.StudentRepository;
import com.example.qubaatisystem.Repository.TeacherRepository;
import com.example.qubaatisystem.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SupervisionRepositoryTest {

    @Autowired StudentRepository studentRepository;
    @Autowired ParentRepository parentRepository;
    @Autowired TeacherRepository teacherRepository;
    @Autowired ClassroomRepository classroomRepository;
    @Autowired UserRepository userRepository;

    private Parent parent;
    private Teacher teacher;
    private Classroom classroom;

    @BeforeEach
    void setUp() {
        User parentUser  = userRepository.save(makeUser("parent_user",   "parent@test.com",  UserRole.PARENT));
        User teacherUser = userRepository.save(makeUser("teacher_user",  "teacher@test.com", UserRole.TEACHER));
        User sUser1      = userRepository.save(makeUser("student1_user", "s1@test.com",      UserRole.STUDENT));
        User sUser2      = userRepository.save(makeUser("student2_user", "s2@test.com",      UserRole.STUDENT));

        parent = parentRepository.save(makeParent("Test Parent", "0501234567", parentUser));

        teacher = new Teacher();
        teacher.setFullName("Test Teacher");
        teacher.setSpecialization("Math");
        teacher.setUser(teacherUser);
        teacher = teacherRepository.save(teacher);

        classroom = new Classroom();
        classroom.setName("Class A");
        classroom.setGradeLevel(5);
        classroom.setSection("A");
        classroom.setTeacher(teacher);
        classroom = classroomRepository.save(classroom);

        // student enrolled in classroom
        studentRepository.save(makeStudent("Student One", 10, sUser1, parent, classroom));
        // student NOT in classroom
        studentRepository.save(makeStudent("Student Two", 11, sUser2, parent, null));
    }

    // ── Repository helpers ───────────────────────────────────────────────────

    private User makeUser(String username, String email, UserRole role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword("password123");
        u.setRole(role);
        return u;
    }

    private Parent makeParent(String fullName, String phone, User user) {
        Parent p = new Parent();
        p.setFullName(fullName);
        p.setPhoneNumber(phone);
        p.setUser(user);
        return p;
    }

    private Student makeStudent(String fullName, int age, User user, Parent parent, Classroom classroom) {
        Student s = new Student();
        s.setFullName(fullName);
        s.setAge(age);
        s.setGrade("Grade 5");
        s.setTotalPoints(0);
        s.setCompletedMissionsCount(0);
        s.setUser(user);
        s.setParent(parent);
        s.setClassroom(classroom);
        return s;
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    void findParentById_returnsCorrectParent() {
        Parent found = parentRepository.findParentById(parent.getId());

        assertThat(found).isNotNull();
        assertThat(found.getFullName()).isEqualTo("Test Parent");
    }

    @Test
    void findClassroomById_returnsCorrectClassroom() {
        Classroom found = classroomRepository.findClassroomById(classroom.getId());

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Class A");
    }

    @Test
    void findByParentId_returnsAllChildrenOfThatParent() {
        List<Student> children = studentRepository.findByParentId(parent.getId());

        assertThat(children).hasSize(2);
        assertThat(children).extracting(Student::getFullName)
                .containsExactlyInAnyOrder("Student One", "Student Two");
    }

    @Test
    void findByParentId_returnsEmptyListForParentWithNoChildren() {
        User otherUser   = userRepository.save(makeUser("other_parent", "other@test.com", UserRole.PARENT));
        Parent otherParent = parentRepository.save(makeParent("Other Parent", "0599999999", otherUser));

        List<Student> children = studentRepository.findByParentId(otherParent.getId());

        assertThat(children).isEmpty();
    }

    @Test
    void findByClassroomId_returnsOnlyStudentsInThatClassroom() {
        List<Student> students = studentRepository.findByClassroomId(classroom.getId());

        assertThat(students).hasSize(1);
        assertThat(students.get(0).getFullName()).isEqualTo("Student One");
    }

    @Test
    void countByClassroomTeacherId_countsStudentsUnderTeachersClassrooms() {
        int count = studentRepository.countByClassroomTeacherId(teacher.getId());

        assertThat(count).isEqualTo(1);
    }
}
