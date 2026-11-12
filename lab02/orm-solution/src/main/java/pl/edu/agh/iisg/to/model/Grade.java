package pl.edu.agh.iisg.to.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Tabela: grade(id PK, grade REAL NOT NULL, student_id FK, course_id FK).
 * Grade jest WŁAŚCICIELEM obu relacji 1:N (ma kolumny kluczy obcych), dlatego to zapis obiektu Grade tworzy powiązanie
 * student-ocena i kurs-ocena; kolekcje gradeSet w Student i Course są tylko "widokiem" (mappedBy).
 */
@Entity
@Table(name = Grade.TABLE_NAME)
public class Grade {

    public static final String TABLE_NAME = "grade";

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    @Column(name = Columns.ID)
    private int id;

    @Column(name = Columns.GRADE, nullable = false)       // wartość oceny, NOT NULL
    private float grade;

    @ManyToOne(fetch = FetchType.LAZY)                    // wiele ocen -> jeden student; LAZY: student jest doczytywany dopiero przy użyciu
    @JoinColumn(name = Columns.STUDENT_ID)                // kolumna klucza obcego student_id w tabeli grade
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)                    // wiele ocen -> jeden kurs; LAZY: proxy zamiast natychmiastowego SELECT
    @JoinColumn(name = Columns.COURSE_ID)                 // kolumna klucza obcego course_id
    private Course course;

    Grade() {
    }

    public Grade(final Student student, final Course course, final float grade) {
        this.student = student;
        this.course = course;
        this.grade = grade;
    }

    public int id() {
        return id;
    }

    public float grade() {
        return grade;
    }

    public Student student() {
        return student;
    }

    public Course course() {
        return course;
    }

    public static class Columns {

        public static final String ID = "id";

        public static final String GRADE = "grade";

        public static final String STUDENT_ID = "student_id";

        public static final String COURSE_ID = "course_id";

    }

    /*
     * equals/hashCode po (id, grade). Ocena nie ma naturalnego klucza biznesowego (ten sam student może dostać
     * dwa razy 3.0 z tego samego kursu - patrz test courseReportCanBeObtained), więc tożsamość musi opierać się na id.
     * Stąd zasada: NAJPIERW persist() (nadaje id), POTEM dodanie do kolekcji HashSet - patrz SchoolService.gradeStudent.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Grade grade1 = (Grade) o;
        return id == grade1.id && Float.compare(grade, grade1.grade) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, grade);
    }
}
