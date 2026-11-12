package model;

import javafx.scene.image.Image;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Rozdział "Model": atrybuty jako właściwości JavaFX. Obraz podajemy jako null - testy nie potrzebują toolkitu JavaFX. */
class PhotoTest {

    @Test
    void gettersAndSettersKeepTheOldApi() {
        Photo photo = photo("cat.png");

        assertEquals("cat.png", photo.getName());
        photo.setName("kitten.png");
        assertEquals("kitten.png", photo.getName());
        assertNull(photo.getPhotoData());
    }

    @Test
    void namePropertyNotifiesObserversAboutChanges() {
        Photo photo = photo("a.png");
        List<String> seen = new ArrayList<>();
        photo.nameProperty().addListener((observable, oldName, newName) -> seen.add(oldName + " -> " + newName));

        photo.setName("b.png");
        photo.setName("b.png");            // ta sama wartość - właściwość nie zgłasza zmiany
        photo.nameProperty().set("c.png");

        assertEquals(List.of("a.png -> b.png", "b.png -> c.png"), seen);
    }

    @Test
    void propertyAccessorsExposeTheSameObjectsThatBackTheGetters() {
        Photo photo = photo("a.png");

        assertSame(photo.nameProperty(), photo.nameProperty());
        assertEquals(photo.getName(), photo.nameProperty().get());
        assertEquals("a.png", photo.toString());
    }

    /** Zdjęcie bez obrazu: testy nie potrzebują dekodowania obrazu ani toolkitu JavaFX. */
    private static Photo photo(String name) {
        return new Photo(name, (Image) null);
    }
}
