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
    Gson gson;
    private final int maxYear = LocalDate.now().getYear() + 1;
    private final int minYear = 1888;

    public MoviesHandler(MoviesStore store) {
        super(store);
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        ArrayList<String> potentialErrors = new ArrayList<>();


        if (method.equalsIgnoreCase("GET")) {
            String path = ex.getRequestURI().getPath();
            String query = ex.getRequestURI().getQuery();
            if (path.equals("/movies") && query == null) {
                List<Movie> result = store.getAllMovies();
                    String json = gson.toJson(result);
                    sendJson(ex, 200, json);
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

                if (year >= minYear && year <= maxYear) {
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

                if (movie.getYear() < minYear || movie.getYear() > maxYear) {
                    potentialErrors.add("год должен быть между " + minYear + " и " + maxYear);
                }

                if (movie.getTitle().length() > 100) {
                    potentialErrors.add("название не может быть таким длинным");
                }

                if (potentialErrors.size() > 0) {
                    ErrorResponse errors = new ErrorResponse("Ошибка валидации", potentialErrors);
                    String json = gson.toJson(errors);
                    sendJson(ex, 422, json);

                } else if (contentType != null && !contentType.equals("application/json")) {
                    sendJson(ex, 415, "Unsupported Media Type");
                } else {
                    int index = store.addMovie(movie.getTitle(), movie.getYear());

                    Map<String, Object> combined = new HashMap<>();
                    combined.put("index", index);
                    combined.put("movie", movie);
                    String json = gson.toJson(combined);
                    sendJson(ex, 201, json);
                }
            } catch (JsonParseException e) {
                sendJson(ex, 422, "Unprocessable Entity");
            }
        } else if (method.equalsIgnoreCase("DELETE")) {
            String requestPath = ex.getRequestURI().getPath();
            int id = Integer.parseInt(requestPath.substring(requestPath.lastIndexOf('/') + 1));
            Optional<Movie> movie = store.deleteMovie(id);
            if (movie.isPresent()) {
                sendNoContent(ex);
            } else {
                sendJson(ex, 404, "Not Found");
            }
        } else {
            sendJson(ex, 405, "Method Not Allowed");
        }
    }
}
