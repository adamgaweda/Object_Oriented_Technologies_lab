package model;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import javafx.beans.property.ReadOnlyObjectProperty;   // widok właściwości tylko do odczytu (można obserwować i bindować, nie można ustawiać)
import javafx.beans.property.ReadOnlyObjectWrapper;    // właściwość do odczytu i zapisu wewnątrz klasy, na zewnątrz wystawiana jako ReadOnly...
import javafx.beans.property.SimpleStringProperty;     // gotowa implementacja StringProperty
import javafx.beans.property.StringProperty;           // abstrakcyjny typ właściwości tekstowej (get/set/bind/addListener)
import javafx.scene.image.Image;

/**
 * Model pojedynczego zdjęcia.
 * Każdy atrybut jest opakowany we właściwość JavaFX: obiekt, który poza wartością przechowuje listę obserwatorów
 * i powiadamia ich o każdej zmianie. Dzięki temu kontrolki mogą się do atrybutów "podpiąć" (binding), zamiast
 * być ręcznie odświeżane po każdej modyfikacji modelu. API klasy (getName/setName/getPhotoData) nie zmieniło się.
 * Klasa jest final: to obiekt wartościowy modelu, a właściwości dostają referencję this już w konstruktorze.
 */
public final class Photo {

    /** Nazwa zdjęcia, a zarazem nazwa pliku na dysku. Zmienna z poziomu UI (pole tekstowe), stąd pełna StringProperty. */
    private final StringProperty name;

    /**
     * Dane obrazu. Obraz nigdy nie zmienia się po utworzeniu zdjęcia, więc na zewnątrz wystawiamy wersję tylko do
     * odczytu: widok może ją obserwować i bindować, ale nie może jej podmienić (ReadOnlyObjectWrapper to idiom JavaFX
     * na właściwość "zapisywalną od środka, tylko do odczytu od zewnątrz").
     */
    private final ReadOnlyObjectWrapper<Image> photoData;

    public Photo(String extension, byte[] photoData) {
        this(UUID.randomUUID() + "." + extension, new Image(new ByteArrayInputStream(photoData)));   // losowa, unikalna nazwa pliku
    }

    /** Zdjęcie o zadanej nazwie i gotowym obrazie (używane też w testach, gdzie obrazu nie trzeba dekodować). */
    public Photo(String name, Image image) {
        this.name = new SimpleStringProperty(this, "name", name);                 // (bean, nazwa, wartość początkowa) - ułatwia debugowanie
        this.photoData = new ReadOnlyObjectWrapper<>(this, "photoData", image);
    }

    public String getName() {
        return name.get();                                                        // getter czyta wartość z właściwości
    }

    public void setName(String name) {
        this.name.set(name);                                                      // setter ustawia wartość we właściwości -> powiadomienie obserwatorów
    }

    /** Konwencja JavaFX: xProperty() zwraca cały obiekt właściwości - to do niego podpinają się kontrolki i listenery. */
    public StringProperty nameProperty() {
        return name;
    }

    public Image getPhotoData() {
        return photoData.get();
    }

    public ReadOnlyObjectProperty<Image> photoDataProperty() {
        return photoData.getReadOnlyProperty();                                   // ten sam obiekt wartości, ale bez metody set()
    }

    @Override
    public String toString() {
        return getName();
    }
}
