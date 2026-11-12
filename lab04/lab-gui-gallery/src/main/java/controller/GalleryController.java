package controller;


import javafx.fxml.FXML;

import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import model.Gallery;
import model.Photo;

public class GalleryController {

    private Gallery galleryModel;
    @FXML
    private TextField imageNameField;
    @FXML
    private Button addPhotoButton;
    @FXML
    private Image imageView;


    @FXML
    public void initialize() {
        // TODO additional FX controls initialization
    }

    public void setModel(Gallery gallery) {
        this.galleryModel = gallery;
        imagesListView.setItems(gallery.getPhotos());
        imagesListView.getSelctionModel().select(0);
    }

    private void bindSelectedPhoto(Photo photo) {
        // TODO view <-> model bindings configuration
        imageNameField.textProperty().bindBidirectional(photo.nameProperty());
        imageView.imageProperty().bind(photo.photoDataProperty());
    }

    private void  unbindSelectedPhoto(Photo photo) {
        imageNameField.textProperty().unbindBidirectional(photo.nameProperty);
        imageView.imageProperty().unbind();
        imageNameField.clear();
        imageView.setImage(null);
    }

}

