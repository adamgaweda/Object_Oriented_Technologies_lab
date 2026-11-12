import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.junit.jupiter.api.Test;
import util.Color;

import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

import static util.ColorUtil.print;
import static util.ColorUtil.printThread;

public class RxTests {

    private static final String MOVIES1_DB = "movies1";

    private static final String MOVIES2_DB = "movies2";

    /**
     * Example 1: Creating and subscribing observable from iterable.
     */
    @Test
    public void loadMoviesAsList() throws FileNotFoundException {
        var movieReader = new MovieReader();

        movieReader.getMoviesFromList(MOVIES1_DB)
                .subscribe(movie -> print(movie, Color.GREEN));
    }

    /**
     * Example 2: Creating and subscribing observable from custom emitter.
     */
    @Test
    public void loadMoviesAsStream() {
        var movieReader = new MovieReader();

        movieReader.getMoviesAsStream(MOVIES1_DB)
                .subscribe(movie -> print(movie, Color.GREEN));
    }

    /**
     * Example 3: Handling errors.
     */
    @Test
    public void loadMoviesAsStreamAndHandleError() {
        var movieReader = new MovieReader();

        movieReader.getMoviesAsStream("unknown.csv")
                .subscribe(movie -> print(movie, Color.GREEN),
                        error -> print("Nie pyklo " + error, Color.MAGENTA));
    }

    /**
     * Example 4: Signaling end of a stream.
     */
    @Test
    public void loadMoviesAsStreamAndFinishWithMessage() {
        var movieReader = new MovieReader();

        movieReader.getMoviesAsStream(MOVIES1_DB)
                .take(10)
                .subscribe(movie -> print(movie, Color.GREEN),
                        error -> print("Nie pyklo " + error, Color.MAGENTA),
                        () -> print("To juz jest koniec", Color.BLUE));
    }

    /**
     * Example 5: Filtering stream data.
     */
    @Test
    public void displayLongMovies() {
        var movieReader = new MovieReader();

        movieReader.getMoviesAsStream(MOVIES1_DB)
                .filter(movie -> movie.getLength() > 150)
                .subscribe(movie -> print(movie, Color.GREEN));
    }

    /**
     * Example 6: Transforming stream data.
     */
    @Test
    public void displaySortedMoviesTitles() {
        var movieReader = new MovieReader();

        movieReader.getMoviesAsStream(MOVIES1_DB)
                .map(Movie::getTitle)
                .sorted()
                .take(10)
                .subscribe(title -> print(title, Color.GREEN));

    }

    /**
     * Example 7: Monads are like burritos.
     */
    @Test
    public void displayActorsForMovies() {
        var movieReader = new MovieReader();

        movieReader.getMoviesAsStream(MOVIES1_DB)
                .flatMap(movie -> Observable.fromIterable(movieReader.readActors(movie)))
                .distinct()
                .sorted()
                .subscribe(actor -> print(actor, Color.GREEN));
    }

    /**
     * Example 8: Combining observables.
     */
    @Test
    public void loadMoviesFromManySources() {
        var movieReader = new MovieReader();

        Observable<Movie> movies1 = movieReader.getMoviesAsStream(MOVIES1_DB)
                .doOnNext(movie -> print(movie, Color.RED));

        Observable<Movie> movies2 = movieReader.getMoviesAsStream(MOVIES2_DB)
                .doOnNext(movie -> print(movie, Color.BLUE));

        Observable.merge(movies1, movies2)
                .subscribe(movie -> print("RECEIVED: " + movie, Color.GREEN));
    }

    /**
     * Example 9: Playing with threads (subscribeOn).
     */
    @Test
    public void loadMoviesInBackground() throws InterruptedException {
        var movieReader = new MovieReader();

        movieReader.getMoviesAsStream(MOVIES1_DB)
                .subscribeOn(Schedulers.newThread())
                .subscribe(movie -> printThread(movie.getIndex(), Color.GREEN));

        printThread("po wszystkim", Color.MAGENTA);
        Thread.sleep(10000);
    }

    /**
     * Example 10: Playing with threads (observeOn).
     */
    @Test
    public void switchThreadsDuringMoviesProcessing() {
        var movieReader = new MovieReader();

        movieReader.getMoviesAsStream(MOVIES1_DB)
                .take(10)
                .subscribeOn(Schedulers.io())
                .doOnNext(movie -> printThread(movie.getIndex(), Color.RED))
                .observeOn(Schedulers.io())
                .doOnNext(movie -> printThread(movie.getIndex(), Color.BLUE))
                .blockingSubscribe(movie -> printThread(movie.getIndex(), Color.GREEN));

        printThread("po wszystkim", Color.MAGENTA);
    }

