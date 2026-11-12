package util;

import driver.GoogleSearchDriver;
import io.reactivex.rxjava3.core.Observable;
import model.Photo;
import org.apache.tika.Tika;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Źródło zdjęć jako zimne strumienie RxJava (patrz laboratorium z RxJava). Samo wywołanie metody niczego nie pobiera;
 * praca zaczyna się przy subskrypcji, na wątku wskazanym przez subscribeOn (albo wołającym, gdy go brak).
 */
public class PhotoDownloader {

    private static final Logger log = Logger.getLogger(PhotoDownloader.class.getName());

    static {
        log.setLevel(Level.SEVERE);
    }

    /** Pięć przykładowych adresów; błąd pobrania jednego z nich nie przerywa pozostałych. */
    public Observable<Photo> getPhotoExamples() {
        return Observable.just("https://i.ytimg.com/vi/7uxQjydfBOU/hqdefault.jpg",
                "http://digitalspyuk.cdnds.net/16/51/1280x640/landscape-1482419524-12382542-low-res-sherlock.jpg",
                "http://image.pbs.org/video-assets/pbs/masterpiece/132733/images/mezzanine_172.jpg",
                "https://classicmystery.files.wordpress.com/2016/04/miss-marple-2.jpg",
                "https://i.pinimg.com/736x/7c/14/c9/7c14c97839940a09f987fbadbd47eb89--detective-monk-adrian-monk.jpg")
                .flatMap(this::downloadPhoto);                                 // adres -> strumień 0..1 zdjęć (0, gdy pobranie się nie uda)
    }

    /** Wyszukanie adresów w Serper API, a potem pobranie każdego z nich; błąd wyszukiwania kończy strumień onError. */
    public Observable<Photo> searchForPhotos(String searchQuery) {
        return Observable.fromCallable(() -> GoogleSearchDriver.searchForImages(searchQuery))   // leniwie, przy subskrypcji
                .flatMap(Observable::fromIterable)                                                // List<String> -> kolejne adresy
                .flatMap(this::downloadPhoto);
    }

    /** Pobranie jednego zdjęcia jako strumień: element albo pusty strumień z ostrzeżeniem w logu. */
    private Observable<Photo> downloadPhoto(String photoUrl) {
        return Observable.fromCallable(() -> getPhoto(photoUrl))
                .onErrorResumeNext(error -> {
                    log.log(Level.WARNING, "Could not download a photo: " + photoUrl, error);
                    return Observable.empty();
                });
    }

    private Photo getPhoto(String photoUrl) throws IOException {
        log.info("Downloading... " + photoUrl);
        byte[] photoData = downloadPhotoBytes(photoUrl);
        return createPhoto(photoData);
    }

    private Photo createPhoto(byte[] photoData) throws IOException {
        Tika tika = new Tika();
        String fileType = tika.detect(photoData);
        if (fileType.startsWith("image")) {
            return new Photo(fileType.substring(fileType.indexOf("/") + 1), photoData);
        }
        throw new IOException("Unsupported media type: " + fileType);
    }

    private byte[] downloadPhotoBytes(String url) throws IOException {
        URL photoUrl = toUrl(url);
        URLConnection yc = photoUrl.openConnection();
        yc.setRequestProperty("User-Agent", GoogleSearchDriver.USER_AGENT);
        try (InputStream inputStream = yc.getInputStream()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        }
    }

    /** Zamiast przestarzałego new URL(String); niepoprawny adres zgłaszamy jak każdy inny błąd pobrania. */
    private static URL toUrl(String url) throws IOException {
        try {
            return new URI(url).toURL();
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new IOException("Malformed photo URL: " + url, e);
        }
    }
}
