package controller;

import io.reactivex.rxjava3.disposables.Disposable;        // uchwyt do trwającej subskrypcji (można ją przerwać)
import io.reactivex.rxjava3.schedulers.Schedulers;        // pule wątków RxJava (io = operacje blokujące)
import javafx.application.Platform;                       // runLater: zlecenie kodu na wątek JavaFX
import javafx.collections.ListChangeListener;             // obserwator listy obserwowalnej
import javafx.event.ActionEvent;
import javafx.fxml.FXML;                                  // oznaczenie pól/metod wstrzykiwanych i wołanych z FXML
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import model.Gallery;
import model.Photo;
import org.pdfsam.rxjavafx.schedulers.JavaFxScheduler;    // scheduler RxJavaFX wykonujący sygnały na wątku JavaFX
import util.PhotoDownloader;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Kontroler widoku galerii
 * FXMLLoader tworzy ten obiekt na podstawie atrybutu fx:controller w galleryView.fxml, wstrzykuje do pól @FXML
 * kontrolki o pasujących fx:id, a na końcu woła initialize(). Model trafia tu później, przez setModel(),
 * bo kontroler nie powinien sam go tworzyć - dostaje go z zewnątrz (GalleryApp).
 */
public class GalleryController {

    private static final Logger log = Logger.getLogger(GalleryController.class.getName());

    @FXML
    private TextField imageNameField;        // fx:id="imageNameField" - edytowalna nazwa zaznaczonego zdjęcia

    @FXML
    private ImageView imageView;             // fx:id="imageView" - podgląd zaznaczonego zdjęcia

    @FXML
    private ListView<Photo> imagesListView;  // fx:id="imagesListView" - lista miniatur; typ elementu musi być Photo

    @FXML
    private TextField searchTextField;       // fx:id="searchTextField" - zapytanie do wyszukiwarki

    @FXML
    private Button searchButton;             // fx:id="searchButton" - blokowany na czas wyszukiwania

    private Gallery galleryModel;

    private final PhotoDownloader photoDownloader = new PhotoDownloader();   // bezstanowy, jedna instancja wystarczy

    private Disposable currentSearch;        // trwające wyszukiwanie; nowe kliknięcie przerywa poprzednie

    /**
     * Wołane przez FXMLLoader po wstrzyknięciu kontrolek, jeszcze przed setModel(). Konfigurujemy tu to, co zależy
     * tylko od kontrolek: sposób rysowania elementów listy i reakcję na zmianę zaznaczenia.
     */
    @FXML
    public void initialize() {
        imagesListView.setCellFactory(listView -> new PhotoListCell());        // każdy wiersz listy rysuje miniaturę zamiast toString()

        // Interakcja widok - widok: zaznaczenie na liście steruje głównym podglądem. selectedItemProperty() to
        // właściwość JavaFX, więc zamiast ją bindować, dodajemy obserwatora (ChangeListener): dostaje starą i nową wartość.
        imagesListView.getSelectionModel().selectedItemProperty()
                .addListener((observable, previousPhoto, selectedPhoto) -> {
                    if (previousPhoto != null) {
                        unbindSelectedPhoto(previousPhoto);                      // najpierw zdejmujemy stare powiązania (bindBidirectional ich sam nie usuwa)
                    }
                    if (selectedPhoto != null) {
                        bindSelectedPhoto(selectedPhoto);                        // potem podpinamy nowe; null = nic nie zaznaczono (np. po wyczyszczeniu galerii)
                    }
                });
    }

    /** Podpięcie modelu: lista kontrolki = lista modelu (ta sama obserwowalna lista, bez kopiowania). */
    public void setModel(Gallery gallery) {
        this.galleryModel = gallery;
        imagesListView.setItems(gallery.getPhotos());                            // wymaga ObservableList - zwykła List nie skompiluje się
        imagesListView.getSelectionModel().select(0);                            // zaznacz pierwsze zdjęcie (brak efektu, gdy lista pusta)
    }

