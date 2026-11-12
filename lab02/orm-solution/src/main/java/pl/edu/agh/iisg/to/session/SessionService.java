package pl.edu.agh.iisg.to.session;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Zarządza sesją Hibernate (kontekstem trwałości) i transakcjami.
 */
public class SessionService implements TransactionService {

    /** Fabryka sesji - ciężki, tworzony raz obiekt; czyta hibernate.cfg.xml z classpath (w testach: src/test/resources). */
    private final SessionFactory sessionFactory =
            new Configuration().configure() // configures settings from hibernate.cfg.xml
                    .buildSessionFactory();

    /** Bieżąca sesja = kontekst trwałości (persistence context): cache 1. poziomu śledzący stan encji. */
    private Session session;

    public void openSession() {
        session = sessionFactory.openSession();
    }

    public Session getSession() {
        return session;
    }

    public void closeSession() {
        session.close();
    }

    /** Odłącza (detach) wszystkie obiekty od sesji - testy używają tego, by wymusić ponowny odczyt z bazy. */
    public void clearSessionObjects() {
        session.clear();
    }

    /**
     * Szablon transakcji: begin -> zadanie -> commit, a w razie wyjątku rollback i Optional.empty().
     * Jeśli transakcja JUŻ trwa (wywołanie zagnieżdżone, np. gradeDao.save() wewnątrz SchoolService.gradeStudent()),
     * zadanie jest po prostu wykonywane w ramach trwającej transakcji - ewentualny wyjątek propaguje się
     * do zewnętrznego doAsTransaction(), który wycofa CAŁOŚĆ. To daje atomowość operacji biznesowych.
     */
    public <T> Optional<T> doAsTransaction(Supplier<T> task) {
        if (session.getTransaction().isActive()) {              // transakcja zewnętrzna już trwa?
            return Optional.ofNullable(task.get());             // -> dołącz do niej (bez własnego commit/rollback)
        }
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();           // start transakcji JDBC (autocommit = false)
            T result = task.get();                              // właściwa praca (modyfikacje encji w pamięci)
            transaction.commit();                               // commit = flush (wysłanie SQL do bazy) + zatwierdzenie
            return Optional.ofNullable(result);
        } catch (Exception e) {                                 // np. ConstraintViolationException przy duplikacie index_number
            e.printStackTrace();
            if (transaction != null) {
                transaction.rollback();                         // wycofanie zmian w bazie
            }
        }
        return Optional.empty();                                // sygnał niepowodzenia dla wywołującego
    }
}
