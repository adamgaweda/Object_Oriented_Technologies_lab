package model;

import javafx.collections.FXCollections;     // fabryka kolekcji obserwowalnych
import javafx.collections.ObservableList;    // lista powiadamiająca obserwatorów o dodaniu/usunięciu elementów

/**
 * Model galerii: obserwowalna lista zdjęć.
 * Zwykła ArrayList nie wystarczy, bo ListView (widok) i PhotoSerializer (serwis) muszą dowiadywać się o każdej
 * zmianie zawartości - ObservableList wysyła im zdarzenia ListChangeListener. Zmiany wykonuje się przez metody
 * galerii (addPhoto, clear); na zewnątrz lista jest wystawiona jako niemodyfikowalna, żeby żaden konsument
 * nie mógł obejść modelu i zmienić jej bezpośrednio.
 */
public class Gallery {

    private final ObservableList<Photo> photos = FXCollections.observableArrayList();               // właściwa lista

    private final ObservableList<Photo> readOnlyPhotos = FXCollections.unmodifiableObservableList(photos); // widok tylko do odczytu, nadal obserwowalny

    public void addPhoto(Photo photo) {
        photos.add(photo);                                   // powiadomi ListView i serializer (wasAdded)
    }

    public ObservableList<Photo> getPhotos() {
        return readOnlyPhotos;                               // ListView.setItems(...) i addListener(...) działają, add()/remove() rzucą wyjątek
    }

    public void clear() {
        photos.clear();                                      // jedno zdarzenie wasRemoved z całą dawną zawartością
    }
}
