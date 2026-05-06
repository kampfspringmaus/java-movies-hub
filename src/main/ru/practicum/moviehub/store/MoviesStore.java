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
        //Movie movie = ;
        index++;
        store.put(index, new Movie(title,year));
        // int resultIndex = index;
        //System.out.println("resultIndex = " + resultIndex);
        System.out.println("index = "+ index);
        return index;
    }

    Optional<Movie> deleteMovie(int index) {
        return Optional.of(store.remove(index));
    }

}