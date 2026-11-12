package pl.edu.agh.iisg.to.model;

import jakarta.persistence.CascadeType;      // typy operacji kaskadowych (PERSIST, REMOVE, ...) propagowanych na powiązane encje
import jakarta.persistence.Column;           // mapowanie pola na kolumnę tabeli (nazwa, NOT NULL, długość, unikalność)
import jakarta.persistence.Entity;           // oznaczenie klasy jako encji zarządzanej przez ORM
import jakarta.persistence.GeneratedValue;   // deklaracja, że wartość klucza głównego generuje ORM/baza
import jakarta.persistence.GenerationType;   // strategia generowania klucza (TABLE, SEQUENCE, IDENTITY, AUTO)
import jakarta.persistence.Id;               // oznaczenie pola klucza głównego
import jakarta.persistence.ManyToMany;       // relacja wiele-do-wielu
import jakarta.persistence.OneToMany;        // relacja jeden-do-wielu
import jakarta.persistence.Table;            // nazwa tabeli, na którą mapowana jest encja

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Encja reprezentująca studenta.
 *
 * ZADANIE 1 (Mapowanie w ORM)
 *
 * Schemat tabeli jest identyczny z Lab 1 (Active Record):
 * student(id PK, first_name VARCHAR(50) NOT NULL, last_name VARCHAR(50) NOT NULL, index_number INT NOT NULL UNIQUE)
 */
@Entity                                   // Hibernate ma zarządzać obiektami tej klasy i odwzorowywać je na wiersze tabeli
@Table(name = Student.TABLE_NAME)         // nazwa tabeli w bazie: "student" (bez tego byłaby to nazwa klasy: "Student")
public class Student {

    public static final String TABLE_NAME = "student";

    @Id                                                   // to pole jest kluczem głównym (identyfikatorem encji)
    @GeneratedValue(strategy = GenerationType.TABLE)      // wartość nadaje Hibernate z pomocniczej tabeli sekwencji (hibernate_sequences);
                                                          // ta sama strategia co w Course i Grade - id jest znane już w chwili persist()
    @Column(name = Columns.ID)                            // kolumna "id"
    private int id;

    @Column(name = Columns.FIRST_NAME, nullable = false, length = 50)   // first_name VARCHAR(50) NOT NULL
    private String firstName;

    @Column(name = Columns.LAST_NAME, nullable = false, length = 50)    // last_name VARCHAR(50) NOT NULL
    private String lastName;

    @Column(name = Columns.INDEX_NUMBER, nullable = false, unique = true) // index_number INT NOT NULL UNIQUE - unikalność egzekwuje BAZA:
                                                                          // próba zapisu duplikatu kończy się wyjątkiem przy commit(),
                                                                          // dzięki czemu StudentDao.create() zwraca Optional.empty()
    private int indexNumber;

    /**
     * Relacja 1:N Student -> Grade. To strona ODWROTNA (inverse): {@code mappedBy = "student"} wskazuje pole
     * {@code Grade.student}, które jest właścicielem relacji (to w tabeli grade jest kolumna FK student_id).
     * Hibernate nie zapisuje tej kolekcji do bazy - wypełnia ją przy odczycie na podstawie kolumny grade.student_id.
     *
     * {@code cascade = CascadeType.REMOVE} - usunięcie studenta usuwa również jego oceny.
     */
    @OneToMany(mappedBy = "student", cascade = CascadeType.REMOVE)
    private Set<Grade> gradeSet = new HashSet<>();

    /**
     * Relacja N:M Student <-> Course. Stroną WŁAŚCICIELSKĄ jest {@code Course.studentSet} (tam zdefiniowano
     * {@code @JoinTable student_course}); tu podajemy tylko {@code mappedBy}. Konsekwencja: dodanie kursu do tej
     * kolekcji NIE tworzy wiersza w student_course - trzeba modyfikować {@code course.studentSet()}
     * (patrz SchoolService.enrollStudent oraz StudentRepository.remove).
     */
    @ManyToMany(mappedBy = "studentSet")
    private Set<Course> courseSet = new HashSet<>();

    /** Konstruktor bezargumentowy wymagany przez JPA/Hibernate (tworzenie obiektów przez refleksję, proxy). Pakietowy, by nie zachęcać do użycia. */
    Student() {
    }

    public Student(final String firstName, final String lastName, final int indexNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.indexNumber = indexNumber;
    }

    public int id() {
        return id;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    /** Imię i nazwisko rozdzielone spacją - dokładnie taki klucz ma mieć mapa w SchoolService.getStudentGrades(). */
    public String fullName() {
        return firstName + " " + lastName;
    }

    public int indexNumber() {
        return indexNumber;
    }

    public Set<Course> courseSet() {
        return courseSet;
    }

    public Set<Grade> gradeSet() {
        return gradeSet;
    }

    public static class Columns {

        public static final String ID = "id";

        public static final String FIRST_NAME = "first_name";

        public static final String LAST_NAME = "last_name";

        public static final String INDEX_NUMBER = "index_number";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return id == student.id && indexNumber == student.indexNumber && Objects.equals(firstName, student.firstName) && Objects.equals(lastName, student.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, indexNumber);
    }
}
