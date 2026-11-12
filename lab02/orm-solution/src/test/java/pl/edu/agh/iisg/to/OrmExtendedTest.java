package pl.edu.agh.iisg.to;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.edu.agh.iisg.to.dao.CourseDao;
import pl.edu.agh.iisg.to.dao.GradeDao;
import pl.edu.agh.iisg.to.dao.StudentDao;
import pl.edu.agh.iisg.to.model.Grade;
import pl.edu.agh.iisg.to.model.Student;
import pl.edu.agh.iisg.to.repository.StudentRepository;
import pl.edu.agh.iisg.to.service.SchoolService;
import pl.edu.agh.iisg.to.session.SessionService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testy uzupełniające:
 * filtrowanie raportu po kursie, kaskadowe usuwanie ocen, deterministyczne sortowanie, stabilność hashCode ocen,
 * poprawne kolumny tabeli łączącej oraz wartości zwracane dla przypadków brzegowych.
 */
public class OrmExtendedTest {

    private final SessionService sessionService = new SessionService();

    private final StudentDao studentDao = new StudentDao(sessionService);

    private final CourseDao courseDao = new CourseDao(sessionService);

    private final GradeDao gradeDao = new GradeDao(sessionService);

    private final StudentRepository studentRepository = new StudentRepository(studentDao, courseDao);

    private final SchoolService schoolService = new SchoolService(sessionService, studentRepository, gradeDao);

    @BeforeEach
    public void before() {
        sessionService.openSession();
    }

    @AfterEach
    public void after() {
        sessionService.closeSession();
    }

    @Test
    public void courseReportContainsOnlyGradesFromTheRequestedCourse() {
        // Given - jeden student, dwa kursy, oceny w obu
        var student = studentDao.create("Anna", "Lis", 200001).orElseThrow();
        var databases = courseDao.create("Bazy").orElseThrow();
        var oop = courseDao.create("TO").orElseThrow();
        schoolService.enrollStudent(databases, student);
        schoolService.enrollStudent(oop, student);
        schoolService.gradeStudent(student, databases, 4.5f);
        schoolService.gradeStudent(student, databases, 3.0f);
        schoolService.gradeStudent(student, oop, 5.0f);
        sessionService.clearSessionObjects();

        // When
        var databasesReport = schoolService.getStudentGrades("Bazy");
        var oopReport = schoolService.getStudentGrades("TO");

        // Then - raport z "Bazy" NIE zawiera oceny
        assertEquals(Map.of("Anna Lis", List.of(3.0f, 4.5f)), databasesReport);
        assertEquals(Map.of("Anna Lis", List.of(5.0f)), oopReport);
    }

    @Test
    public void enrolledStudentWithoutGradesAppearsInReportWithEmptyList() {
        // Given
        var graded = studentDao.create("Jan", "Kot", 200002).orElseThrow();
        var notGraded = studentDao.create("Ewa", "Pies", 200003).orElseThrow();
        var course = courseDao.create("WDI").orElseThrow();
        schoolService.enrollStudent(course, graded);
        schoolService.enrollStudent(course, notGraded);
        schoolService.gradeStudent(graded, course, 4.0f);
        sessionService.clearSessionObjects();

        // When
        var report = schoolService.getStudentGrades("WDI");

        // Then
        assertEquals(Map.of("Jan Kot", List.of(4.0f), "Ewa Pies", List.<Float>of()), report);
    }

    @Test
    public void reportForUnknownCourseIsEmpty() {
        assertTrue(schoolService.getStudentGrades("nie ma takiego kursu").isEmpty());
    }

    @Test
    public void removingStudentRemovesTheirGradesAndEnrollments() {
        // Given - student zapisany na kurs i oceniony; obiekty odłączone od sesji (ścieżka "wczytaj z bazy i usuń")
        var student = studentDao.create("Kasia", "Kowalska", 200004).orElseThrow();
        var course = courseDao.create("MOWNIT").orElseThrow();
        schoolService.enrollStudent(course, student);
        schoolService.gradeStudent(student, course, 4.0f);
        int gradeId = student.gradeSet().iterator().next().id();
        sessionService.clearSessionObjects();

        // When
        boolean removed = schoolService.removeStudent(student.indexNumber());
        sessionService.clearSessionObjects();

        // Then
        assertTrue(removed);
        assertTrue(studentDao.findByIndexNumber(student.indexNumber()).isEmpty());
        assertTrue(gradeDao.findById(gradeId).isEmpty(), "ocena powinna zostać usunięta kaskadowo");
        var reloadedCourse = courseDao.findById(course.id()).orElseThrow();
        assertTrue(reloadedCourse.studentSet().isEmpty(), "wpis w student_course powinien zniknąć");
        assertTrue(reloadedCourse.gradeSet().isEmpty(), "kurs nie powinien mieć już ocen usuniętego studenta");
    }

    @Test
    public void removingUnknownStudentReturnsFalse() {
        assertFalse(schoolService.removeStudent(999999));
    }

    @Test
    public void studentsWithTheSameLastNameAreOrderedByFirstName() {
        // Given
        var zofia = studentDao.create("Zofia", "Nowak", 200005).orElseThrow();
        var adam = studentDao.create("Adam", "Nowak", 200006).orElseThrow();
        var jan = studentDao.create("Jan", "Abacki", 200007).orElseThrow();
        sessionService.clearSessionObjects();

        // When
        List<Student> all = studentDao.findAll();

        // Then - nazwisko, a przy równych nazwiskach imię
        assertEquals(List.of(jan, adam, zofia), all);
    }

    @Test
    public void gradeRemainsFindableInCollectionsWithinTheSameSession() {
        // Given
        var student = studentDao.create("Piotr", "Budynek", 200008).orElseThrow();
        var course = courseDao.create("TK").orElseThrow();

        // When
        schoolService.gradeStudent(student, course, 5.0f);
        Grade grade = student.gradeSet().iterator().next();

        // Then - ocena ma już id, więc jest odnajdywana w obu zbiorach
        assertTrue(grade.id() > 0);
        assertTrue(student.gradeSet().contains(grade));
        assertTrue(course.gradeSet().contains(grade));
    }

    @Test
    public void enrollmentIsStoredInJoinTableWithProperlyNamedColumns() {
        // Given - identyfikatory studenta i kursu muszą się różnić, by wykryć zamianę kolumn
        studentDao.create("Adam", "Pierwszy", 200009).orElseThrow();
        var student = studentDao.create("Jan", "Drugi", 200010).orElseThrow();
        var course = courseDao.create("Bazy").orElseThrow();
        schoolService.enrollStudent(course, student);

        // When - odczyt tabeli łączącej zapytaniem natywnym (SQL), z pominięciem mapowania
        List<Object[]> rows = sessionService.getSession()
                .createNativeQuery("SELECT student_id, course_id FROM student_course", Object[].class)
                .getResultList();

        // Then - w kolumnie student_id jest id STUDENTA, a w course_id id KURSU
        assertEquals(1, rows.size());
        assertEquals(student.id(), ((Number) rows.get(0)[0]).intValue());
        assertEquals(course.id(), ((Number) rows.get(0)[1]).intValue());
    }
}
