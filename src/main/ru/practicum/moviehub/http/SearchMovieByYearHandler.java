package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;

public class SearchMovieByYearHandler extends BaseHttpHandler{
    public SearchMovieByYearHandler(MoviesStore store) {
        super(store);
    }
    public void handle(HttpExchange ex) throws IOException {

    }
}
