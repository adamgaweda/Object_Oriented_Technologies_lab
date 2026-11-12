package pl.edu.agh.iisg.to.session;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Abstrakcja "wykonaj zadanie w transakcji". Warstwa serwisu (SchoolService) zależy od tego interfejsu,
 * a nie od konkretnej SessionService - dzięki temu logika biznesowa nie zna szczegółów Hibernate.
 */
public interface TransactionService {

    /**
     * Wykonuje {@code task} w transakcji bazodanowej.
     * @return wynik zadania opakowany w Optional; Optional.empty() gdy zadanie zwróciło null lub transakcja została wycofana.
     */
    <T> Optional<T> doAsTransaction(Supplier<T> task);
}
