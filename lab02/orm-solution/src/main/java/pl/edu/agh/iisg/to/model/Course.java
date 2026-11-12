package pl.edu.agh.iisg.to.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Encja kursu
 * Tabela: course(id PK, name VARCHAR(50) NOT NULL UNIQUE) + tabela łącząca student_course(course_id, student_id).
 */
@Entity                                 // klasa jest encją
@Table(name = Course.TABLE_NAME)        // tabela "course"
public class Course {

    public static final String TABLE_NAME = "course";

    @Id                                                   // klucz główny
    @GeneratedValue(strategy = GenerationType.TABLE)      // generowany z tabeli sekwencji hibernate_sequences
    @Column(name = Columns.ID)                            // kolumna "id"
    private int id;

    @Column(name = Columns.NAME, nullable = false, length = 50, unique = true)  // name VARCHAR(50) NOT NULL UNIQUE
    private String name;

    /**
     * Relacja N:M - TO JEST STRONA WŁAŚCICIELSKA (tu jest @JoinTable), więc tylko zmiany tej kolekcji
     * trafiają do tabeli student_course.
     *
     */
    @ManyToMany
    @JoinTable(
            name = "student_course",                                                                   // nazwa tabeli łączącej jak w Lab 1
            joinColumns = @JoinColumn(name = "course_id", referencedColumnName = Columns.ID),           // FK do tabeli course (właściciel)
            inverseJoinColumns = @JoinColumn(name = "student_id", referencedColumnName = Student.Columns.ID)) // FK do tabeli student
    private Set<Student> studentSet = new HashSet<>();

    /** Relacja 1:N Course -> Grade, strona odwrotna; właścicielem jest Grade.course (kolumna grade.course_id). */
    @OneToMany(mappedBy = "course")
    private Set<Grade> gradeSet = new HashSet<>();

    /** Konstruktor bezargumentowy wymagany przez Hibernate. */
    Course() {
    }

    public Course(final String name) {
        this.name = name;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Set<Student> studentSet() {
        return studentSet;
    }

    public Set<Grade> gradeSet() {
        return gradeSet;
    }

    public static class Columns {

        public static final String ID = "id";

        public static final String NAME = "name";

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return id == course.id && Objects.equals(name, course.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