    /**
     * Interakcja model - widok. Zamiast imageNameField.setText(photo.getName()) wiążemy właściwości:
     * dwukierunkowo dla nazwy (edycja w polu zmienia model, zmiana modelu odświeża pole) i jednokierunkowo dla obrazu
     * (widok tylko odczytuje). Typy po obu stronach muszą się zgadzać: StringProperty z StringProperty,
     * ObjectProperty<Image> z ReadOnlyObjectProperty<Image>.
     */
    private void bindSelectedPhoto(Photo photo) {
        imageNameField.textProperty().bindBidirectional(photo.nameProperty());
        imageView.imageProperty().bind(photo.photoDataProperty());
    }

    /** Odwrotność bindSelectedPhoto: bez tego pole tekstowe pozostałoby związane ze starym zdjęciem (i zmieniałoby jego nazwę). */
    private void unbindSelectedPhoto(Photo photo) {
        imageNameField.textProperty().unbindBidirectional(photo.nameProperty());
        imageView.imageProperty().unbind();
        imageNameField.clear();                                                  // nie pokazujemy danych zdjęcia, które przestało być zaznaczone
        imageView.setImage(null);
    }

    // Gdy zdjęcia dopiero przyjdą (asynchroniczne wyszukiwanie), zaznacz pierwsze z nich automatycznie.
//        gallery.getPhotos().addListener((ListChangeListener<? super Photo>) change ->
//            Platform.runLater(() -> {                                        // poza obsługą bieżącego zdarzenia listy - ListView zdążył je przetworzyć
//        if (imagesListView.getSelectionModel().isEmpty() && !gallery.getPhotos().isEmpty()) {
//            imagesListView.getSelectionModel().select(0);
//        }
//    }));

    /**
     * Interakcja z serwisem i obsługa długotrwałej operacji. Wyszukiwanie i pobieranie trwają sekundy, więc nie mogą
     * wykonywać się na wątku JavaFX (okno przestałoby reagować). subscribeOn przenosi je na pulę io; observeOn
     * sprowadza każde zdjęcie z powrotem na wątek JavaFX, bo addPhoto modyfikuje obserwowalną listę, na którą patrzy
     * ListView - JavaFX dopuszcza takie zmiany wyłącznie ze swojego wątku (inaczej "Not on FX application thread").
     */
    @FXML
    public void searchButtonClicked(ActionEvent event) {
        String query = searchTextField.getText().trim();
        if (query.isEmpty()) {
            return;                                                              // puste zapytanie - nic nie robimy
        }
        if (currentSearch != null) {
            currentSearch.dispose();                                             // poprzednie wyszukiwanie przestaje dodawać zdjęcia do nowej galerii
        }
        galleryModel.clear();                                                    // serializer usunie stare pliki, lista się opróżni, zaznaczenie zniknie
        searchButton.setDisable(true);                                           // informacja zwrotna dla użytkownika i blokada równoległych wyszukiwań
        currentSearch = photoDownloader.searchForPhotos(query)                  // zimny strumień zdjęć
                .subscribeOn(Schedulers.io())                                    // praca sieciowa na wątku z puli io
                .observeOn(JavaFxScheduler.platform())                           // od tego miejsca w dół: wątek JavaFX
                .doFinally(() -> searchButton.setDisable(false))                 // po onComplete, onError albo dispose
                .subscribe(galleryModel::addPhoto,                               // każde zdjęcie trafia do modelu -> ListView i serializer reagują same
                        error -> log.log(Level.SEVERE, "Searching photos for '" + query + "' failed", error),
                        () -> log.info("Search finished: " + query));
    }

    /** Komórka listy: miniatura zdjęcia o wysokości 50 px. Jeden ImageView na komórkę, ponownie używany przy przewijaniu. */
    private static class PhotoListCell extends ListCell<Photo> {

        private final ImageView thumbnail = new ImageView();

        PhotoListCell() {
            thumbnail.setPreserveRatio(true);
            thumbnail.setFitHeight(50);
        }

        @Override
        protected void updateItem(Photo photo, boolean empty) {
            super.updateItem(photo, empty);                                      // obowiązkowe: aktualizuje stan komórki
            if (empty || photo == null) {
                setText(null);                                                   // pusta komórka (lista krótsza niż widoczny obszar)
                setGraphic(null);
            } else {
                thumbnail.setImage(photo.getPhotoData());
                setGraphic(thumbnail);
            }
        }
    }
}
