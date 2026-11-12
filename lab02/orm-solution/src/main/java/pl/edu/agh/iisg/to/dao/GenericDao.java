package pl.edu.agh.iisg.to.dao;


import jakarta.persistence.PersistenceException;
import org.hibernate.Session;
import pl.edu.agh.iisg.to.session.SessionService;

import java.util.Optional;

/**
 * Bazowe DAO z operacjami wspólnymi dla wszystkich encji
 * Wzorzec DAO: CAŁY kod dostępu do bazy jest tutaj i w podklasach, a nie w klasach modelu (jak w Active Record).
 *
 * @param <T> typ encji obsługiwanej przez konkretne DAO
 */
public abstract class GenericDao<T> {

    private final SessionService sessionService;

    /** Obiekt Class<T> jest potrzebny do session.get(entityClass, id) - generyki Javy są wymazywane w czasie wykonania. */
    private final Class<T> entityClass;

    protected GenericDao(SessionService sessionService, Class<T> entityClass) {
        this.sessionService = sessionService;
        this.entityClass = entityClass;
    }

    /** Odczyt po kluczu głównym: get() zwraca encję lub null (-> Optional.empty()), najpierw sprawdza cache sesji. */
    public Optional<T> findById(int id) {
        return sessionService.doAsTransaction(() -> currentSession().get(entityClass, id));
    }

    /**
     * Zapis nowej encji. persist() przenosi obiekt ze stanu TRANSIENT do PERSISTENT (od tej chwili Hibernate
     * śledzi jego zmiany); INSERT jest wykonywany dopiero przy commit() w doAsTransaction().
     * Przy naruszeniu ograniczeń (np. UNIQUE) commit rzuca wyjątek -> rollback -> Optional.empty().
     */
    public Optional<T> save(final T object) throws PersistenceException {
        Session session = sessionService.getSession();
        return sessionService.doAsTransaction(() -> {
            session.persist(object);
            return object;
        });
    }

    /** Usunięcie: remove() oznacza encję do skasowania (stan REMOVED); DELETE idzie do bazy przy commit(). */
    public boolean remove(final T object) throws PersistenceException {
        Session session = sessionService.getSession();
        return sessionService.doAsTransaction(() -> {
            session.remove(object);
            return true;
        }).orElse(false);
    }

    /** Bieżąca sesja - podklasy budują na niej zapytania JPQL. */
    public Session currentSession() {
        return sessionService.getSession();
    }
}
