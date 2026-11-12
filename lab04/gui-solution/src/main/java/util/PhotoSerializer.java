package util;

import javafx.beans.value.ChangeListener;              // obserwator pojedynczej właściwości (stara i nowa wartość)
import javafx.collections.ListChangeListener;          // obserwator listy obserwowalnej (dodane/usunięte elementy)
import javafx.embed.swing.SwingFXUtils;                // konwersja javafx.scene.image.Image -> BufferedImage dla ImageIO
import javafx.scene.image.Image;
import model.Gallery;
import model.Photo;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serwis utrzymujący katalog na dysku w zgodzie z modelem galerii (interakcja model - serwis).
 * Serializer nie jest wołany przez kontroler ani przez galerię: sam obserwuje model (wzorzec Obserwator) i reaguje
 * na trzy zdarzenia: zdjęcie dodane do galerii -> zapis pliku, zdjęcie usunięte -> usunięcie pliku,
 * zmiana nazwy zdjęcia -> zmiana nazwy pliku.
 *
 * Operacje dyskowe wykonuje jeden wątek roboczy w kolejności zleceń. Dzięki temu wątek JavaFX nie czeka na dysk,
 * a zapis, zmiana nazwy i usunięcie tego samego zdjęcia nigdy się nie wyprzedzą.
 */
public class PhotoSerializer implements AutoCloseable {

    private static final Logger log = Logger.getLogger(PhotoSerializer.class.getName());

    private final Path photoLibraryPath;

