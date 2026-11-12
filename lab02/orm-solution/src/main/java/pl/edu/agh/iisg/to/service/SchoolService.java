package pl.edu.agh.iisg.to.service;

import pl.edu.agh.iisg.to.dao.GradeDao;
import pl.edu.agh.iisg.to.model.Course;
import pl.edu.agh.iisg.to.model.Grade;
import pl.edu.agh.iisg.to.model.Student;
import pl.edu.agh.iisg.to.repository.StudentRepository;
import pl.edu.agh.iisg.to.session.TransactionService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Warstwa LOGIKI BIZNESOWEJ
 * Serwis modyfikuje model (obiekty i ich kolekcje) i wyznacza granice transakcji; sam nie pisze SQL -
 * zapis do bazy to zadanie Hibernate (flush przy commit) oraz DAO/repozytorium.
 *
 * Po wprowadzeniu repozytorium serwis nie zależy już od StudentDao ani CourseDao - jedynie od
 * StudentRepository (fasada) i GradeDao (zapis ocen; proste DAO może pełnić rolę repozytorium).
 */
public class SchoolService {

    private final TransactionService transactionService;   // granice transakcji - interfejs, nie konkretna SessionService

    private final StudentRepository studentRepository;     // "kolekcja studentów"

    private final GradeDao gradeDao;                       // zapis nowych ocen

    public SchoolService(TransactionService transactionService, StudentRepository studentRepository, GradeDao gradeDao) {
        this.transactionService = transactionService;
        this.studentRepository = studentRepository;
        this.gradeDao = gradeDao;
    }

    /**
     * ZADANIE 1 - zapis studenta na kurs. Zwraca false, jeśli student już jest zapisany.
     * Relacja N:M: stroną właścicielską jest Course.studentSet, więc to TA kolekcja decyduje o wierszu
     * w student_course; Student.courseSet aktualizujemy dla spójności obiektów w pamięci.
     * Zmiany trafiają do bazy dopiero przy synchronizacji kontekstu trwałości (flush), a tę wyzwala commit()
     * - stąd cała operacja jest opakowana w doAsTransaction().
     */
    public boolean enrollStudent(final Course course, final Student student) {
        return transactionService.doAsTransaction(() -> {
            if (!course.studentSet().add(student)) {       // Set.add() zwraca false, gdy element już był -> student zapisany wcześniej
                return false;                              // nic nie zmieniamy; Optional.of(false) -> orElse -> false
            }
            student.courseSet().add(course);               // druga strona relacji (tylko w pamięci - mappedBy)
            return true;                                   // commit() -> INSERT INTO student_course
        }).orElse(false);                                  // rollback (wyjątek) -> Optional.empty() -> false
    }

    /**
     * ZADANIE 3 - usunięcie studenta o podanym numerze indeksu, w JEDNEJ transakcji:
     * wyszukanie + uporządkowanie relacji N:M + remove (szczegóły w StudentRepository.remove()).
     * Zwraca true tylko gdy student istniał i został usunięty
     */
    public boolean removeStudent(final int indexNumber) {
        return transactionService.doAsTransaction(() ->
                studentRepository.findByIndexNumber(indexNumber)   // Optional<Student> - zapytanie w ramach transakcji
                        .map(student -> {                          // student istnieje:
                            studentRepository.remove(student);     //   odłączenie od kursów + remove (+ kaskada ocen)
                            return true;                           //   wynik zadania
                        })
                        .orElse(false)                             // brak studenta -> false (transakcja i tak zostanie zatwierdzona, pusta)
        ).orElse(false);                                           // wyjątek -> rollback -> false
    }

    /**
     * ZADANIE 2 - wystawienie oceny, w jednej transakcji: utworzenie Grade, zapis, dodanie do kolekcji studenta i kursu.
     * KOLEJNOŚĆ MA ZNACZENIE: najpierw gradeDao.save() (persist nadaje id - generator TABLE robi to od razu),
     * dopiero potem dodanie do HashSet-ów. Grade.hashCode() zależy od id, więc ocena musi mieć id, zanim trafi do zbiorów.
     */
    public boolean gradeStudent(final Student student, final Course course, final float gradeValue) {
        return transactionService.doAsTransaction(() -> {
            Grade grade = new Grade(student, course, gradeValue);   // TRANSIENT - Hibernate jeszcze o niej nie wie
            gradeDao.save(grade);                                   // persist w trwającej transakcji -> PERSISTENT, id nadane; wyjątek -> rollback całości
            student.gradeSet().add(grade);                          // strona odwrotna 1:N (pamięć)
            course.gradeSet().add(grade);                           // strona odwrotna 1:N (pamięć)
            return true;                                            // commit() -> INSERT INTO grade
        }).orElse(false);
    }

    /**
     * ZADANIE 4 - raport ocen z kursu: imię i nazwisko (rozdzielone spacją) -> lista ocen rosnąco.
     * Ręcznie wykonujemy tylko jedno zapytanie (kurs po nazwie - wewnątrz repozytorium); studentów i ich oceny
     * Hibernate doczytuje leniwie przy dostępie do kolekcji (LAZY) - dlatego sesja musi być otwarta.
     */
    public Map<String, List<Float>> getStudentGrades(final String courseName) {
        return studentRepository.findAllByCourseName(courseName).stream()          // zapisani studenci (pusta lista, gdy kurs nie istnieje)
                .collect(Collectors.toMap(
                        Student::fullName,                                         // klucz: "Imię Nazwisko"
                        student -> gradesInCourse(student, courseName),            // wartość: posortowane oceny z TEGO kursu
                        SchoolService::mergeSorted));                              // dwoje studentów o tym samym imieniu i nazwisku: łączymy listy zamiast rzucać wyjątek
    }

    /** Oceny studenta z kursu o podanej nazwie, rosnąco. */
    private static List<Float> gradesInCourse(final Student student, final String courseName) {
        return student.gradeSet().stream()                                         // wszystkie oceny studenta (lazy load)
                .filter(grade -> courseName.equals(grade.course().name()))         // tylko z zadanego kursu (kurs już jest w sesji - bez dodatkowego SELECT)
                .map(Grade::grade)                                                 // Grade -> float
                .sorted()                                                          // rosnąco
                .toList();                                                         // niemodyfikowalna lista
    }

    /** Scalanie dwóch posortowanych list ocen (kolizja klucza w mapie - dwie osoby o tym samym imieniu i nazwisku). */
    private static List<Float> mergeSorted(final List<Float> first, final List<Float> second) {
        return Stream.concat(first.stream(), second.stream()).sorted().toList();
    }
}
