package pl.edu.agh.iisg.to.repository;

import pl.edu.agh.iisg.to.dao.CourseDao;
import pl.edu.agh.iisg.to.dao.StudentDao;
import pl.edu.agh.iisg.to.model.Course;
import pl.edu.agh.iisg.to.model.Student;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * ZADANIA 2-5 (Repozytorium) - repozytorium studentów.
 * Pełni rolę FASADY nad DAO: serwis widzi jeden obiekt "kolekcja studentów", a repozytorium wie, że za
 * niektórymi operacjami stoi więcej niż jedno DAO (findAllByCourseName korzysta z CourseDao).
 *
 * UWAGA: repozytorium NIE otwiera transakcji - robi to warstwa serwisu (SchoolService), która wie,
 * które operacje mają być atomowe. Metody zapisujące (add) opierają się na transakcji w GenericDao.
 */
public class StudentRepository implements Repository<Student> {

    private final StudentDao studentDao;      // operacje na studentach

    private final CourseDao courseDao;        // potrzebne do findAllByCourseName (zadanie 5)

    public StudentRepository(StudentDao studentDao, CourseDao courseDao) {   // zależności wstrzykiwane przez konstruktor
        this.studentDao = studentDao;
        this.courseDao = courseDao;
    }

    /** ZADANIE 3 - delegacja do DAO. */
    @Override
    public Optional<Student> add(Student student) {
        return studentDao.save(student);
    }

    @Override
    public Optional<Student> getById(int id) {
        return studentDao.findById(id);
    }

    @Override
    public List<Student> findAll() {
        return studentDao.findAll();
    }

    /** Wyszukanie po numerze indeksu - naturalne zapytanie "kolekcji studentów", używane przez SchoolService.removeStudent(). */
    public Optional<Student> findByIndexNumber(int indexNumber) {
        return studentDao.findByIndexNumber(indexNumber);
    }

    /**
     * ZADANIE 5 - studenci zapisani na kurs o podanej nazwie.
     * Jedyne ręczne zapytanie to znalezienie kursu; listę studentów Hibernate doczytuje sam (LAZY) z tabeli
     * student_course w chwili pierwszego użycia course.studentSet() - sesja musi być wtedy otwarta.
     * Zbiór (HashSet) nie ma kolejności, więc sortujemy jak w StudentDao.findAll(), by wynik był deterministyczny.
     */
    public List<Student> findAllByCourseName(String courseName) {
        return courseDao.findByName(courseName)                                 // Optional<Course>
                .map(course -> course.studentSet().stream()                      // kurs istnieje -> jego studenci (lazy load kolekcji)
                        .sorted(Comparator.comparing(Student::lastName)          // porządek: nazwisko,
                                .thenComparing(Student::firstName)               //           imię,
                                .thenComparingInt(Student::indexNumber))          //           numer indeksu
                        .toList())                                               // niemodyfikowalna lista
                .orElseGet(List::of);                                            // brak kursu -> pusta lista, nie wyjątek
    }

    /**
     * ZADANIE 4 - usunięcie studenta (logika przeniesiona z SchoolService.removeStudent()).
     * Krok 1: student jest w relacji N:M z kursami, a stroną WŁAŚCICIELSKĄ jest Course.studentSet.
     *         Hibernate kasuje wiersze z tabeli student_course tylko wtedy, gdy student zniknie z kolekcji
     *         po stronie właścicielskiej - dlatego przechodzimy po kursach studenta i usuwamy go z każdego.
     *         Pominięcie tego kroku zostawiłoby w student_course "wiszące" wiersze (SQLite nie sprawdza FK).
     * Krok 2: session.remove(student) - oceny studenta usuwa Hibernate kaskadowo (CascadeType.REMOVE w Student.gradeSet).
     * Całość musi wykonać się w jednej transakcji (otwiera ją SchoolService.removeStudent()).
     */
    @Override
    public void remove(Student student) {
        for (Course course : student.courseSet()) {        // kursy studenta (strona odwrotna - lazy load)
            course.studentSet().remove(student);           // zmiana po stronie właścicielskiej -> DELETE FROM student_course
        }
        studentDao.remove(student);                        // DELETE FROM grade (kaskada) i DELETE FROM student przy commit
    }
}
