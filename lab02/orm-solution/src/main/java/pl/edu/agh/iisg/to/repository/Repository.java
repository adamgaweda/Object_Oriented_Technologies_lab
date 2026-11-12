package pl.edu.agh.iisg.to.repository;

import java.util.List;
import java.util.Optional;

/**
 * ZADANIE 1 (Repozytorium) - generyczny interfejs repozytorium.
 * Repozytorium udostępnia encje tak, jakby były zwykłą KOLEKCJĄ w pamięci (add/getById/findAll/remove);
 * to, że pod spodem jest baza danych i DAO, pozostaje ukryte przed warstwą serwisu.
 *
 * @param <T> typ encji przechowywanej w repozytorium
 */
public interface Repository<T> {

    /** Dodaje encję; Optional.empty() gdy zapis się nie powiódł (np. naruszenie unikalności). */
    Optional<T> add(T entity);

    /** Zwraca encję o podanym kluczu głównym lub Optional.empty(). */
    Optional<T> getById(int id);

    /** Wszystkie encje danego typu. */
    List<T> findAll();

    /** Usuwa encję (wraz z koniecznym uporządkowaniem powiązań). */
    void remove(T entity);
}
