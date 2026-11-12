package pl.edu.agh.iisg.to.dao;

import jakarta.persistence.PersistenceException;      // bazowy wyjątek JPA; Hibernate rzuca nim (lub podklasami), gdy zapytanie się nie powiedzie
import pl.edu.agh.iisg.to.model.Student;
import pl.edu.agh.iisg.to.session.SessionService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * DAO studenta - ZADANIA 1-3 z rozdziału "Data Access Object".
 */
public class StudentDao extends GenericDao<Student> {

    public StudentDao(SessionService sessionService) {
        super(sessionService, Student.class);               // przekazujemy klasę encji, by GenericDao umiało wykonać session.get(Student.class, id)
    }

    /**
     * ZADANIE 1 - utworzenie studenta.
     * Cykl życia encji: new Student(...) -> TRANSIENT (Hibernate nic o nim nie wie);
     * save() -> persist() -> PERSISTENT (obiekt w kontekście trwałości, dostał id); commit() -> INSERT w bazie.
     * Jeśli INSERT naruszy UNIQUE(index_number), transakcja jest wycofywana i save() zwraca Optional.empty()
     */
    public Optional<Student> create(final String firstName, final String lastName, final int indexNumber) {
        Student student = new Student(firstName, lastName, indexNumber);   // obiekt w stanie TRANSIENT
        return save(student);                                             // GenericDao.save(): persist + commit w transakcji
    }

    /**
     * ZADANIE 3 - lista wszystkich studentów w ustalonej kolejności.
     * Test allStudentsCanBeListed() oczekuje kolejności -> sortowanie po nazwisku.
     * Sortowanie realizujemy W ZAPYTANIU (ORDER BY), nie w Javie: baza robi to szybciej (indeksy), nie musimy
     * ładować wszystkiego do pamięci, by posortować, a kolejność jest gwarantowana niezależnie od implementacji
     * kolekcji. Dodatkowe klucze (imię, numer indeksu) dają porządek całkowity - wynik jest deterministyczny
     * także dla dwóch osób o tym samym nazwisku.
     */
    public List<Student> findAll() {
        try {
            return currentSession()                                                                         // bieżąca sesja Hibernate
                    .createQuery("SELECT s FROM Student s ORDER BY s.lastName, s.firstName, s.indexNumber", // JPQL: nazwy KLAS i PÓL, nie tabel i kolumn
                            Student.class)                                                                  // typ wyniku -> Query<Student>, bez rzutowań
                    .getResultList();                                                                       // wykonanie SELECT i zmapowanie wierszy na encje
        } catch (PersistenceException e) {                                                                  // błąd zapytania / połączenia
            e.printStackTrace();                                                                            // w realnej aplikacji: logger
            return Collections.emptyList();                                                                 // kontrakt metody: nigdy null
        }
    }

    /**
     * ZADANIE 2 - wyszukanie studenta po numerze indeksu (zapytanie JPQL z parametrem nazwanym).
     * Parametr wiążemy przez setParameter(), a nie sklejając String - to chroni przed SQL injection i pozwala
     * Hibernate cache'ować plan zapytania. indexNumber jest UNIQUE, więc wynik to 0 lub 1 wiersz:
     * uniqueResultOptional() zwraca Optional.empty() dla 0 wierszy, Optional.of(student) dla 1,
     * a NonUniqueResultException (podklasa PersistenceException) dla >1 - co jest obsłużone w catch.
     */
    public Optional<Student> findByIndexNumber(final int indexNumber) {
        try {
            return currentSession()
                    .createQuery("SELECT s FROM Student s WHERE s.indexNumber = :indexNumber", Student.class) // :indexNumber - parametr nazwany
                    .setParameter("indexNumber", indexNumber)                                                // bezpieczne wiązanie wartości
                    .uniqueResultOptional();                                                                 // 0 lub 1 wynik jako Optional
        } catch (PersistenceException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
