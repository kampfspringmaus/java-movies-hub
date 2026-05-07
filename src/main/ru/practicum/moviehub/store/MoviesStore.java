package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;
import java.util.stream.Collectors;

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
            return index;
        }


    }

    public Optional<Movie> deleteMovie(int index) {
        return Optional.ofNullable(store.remove(index));
    }

    public boolean containsMovie(int index) {
        return store.containsKey(index);
    }

    public Movie getMovie(int index) {
        return store.get(index);
    }

    public List<String> getMovieByYear (int year) {
        List<String> movies = new ArrayList<>();
        movies = store.values().stream()
                .filter(movie -> movie.getYear() == year)
                .map(movie -> movie.getTitle())
                .collect(Collectors.toList());
        return movies;
    }

}