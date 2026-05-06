package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;

public class CreateMovieHandler extends BaseHttpHandler {
    public CreateMovieHandler(MoviesStore store) {
        super(store);
    }
    @Override
    public void handle(HttpExchange ex) throws IOException {

    }
}