    /** Jeden wątek = operacje w kolejności zleceń; daemon, żeby nie blokował zamknięcia aplikacji. */
    private final ExecutorService diskWorker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "photo-serializer");
        thread.setDaemon(true);
        return thread;
    });

    /** Obserwatorzy nazw zarejestrowani na zdjęciach z galerii - potrzebni, żeby móc ich zdjąć po usunięciu zdjęcia. */
    private final Map<Photo, ChangeListener<String>> nameListeners = new HashMap<>();

    /** Nazwa pliku, pod którą zdjęcie faktycznie leży na dysku (może różnić się od modelu, gdy nowa nazwa była niepoprawna). */
    private final Map<Photo, String> fileNames = new HashMap<>();

    public PhotoSerializer(String photoLibraryPath) throws IOException {
        this.photoLibraryPath = Path.of(photoLibraryPath);
        deleteLibraryContents();
        createLibraryDirectory();
    }

    /**
     * Podpięcie serializera pod model. Zdjęcia już obecne w galerii są obsługiwane od razu, kolejne - przez obserwatora
     * listy. Usunięte sprawdzamy przed dodanymi, bo zamiana elementu (set) zgłasza oba rodzaje zmiany w jednym zdarzeniu.
     */
    public void registerGallery(Gallery gallery) {
        gallery.getPhotos().forEach(this::track);
        gallery.getPhotos().addListener((ListChangeListener<? super Photo>) change -> {
            while (change.next()) {                                  // jedno zdarzenie może opisywać kilka zmian
                if (change.wasRemoved()) {
                    change.getRemoved().forEach(this::untrack);      // np. clear() przed nowym wyszukiwaniem
                }
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::track);   // np. addPhoto() z wyszukiwania
                }
            }
        });
    }

    /**
     * Nowe zdjęcie w galerii: zapisujemy plik i zaczynamy obserwować nazwę. Obserwator nazwy musi powstać tutaj,
     * bo dopiero w tym momencie serializer dowiaduje się o istnieniu zdjęcia; wcześniej nie ma do czego go podpiąć.
     */
    private void track(Photo photo) {
        ChangeListener<String> nameListener = (observable, oldName, newName) -> renamePhoto(photo, newName);
        nameListeners.put(photo, nameListener);
        fileNames.put(photo, photo.getName());
        photo.nameProperty().addListener(nameListener);
        savePhoto(photo);
    }

    /** Zdjęcie usunięte z galerii: zdejmujemy obserwatora (inaczej zostałby w pamięci razem ze zdjęciem) i kasujemy plik. */
    private void untrack(Photo photo) {
        ChangeListener<String> nameListener = nameListeners.remove(photo);
        if (nameListener != null) {
            photo.nameProperty().removeListener(nameListener);
        }
        removePhoto(photo);
    }

    private void createLibraryDirectory() throws IOException {
        Files.createDirectories(photoLibraryPath);                   // tworzy katalog (i brakujące nadrzędne); istniejący nie przeszkadza
        if (!Files.isDirectory(photoLibraryPath)) {
            throw new IOException("This is not valid photo library directory path or directory could not be created: " + photoLibraryPath);
        }
    }

    /**
     * Zapis zdjęcia. Nazwę i obraz odczytujemy na wątku wołającym (JavaFX), zanim zlecimy pracę: wątek roboczy dostaje
     * gotowe wartości, więc nie musi dotykać modelu, a kolejność zleceń (zapis, potem ewentualna zmiana nazwy) jest
     * zachowana nawet, gdy użytkownik zmieni nazwę, zanim plik zdąży się zapisać.
     */
    public void savePhoto(Photo photo) {
        String fileName = fileNames.getOrDefault(photo, photo.getName());
        Image image = photo.getPhotoData();
        diskWorker.execute(() -> {
            try {
                writeImage(image, photoLibraryPath.resolve(fileName));
                log.info("SAVE photo: " + fileName);
            } catch (IOException e) {
                log.log(Level.WARNING, "Could not save photo " + fileName, e);
            }
        });
    }

    /** Zmiana nazwy pliku w ślad za zmianą nazwy zdjęcia w modelu. Niepoprawną nazwę pomijamy; plik zostaje pod dotychczasową. */
    public void renamePhoto(Photo photo, String newName) {
        String currentName = fileNames.getOrDefault(photo, photo.getName());
        if (!isValidFileName(newName) || newName.equals(currentName)) {
            log.warning("Ignoring invalid or unchanged photo name: '" + newName + "'");
            return;
        }
        fileNames.put(photo, newName);
        diskWorker.execute(() -> {
            try {
                moveFile(photoLibraryPath.resolve(currentName), photoLibraryPath.resolve(newName));
                log.info("RENAME photo: " + currentName + " -> " + newName);
            } catch (IOException e) {
                log.log(Level.WARNING, "Could not rename photo " + currentName + " -> " + newName, e);
            }
        });
    }

    public void removePhoto(Photo photo) {
        String fileName = fileNames.remove(photo);
        if (fileName == null) {
            fileName = photo.getName();
        }
        String toDelete = fileName;
        diskWorker.execute(() -> {
            try {
                deleteFile(photoLibraryPath.resolve(toDelete));
                log.info("REMOVE photo: " + toDelete);
            } catch (IOException e) {
                log.log(Level.WARNING, "Could not remove photo " + toDelete, e);
            }
        });
    }

    /** Czyści katalog przy starcie; prywatna, bo konstruktor nie może wołać metod, które podklasa mogłaby nadpisać. */
    private void deleteLibraryContents() throws IOException {
        if (Files.isDirectory(photoLibraryPath)) {
            try (var files = Files.list(photoLibraryPath)) {
                for (Path file : files.toList()) {
                    Files.deleteIfExists(file);
                }
            }
        }
    }

    /** Czeka na dokończenie zleconych operacji dyskowych (wołane przy zamykaniu aplikacji z GalleryApp.stop()). */
    @Override
    public void close() {
        diskWorker.shutdown();
        try {
            if (!diskWorker.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warning("Some photo files may not have been written before exit");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isValidFileName(String name) {
        return name != null && !name.isBlank() && !name.contains("/") && !name.contains("\\") && !name.equals(".") && !name.equals("..");
    }

    /* Operacje na plikach wydzielone do osobnych metod: w testach podmienia się je na rejestrowanie wywołań, bez dysku i bez obrazów. */

    protected void writeImage(Image image, Path target) throws IOException {
        // Zawsze PNG: BufferedImage z SwingFXUtils ma kanał alfa, którego koder JPEG nie zapisze (ImageIO.write zwróciłoby false).
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", target.toFile());
    }

    protected void moveFile(Path from, Path to) throws IOException {
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    protected void deleteFile(Path file) throws IOException {
        Files.deleteIfExists(file);
    }
}
