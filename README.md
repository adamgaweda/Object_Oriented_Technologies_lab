# Technologie Obiektowe (Object-Oriented Technologies)

Repozytorium zawiera projekty laboratoryjne realizowane w ramach przedmiotu **Technologie Obiektowe** (AGH). Projekty prezentują kluczowe wzorce architektoniczne, programowanie reaktywne oraz tworzenie reaktywnych interfejsów graficznych w języku Java.

---

## 📋 Spis treści

- [Wymagania środowiskowe](#-wymagania-środowiskowe)
- [Struktura projektów](#-struktura-projektów)
  - [Lab 01 – Active Record](#lab-01--active-record)
  - [Lab 02 – ORM & Hibernate (DAO / Repository)](#lab-02--orm--hibernate-dao--repository)
  - [Lab 03 – Programowanie reaktywne (RxJava)](#lab-03--programowanie-reaktywne-rxjava)
  - [Lab 04 – Reaktywne GUI (JavaFX + RxJava)](#lab-04--reaktywne-gui-javafx--rxjava)
  - [rx-examples-solved – Rozwiązania zadań RxJava](#rx-examples-solved--rozwiązania-zadań-rxjava)
- [Uruchamianie i testowanie](#-uruchamianie-i-testowanie)
- [Konfiguracja Git (.gitignore)](#-konfiguracja-git-gitignore)

---

## 🛠 Wymagania środowiskowe

- **Java JDK**: 25 (rekomendowana wersja zgodna z konfiguracją `build.gradle`, min. Java 21)
- **Gradle**: 9.x (w projektach dostępny jest wrapper `gradlew` / `gradlew.bat`)
- **IDE**: IntelliJ IDEA / VS Code / Eclipse

---

## 📂 Struktura projektów

```text
Object_oriented_technologies/
├── lab01/
│   └── active-record/          # Wzorzec Active Record z SQLite
├── lab02/
│   ├── orm/                    # Zadanie z mapowaniem ORM (Hibernate) i wzorcami DAO/Repository
│   └── orm-solution/           # Pełne rozwiązanie laboratorium ORM
├── lab03/
│   ├── rx-examples/            # Ćwiczenia wprowadzające do RxJava (analiza danych o filmach)
│   └── lab-rx/                 # Reaktywny crawler zdjęć (PhotoCrawler + Serper API)
├── lab04/
│   ├── lab-gui-gallery/        # Galeria zdjęć w JavaFX z integracją reaktywną
│   └── gui-solution/           # Pełne rozwiązanie galerii zdjęć w JavaFX
├── rx-examples-solved/
│   └── rx-examples/            # Wzorcowo rozwiązane zadania z programowania reaktywnego
├── .gitignore                  # Zoptymalizowana konfiguracja wykluczeń Gita
└── README.md                   # Dokumentacja projektu
```

---

### Lab 01 – Active Record
- **Katalog**: `lab01/active-record`
- **Tematyka**: Implementacja wzorca architektonicznego **Active Record** z bezpośrednim dostępem do relacyjnej bazy danych (SQLite / JDBC).
- **Główne elementy**:
  - `ConnectionProvider` – zarządzanie połączeniem JDBC.
  - `QueryExecutor` i `QueryHelper` – dynamiczne konstruowanie i wykonywanie zapytań SQL (`SELECT`, `INSERT`, `UPDATE`, `DELETE`).
  - Modele encji: `Student`, `Course`, `Grade` zawierające zarówno stan, jak i logikę utrwalania w bazie danych.
  - Testy jednostkowe: `ActiveRecordTest`.

### Lab 02 – ORM & Hibernate (DAO / Repository)
- **Katalogi**: `lab02/orm` oraz `lab02/orm-solution`
- **Tematyka**: Zaawansowane mapowanie obiektowo-relacyjne przy użyciu biblioteki **Hibernate / JPA**, ze szczególnym uwzględnieniem wzorców architektonicznych warstwy dostępu do danych.
- **Główne elementy**:
  - `GenericDao<T>` oraz dedykowane DAO: `CourseDao`, `StudentDao`, `GradeDao`.
  - Wzorzec **Repository**: `Repository<T>`, `StudentRepository`.
  - Serwis biznesowy: `SchoolService` (przypisywanie studentów do kursów, obliczanie średnich ocen, operacje transakcyjne).
  - Zarządzanie kontekstem transakcji i sesji: `SessionService`, `TransactionService`.
  - Testy weryfikujące: `OrmTest`, `OrmExtendedTest`.

### Lab 03 – Programowanie reaktywne (RxJava)
- **Katalogi**: `lab03/rx-examples` oraz `lab03/lab-rx`
- **Tematyka**: Paradygmat programowania reaktywnego przy użyciu biblioteki **RxJava 3**.
- **Główne elementy**:
  - **rx-examples**: Przetwarzanie asynchronicznych strumieni zdarzeń (`Observable`), filtrowanie, mapowanie, grupowanie i agregacja danych (`flatMap`, `groupBy`, `reduce`, `zip`) na zbiorach danych filmowych (`movies.tsv`, `actors.tsv`).
  - **lab-rx**: Reaktywny pobieracz zdjęć (`PhotoCrawler`), wykorzystujący API wyszukiwania (`GoogleSearchDriver`), asynchroniczne pobieranie (`PhotoDownloader`) oraz przetwarzanie i skalowanie obrazów (`PhotoProcessor`).

### Lab 04 – Reaktywne GUI (JavaFX + RxJava)
- **Katalogi**: `lab04/lab-gui-gallery` oraz `lab04/gui-solution`
- **Tematyka**: Budowa aplikacji desktopowej w **JavaFX** z reaktywnym przepływem danych i architekturą MVC.
- **Główne elementy**:
  - `GalleryApp` – punkt wejściowy aplikacji JavaFX, ładowanie widoków FXML i stylów CSS.
  - `GalleryController` – kontroler widoku obsługujący interakcje użytkownika, zdarzenia interfejsu i powiązania (bindings).
  - Modele `Gallery` i `Photo` oparte o właściwości obserwowalne JavaFX (`ObservableList`, `StringProperty`).
  - `PhotoSerializer` – asynchroniczny serwis dyskowy na dedykowanym wątku roboczym, reagujący na zdarzenia modelu (zapis, zmiana nazwy, usunięcie pliku) bez blokowania wątku UI JavaFX.
  - Testy jednostkowe: `GalleryTest`, `PhotoTest`, `PhotoSerializerTest`.

### rx-examples-solved – Rozwiązania zadań RxJava
- **Katalog**: `rx-examples-solved/rx-examples`
- **Tematyka**: Kompletne, referencyjne rozwiązania zadań ze strumieniami reaktywnymi RxJava 3 wraz z zestawem testów `RxTests`.

---

## 🚀 Uruchamianie i testowanie

W każdym podprojekcie znajduje się skrypt Gradle Wrapper (`gradlew` na Linux/macOS, `gradlew.bat` na Windows).

### Uruchomienie testów dla wybranego modułu:

```bash
# Lab 01 - Active Record
cd lab01/active-record
./gradlew test

# Lab 02 - ORM
cd lab02/orm-solution
./gradlew test

# Lab 03 - RxJava Przykłady
cd lab03/rx-examples
./gradlew test

# Lab 04 - Galeria JavaFX
cd lab04/gui-solution
./gradlew test
```

### Uruchomienie aplikacji:

```bash
# Uruchomienie aplikacji Active Record (Lab 01)
cd lab01/active-record
./gradlew run

# Uruchomienie crawlera zdjęć (Lab 03)
cd lab03/lab-rx
./gradlew run

# Uruchomienie aplikacji galerii JavaFX (Lab 04)
cd lab04/gui-solution
./gradlew run
```

---

## 🛡 Konfiguracja Git (.gitignore)

Plik `.gitignore` w katalogu głównym został zoptymalizowany i skonfigurowany pod kątem ekosystemu Java, Gradle oraz środowisk IDE:

- **Pliki budowania i kompilacji**: Wykluczone katalogi `build/`, `bin/`, `out/`, `target/` oraz pliki `*.class`.
- **Pamięć podręczna Gradle**: Wykluczone foldery `.gradle/`.
- **Gradle Wrapper**: Wykluczone archiwa `*.jar`, z wyjątkiem `gradle/wrapper/gradle-wrapper.jar` (niezbędnego do działania `./gradlew` po sklonowaniu repozytorium).
- **Bazy danych SQLite / testowe**: Ignorowane pliki tymczasowych baz tworzonych w czasie testów (`*.db`, `*.db-journal`, `*.sqlite*`).
- **Pliki tymczasowe laboratoriów**: Ignorowane katalogi `photos/` (pobrane dynamicznie z internetu obrazy w Lab 03 i 04).
- **Środowiska IDE**: Ignorowane pliki konfiguracyjne IntelliJ (`.idea/`, `.run/`, `*.iml`), VS Code (`.vscode/`) oraz Eclipse.

