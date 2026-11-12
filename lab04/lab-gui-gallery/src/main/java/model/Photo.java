package model;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import javafx.beans.property.*;
import javafx.scene.image.Image;


public class Photo {

//    private String name;
    private final StringProperty name;
    private final ReadOnlyObjectProperty<Image> photoData;

//    private final Image photoData;

    public Photo(String extension, byte[] photoData) {
        this( UUID.randomUUID() + "." + extension , new Image(new ByteArrayInputStream(photoData)));
    }
    public Photo(String name, Image image){
        this.name = new SimpleStringProperty(name);
        this.photoData = new ReadOnlyObjectWrapper(image);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public Image getPhotoData() {
        return photoData.get();
    }
}
