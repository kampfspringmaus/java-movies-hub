package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public class MoviesHandler extends BaseHttpHandler {


    public MoviesHandler(MoviesStore store) {
        super(store);


    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        ArrayList<String> potentialErrors = new ArrayList<>();
        Gson gson = new Gson();


        if (method.equalsIgnoreCase("GET")) {
            String path = ex.getRequestURI().getPath();
            String query = ex.getRequestURI().getQuery();
            if (path.equals("/movies") && query == null) {
                List<Movie> result = store.getAllMovies();
                if (result.size() == 0) {
                    sendJson(ex, 200, "[]");
                } else {
                    String json = gson.toJson(result);
                    sendJson(ex, 200, json);
                }
                //Эндпоинт GET /movies?year=YYYY
            } else if (path.equals("/movies") && query.matches("year=.*")) {
                String yearPath = query.substring("year=".length());

                int year = -1;
                try {
                    year = Integer.parseInt(yearPath);
                } catch (NumberFormatException e) {
                    String json = gson.toJson("Некорректный параметр запроса — 'year'");
                    sendJson(ex, 400, json);
                }

                if (year >= 1888 && year <= LocalDate.now().getYear() + 1) {
                    List<String> movies = store.getMovieByYear(year);

                    String json = gson.toJson(movies);
                    sendJson(ex, 200, json);
                } else {
                    String json = gson.toJson("Некорректный параметр запроса — 'year'");
                    sendJson(ex, 400, json);
                }

                //Эндпоинт  GET /movies/{id}
            } else if (path.matches("/movies/.+")) {
                String requestPath = path.substring("/movies/".length());
                int movieId = -1;
                try {
                    movieId = Integer.parseInt(requestPath);
                } catch (NumberFormatException e) {
                    String json = gson.toJson("Некорректный ID");
                    sendJson(ex, 400, json);
                }
                if (movieId > 0) {
                    if (store.containsMovie(movieId)) {
                        Movie movie = store.getMovie(movieId);
                        String json = gson.toJson(movie);
                        sendJson(ex, 200, json);
                    } else {
                        String json = gson.toJson("Фильм не найден");
                        sendJson(ex, 404, json);
                    }
                }

            } else {
                sendJson(ex, 404, "wrong request");
            }
            //НАЧАЛО ПОСТА
        } else if (method.equalsIgnoreCase("POST")) {
            InputStream inputStream = ex.getRequestBody();
            String requestBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            try {
                Movie movie = gson.fromJson(requestBody, Movie.class);
                //данные для проверки content-type
                Headers headers = ex.getRequestHeaders();
                String contentType = headers.getFirst("Content-Type");
                // обработка ошибок добавления фильма
                if (movie.getTitle().isEmpty()) {
                    potentialErrors.add("название не должно быть пустым");
                }

                if (movie.getYear() < 1888 || movie.getYear() > LocalDate.now().getYear() + 1) {
                    potentialErrors.add("год должен быть между 1888 и " + LocalDate.now().getYear() + 1);
                }

                if (movie.getTitle().length() > 100) {
                    potentialErrors.add("название не может быть таким длинным");
                }

                if (potentialErrors.size() > 0) {
                    ErrorResponse errors = new ErrorResponse("Ошибка валидации", potentialErrors.toArray(new String[potentialErrors.size()]));
                    String json = gson.toJson(errors);
                    sendJson(ex, 422, json);

                } else if (contentType != null && !contentType.equals("application/json")) {
                    sendJson(ex, 415, "");
                } else {
                    int index = store.addMovie(movie.getTitle(), movie.getYear());

                    Map<String, Object> combined = new HashMap<>();
                    combined.put("index", index);
                    combined.put("movie", movie);
                    String json = gson.toJson(combined);
                    sendJson(ex, 201, json);
                }
            } catch (JsonParseException e) {
                sendJson(ex, 422, "");

            }
        } else if (method.equalsIgnoreCase("DELETE")) {
            String requestPath = ex.getRequestURI().getPath();
            int id = Integer.parseInt(requestPath.substring(requestPath.lastIndexOf('/') + 1));
            Optional<Movie> movie = store.deleteMovie(id);
            if (movie.isPresent()) {
                sendNoContent(ex);
            } else {
                sendJson(ex, 404, "");
            }
        } else {
            sendJson(ex, 405, "");
        }
    }
}