    /**
     * Example 11: Combining parallel streams.
     */
    @Test
    public void loadMoviesFromManySourcesParallel() {
        // Static merge solution
        var movieReader = new MovieReader();

//        Observable<Movie> movies1 = movieReader.getMoviesAsStream(MOVIES1_DB)
//                .subscribeOn(Schedulers.newThread())
//                .doOnNext(movie -> print(movie, Color.RED));
//
//        Observable<Movie> movies2 = movieReader.getMoviesAsStream(MOVIES2_DB)
//                .subscribeOn(Schedulers.newThread())
//                .doOnNext(movie -> print(movie, Color.BLUE));
//
//        Observable.merge(movies1, movies2)
//                .blockingSubscribe(movie -> print("RECEIVED: " + movie, Color.GREEN));


//        // FlatMap solution:
        final MovieDescriptor movie1Descriptor = new MovieDescriptor(MOVIES1_DB, Color.RED);
        final MovieDescriptor movie2Descriptor = new MovieDescriptor(MOVIES2_DB, Color.BLUE);

        Observable.just(movie1Descriptor, movie2Descriptor)
                .flatMap(movieDescriptor -> movieReader.getMoviesAsStream(movieDescriptor.movieDbFilename)
                        .doOnNext(movie -> print(movie, movieDescriptor.debugColor))
                        .subscribeOn(Schedulers.newThread())
                )
                .blockingSubscribe(movie -> print("RECEIVED: " + movie, Color.GREEN));
    }

    /**
     * Example 12: Zip operator.
     */
    @Test
    public void loadMoviesWithDelay() {
        MovieReader movieReader = new MovieReader();

        Observable<Movie> movieObservable = movieReader.getMoviesAsStream(MOVIES1_DB)
                .subscribeOn(Schedulers.newThread());

        Observable<Long> interval = Observable.interval(1, TimeUnit.SECONDS);
        
        Observable.zip(movieObservable, interval, (movie, tick) -> movie)
                .blockingSubscribe(movie -> print(movie, Color.GREEN));
    }

    /**
     * Example 13: Backpressure.
     */
    @Test
    public void trackMoviesLoadingWithBackpressure() {
        MovieReader movieReader = new MovieReader();
        
        movieReader.getMoviesAsStream(MOVIES1_DB)
                .doOnNext(movie -> Thread.sleep(10))
                .doOnNext(movie -> print(movie, Color.RED))
                .subscribeOn(Schedulers.io())
                .toFlowable(BackpressureStrategy.LATEST)
                .observeOn(Schedulers.io(), true, 1)
                .doOnNext(this::displayProgress)
                .blockingSubscribe();
    }

    /**
     * Example 14: Cold and hot observables.
     */
    @Test
    public void oneMovieStreamManyDifferentSubscribers() {
        MovieReader movieReader = new MovieReader();

        Observable<Movie> coldObservable = movieReader.getMoviesAsStream(MOVIES1_DB);

        ConnectableObservable<Movie> hotObservable = coldObservable.publish();
        
        hotObservable
                .take(10)
                .subscribe(movie -> print(movie, Color.RED));
        
        hotObservable
                .filter(movie -> movie.getRating().equals("R"))
                .subscribe(movie -> print(movie, Color.BLUE));
        
        hotObservable.connect();

    }

    /**
     * Example 15: Caching observables (hot-cold hybrid).
     */
    @Test
    public void cacheMoviesInfo() {
        MovieReader movieReader = new MovieReader();

        Observable<Movie> cachedMovies = movieReader.getMoviesAsStream(MOVIES1_DB).cache();
        
        cachedMovies.subscribe(movie -> print(movie, Color.RED));
        
        IO.println(cachedMovies.count().blockingGet());
    }

    private void displayProgress(Movie movie) throws InterruptedException {
        print((movie.getIndex() / 500.0 * 100) + "%", Color.GREEN);
        Thread.sleep(50);
    }

    private class MovieDescriptor {
        private final String movieDbFilename;

        private final Color debugColor;

        private MovieDescriptor(String movieDbFilename, Color debugColor) {
            this.movieDbFilename = movieDbFilename;
            this.debugColor = debugColor;
        }

        public Color getDebugColor() {
            return debugColor;
        }

        public String getMovieDbFilename() {
            return movieDbFilename;
        }
    }
}
