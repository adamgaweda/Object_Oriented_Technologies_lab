package model;

import javafx.collections.ListChangeListener;
import javafx.scene.image.Image;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rozdział "Interakcje w GUI": galeria musi być obserwowalna, a zmieniać ją wolno tylko przez jej metody. */
class GalleryTest {

    @Test
    void observersAreNotifiedAboutAddedAndRemovedPhotos() {
        Gallery gallery = new Gallery();
        List<String> events = new ArrayList<>();
        gallery.getPhotos().addListener((ListChangeListener<? super Photo>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) events.add("removed " + change.getRemoved());
                if (change.wasAdded()) events.add("added " + change.getAddedSubList());
            }
        });

        gallery.addPhoto(photo("a.png"));
        gallery.addPhoto(photo("b.png"));
        gallery.clear();

        assertEquals(List.of("added [a.png]", "added [b.png]", "removed [a.png, b.png]"), events);
        assertTrue(gallery.getPhotos().isEmpty());
    }

    @Test
    void photosCannotBeModifiedBypassingTheModel() {
        Gallery gallery = new Gallery();
        gallery.addPhoto(photo("a.png"));

        assertThrows(UnsupportedOperationException.class, () -> gallery.getPhotos().add(photo("x.png")));
        assertThrows(UnsupportedOperationException.class, () -> gallery.getPhotos().remove(0));
        assertThrows(UnsupportedOperationException.class, () -> gallery.getPhotos().clear());
        assertEquals(1, gallery.getPhotos().size());
    }

    /** Zdjęcie bez obrazu: testy nie potrzebują dekodowania obrazu ani toolkitu JavaFX. */
    private static Photo photo(String name) {
        return new Photo(name, (Image) null);
    }
}
