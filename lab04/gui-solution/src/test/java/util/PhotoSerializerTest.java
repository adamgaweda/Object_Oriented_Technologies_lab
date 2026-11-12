package util;

import javafx.scene.image.Image;
import model.Gallery;
import model.Photo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Interakcje model - serwis: serializer obserwuje galerię i nazwy zdjęć. Operacje dyskowe są podmienione na zapis
 * do listy, więc test sprawdza wyłącznie logikę obserwatorów i kolejność zleceń, bez obrazów i bez zapisu plików.
 */
class PhotoSerializerTest {

    /** Serializer rejestrujący operacje zamiast wykonywać je na dysku. */
    private static class RecordingSerializer extends PhotoSerializer {

        final List<String> operations = Collections.synchronizedList(new java.util.ArrayList<>());

        RecordingSerializer() throws IOException {
            super(Files.createTempDirectory("gallery-test").toString());
        }

        @Override
        protected void writeImage(Image image, Path target) {
            operations.add("write " + target.getFileName());
        }

        @Override
        protected void moveFile(Path from, Path to) {
            operations.add("move " + from.getFileName() + " -> " + to.getFileName());
        }

        @Override
        protected void deleteFile(Path file) {
            operations.add("delete " + file.getFileName());
        }
    }

    @Test
    void photosPresentAtRegistrationAndAddedLaterAreSaved() throws IOException {
        Gallery gallery = new Gallery();
        gallery.addPhoto(photo("first.png"));
        RecordingSerializer serializer = new RecordingSerializer();

        serializer.registerGallery(gallery);
        gallery.addPhoto(photo("second.png"));
        serializer.close();

        assertEquals(List.of("write first.png", "write second.png"), serializer.operations);
    }

    @Test
    void renamingAPhotoRenamesItsFileAndInvalidNamesAreSkipped() throws IOException {
        Gallery gallery = new Gallery();
        RecordingSerializer serializer = new RecordingSerializer();
        serializer.registerGallery(gallery);
        Photo photo = photo("old.png");

        gallery.addPhoto(photo);
        photo.setName("new.png");          // zmiana w modelu -> zmiana nazwy pliku
        photo.setName("");                 // pusta nazwa - plik zostaje pod "new.png"
        photo.setName("final.png");        // zmiana liczona od ostatniej poprawnej nazwy, nie od ""
        serializer.close();

        assertEquals(List.of("write old.png", "move old.png -> new.png", "move new.png -> final.png"), serializer.operations);
    }

    @Test
    void removedPhotosAreDeletedAndNoLongerObserved() throws IOException {
        Gallery gallery = new Gallery();
        RecordingSerializer serializer = new RecordingSerializer();
        serializer.registerGallery(gallery);
        Photo kept = photo("kept.png");
        Photo removed = photo("removed.png");
        gallery.addPhoto(kept);
        gallery.addPhoto(removed);
        removed.setName("renamed.png");

        gallery.clear();                   // wasRemoved dla obu zdjęć
        removed.setName("ghost.png");      // zdjęcie poza galerią - serializer nie reaguje
        serializer.close();

        assertEquals(List.of("write kept.png", "write removed.png", "move removed.png -> renamed.png",
                "delete kept.png", "delete renamed.png"), serializer.operations);
    }

    @Test
    void operationsOnOnePhotoKeepTheirOrderEvenWhenIssuedImmediatelyOneAfterAnother() throws IOException {
        Gallery gallery = new Gallery();
        RecordingSerializer serializer = new RecordingSerializer();
        serializer.registerGallery(gallery);
        Photo photo = photo("a.png");

        gallery.addPhoto(photo);           // zapis zlecony...
        photo.setName("b.png");            // ...i natychmiast zmiana nazwy, zanim wątek roboczy cokolwiek zrobił
        gallery.clear();
        serializer.close();

        assertEquals(List.of("write a.png", "move a.png -> b.png", "delete b.png"), serializer.operations);
    }

    /** Zdjęcie bez obrazu: testy nie potrzebują dekodowania obrazu ani toolkitu JavaFX. */
    private static Photo photo(String name) {
        return new Photo(name, (Image) null);
    }
}
