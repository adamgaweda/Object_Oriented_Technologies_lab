package app;

import controller.GalleryController;

import javafx.application.Application;      // klasa bazowa aplikacji JavaFX: launch() -> init() -> start() -> ... -> stop()
import javafx.fxml.FXMLLoader;               // buduje drzewo kontrolek z pliku FXML i tworzy kontroler
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import model.Gallery;
import util.PhotoDownloader;
import util.PhotoSerializer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klasa aplikacji JavaFX. Składa trzy elementy wzorca MVC: model (Gallery), widok (galleryView.fxml)
 * i kontroler (GalleryController), oraz podpina serwis (PhotoSerializer) pod model.
 * start() wykonuje się na wątku JavaFX po Application.launch() z klasy Main.
 */
public class GalleryApp extends Application {

    private static final Logger log = Logger.getLogger(GalleryApp.class.getName());

    private PhotoSerializer photoSerializer;

    @Override
    public void start(Stage primaryStage) throws Exception {
        var gallery = new Gallery();                                  // model
        configureModel(gallery);                                      // serwis obserwujący model + przykładowe zdjęcia
        try {
            var loader = new FXMLLoader();
            loader.setLocation(GalleryApp.class.getClassLoader().getResource("view/galleryView.fxml"));
            BorderPane rootLayout = loader.load();                    // widok: drzewo kontrolek; przy okazji powstaje kontroler i woła initialize()

            GalleryController controller = loader.getController();    // kontroler utworzony przez loader (fx:controller w FXML)
            controller.setModel(gallery);                             // dopiero teraz kontroler dostaje model

            configureStage(primaryStage, rootLayout);
            primaryStage.show();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not load the gallery view", e);
        }
    }

    /** Wołane przez JavaFX przy zamykaniu okna: dajemy serializerowi dokończyć zapis plików. */
    @Override
    public void stop() {
        if (photoSerializer != null) {
            photoSerializer.close();
        }
    }

    private void configureStage(Stage primaryStage, BorderPane rootLayout) {
        var scene = new Scene(rootLayout);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Gallery app");
        primaryStage.minWidthProperty().bind(rootLayout.minWidthProperty());    // okno nie może być mniejsze niż minimalny rozmiar widoku
        primaryStage.minHeightProperty().bind(rootLayout.minHeightProperty());
    }

    private void configureModel(Gallery gallery) throws IOException {
        photoSerializer = new PhotoSerializer("photos");
        photoSerializer.registerGallery(gallery);                     // od teraz każde zdjęcie w galerii ląduje na dysku
        fillGallery(gallery);
    }

    /** Przykładowe zdjęcia pobierane synchronicznie na wątku JavaFX: okno pojawi się dopiero po ich pobraniu. */
    private void fillGallery(Gallery gallery) {
        new PhotoDownloader().getPhotoExamples()
                .subscribe(gallery::addPhoto,
                        error -> log.log(Level.WARNING, "Could not download example photos", error));
    }
}
