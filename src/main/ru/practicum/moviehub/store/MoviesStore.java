package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MoviesStore {
    Map<Integer, Movie> store;
    int index;

    public MoviesStore() {
        this.store = new HashMap<>();
        this.index = 0;
    }

    public int addMovie(String title, int year) {
        Movie movie = new Movie(title,year);
        if (store.containsValue(movie)) {
            return -1;
        } else {
            index++;
            store.put(index, movie);
            // int resultIndex = index;
            //System.out.println("resultIndex = " + resultIndex);
            System.out.println("index = "+ index);
            System.out.println("вот такой список фильмов: \n"+store.keySet());
            return index;
        }


    }

    public Optional<Movie> deleteMovie(int index) {
        System.out.println("вот такой список фильмов: \n"+store.keySet());
        return Optional.ofNullable(store.remove(index));
    }

    public boolean containsMovie(int index) {
        return store.containsKey(index);
    }

    public Movie getMovie(int index) {
        return store.get(index);
    }

}