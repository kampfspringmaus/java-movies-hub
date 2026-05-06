package ru.practicum.moviehub.http;

import com.google.gson.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080"; // Исправлено: http:// → http://
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() throws Exception { // Добавлено throws Exception
        server = new MoviesServer(new MoviesStore(), 8080); // Исправлено: инициализация поля класса, а не локальной переменной
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) { // Защита от NullPointerException
            server.stop();
        }
    }


    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"), // Исправлено: endsWith → endsWith
                "Ожидается JSON-массив");
    }
    @Test
    void whenPostMovieReturns201AndMovieIDIfOk() {
        Movie movie = new Movie("Титаник", 1997);
        Gson gson = new Gson();
        String movieBody = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieBody))
                .build();
        try {
            HttpResponse<String> response = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(201,response.statusCode(),"При добавлении валидного фильма должны получать 201 код");
            //Assertions.assertEquals(201,response.statusCode(),"При добавлении валидного фильма должны получать 201 код");
            //Assertions.assert
            JsonElement jsonElement = JsonParser.parseString(response.body());
            if(!jsonElement.isJsonObject()) { // проверяем, точно ли мы получили JSON-объект
                System.out.println("Ответ от сервера не соответствует ожидаемому.");
                return;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            int index = jsonObject.get("index").getAsInt();
            Assertions.assertEquals(1, index,"при добавлении фильма вернулся неправильный индекс");
            JsonObject movieObject = jsonObject.getAsJsonObject("movie");
            Movie movieResponse = gson.fromJson(movieObject,Movie.class);
            Assertions.assertEquals(movie, movieResponse,"Возвращённый фильм не совпадает с опубликованным");

        } catch (IOException|InterruptedException e) {
            System.out.println(e.getMessage());
        }

    }

    @Test
    void whenPostMovieWithBlankTitleReturns422AndValidationError() {
        Movie movie = new Movie("", 1997);
        Gson gson = new Gson();
        String movieBody = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieBody))
                .build();
        try {
            HttpResponse<String> response = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(422,response.statusCode(),"При добавлении фильма без названия должна возвращаться ошибка");
            JsonElement jsonElement = JsonParser.parseString(response.body());
            if(!jsonElement.isJsonObject()) { // проверяем, точно ли мы получили JSON-объект
                System.out.println("Ответ от сервера не соответствует ожидаемому.");
                return;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String errorDescription = jsonObject.get("shortDescription").getAsString();
            JsonArray detailsArray = jsonObject.get("details").getAsJsonArray();
            String errorMessage = detailsArray.get(0).getAsString();
            Assertions.assertEquals("название не должно быть пустым",errorMessage,"чё-то не то");
           // String errorDetails = jsonObject.get("shortDescription").getAsString();
         //   System.out.println(errorDetails);
            Assertions.assertEquals("Ошибка валидации",errorDescription,"чё-то не то");
            System.out.println("мы тут были");
        } catch (IOException|InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void whenPostMovieWithLongTitleReturns422AndValidationError(){
        Movie movie = new Movie("shalalalalashalalalalashalalalalashalalalalashalalalalashalalalalashalalalalashalalalalashalalalalashalalalalashalalalala", 1997);
        Gson gson = new Gson();
        String movieBody = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieBody))
                .build();
        try {
            HttpResponse<String> response = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(422,response.statusCode(),"При добавлении фильма " +
                    "c названием больше 100 символов должна возвращаться ошибка");
            JsonElement jsonElement = JsonParser.parseString(response.body());
            if(!jsonElement.isJsonObject()) { // проверяем, точно ли мы получили JSON-объект
                System.out.println("Ответ от сервера не соответствует ожидаемому.");
                return;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String errorDescription = jsonObject.get("shortDescription").getAsString();
            JsonArray detailsArray = jsonObject.get("details").getAsJsonArray();
            String errorMessage = detailsArray.get(0).getAsString();
            Assertions.assertEquals("название не может быть таким длинным",errorMessage,"чё-то не то");
            Assertions.assertEquals("Ошибка валидации",errorDescription,"чё-то не то");
            System.out.println("мы тут были длинный фильм");
        } catch (IOException|InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
    @Test
    void whenPostMovieWithWrongYearReturns422AndValidationError(){
        Movie movieOld = new Movie("Titanic", 1886);
        Movie movieYoung = new Movie("Titanic", LocalDate.now().getYear()+2);
        Gson gson = new Gson();
        String movieBodyOld = gson.toJson(movieOld);
        String movieBodyYoung = gson.toJson(movieYoung);

        HttpRequest reqOld = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieBodyOld))
                .build();
        HttpRequest reqYoung = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieBodyYoung))
                .build();
        try {
            HttpResponse<String> responseOld = client.send(reqOld,
                    HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> responseYoung = client.send(reqYoung,
                    HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(422,responseOld.statusCode(),"При добавлении фильма раньше 1888 года должна возвращаться ошибка");
            Assertions.assertEquals(422,responseYoung.statusCode(),"При добавлении фильма позже текущего года +1 год должна возвращаться ошибка");
            JsonElement jsonElementOld = JsonParser.parseString(responseOld.body());
            JsonElement jsonElementYoung = JsonParser.parseString(responseYoung.body());

            if(!jsonElementOld.isJsonObject()||!jsonElementYoung.isJsonObject()) { // проверяем, точно ли мы получили JSON-объект
                System.out.println("Ответ от сервера не соответствует ожидаемому.");
                return;
            }

            JsonObject jsonObjectOld = jsonElementOld.getAsJsonObject();
            JsonObject jsonObjectYoung = jsonElementYoung.getAsJsonObject();

            String errorDescriptionOld = jsonObjectOld.get("shortDescription").getAsString();
            String errorDescriptionYoung = jsonObjectYoung.get("shortDescription").getAsString();
            JsonArray detailsArrayOld = jsonObjectOld.get("details").getAsJsonArray();
            JsonArray detailsArrayYoung = jsonObjectYoung.get("details").getAsJsonArray();
            String errorMessageOld = detailsArrayOld.get(0).getAsString();
            String errorMessageYoung = detailsArrayYoung.get(0).getAsString();
            Assertions.assertEquals("год должен быть между 1888 и "+ LocalDate.now().getYear()+1,errorMessageOld,"чё-то не то");
            Assertions.assertEquals("год должен быть между 1888 и "+ LocalDate.now().getYear()+1,errorMessageYoung,"чё-то не то");
            // String errorDetails = jsonObject.get("shortDescription").getAsString();
            //   System.out.println(errorDetails);
            Assertions.assertEquals("Ошибка валидации",errorDescriptionOld,"чё-то не то");
            Assertions.assertEquals("Ошибка валидации",errorDescriptionYoung,"чё-то не то");
            System.out.println("мы тут были");
        } catch (IOException|InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
    @Test
    void whenPostMovieWithIncorrectContentTypeReturns415AndUnsupportedMediaType() {
        Movie movie = new Movie("Titanic", 1997);
        Gson gson = new Gson();
        String movieBody = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(movieBody))
                .build();
        try {
            HttpResponse<String> response = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(415, response.statusCode(), "При неправильном Content Type нужна 415 ошибка");
            //System.out.println("response "+ response.statusCode());
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void whenPostMovieWithIncorrectJsonReturns422AndValidationError() {
        HashMap<String, String> movie = new HashMap();
        movie.put("Titanic", "1997");
        Gson gson = new Gson();
        String movieBody = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(movieBody))
                .build();
        try {
            HttpResponse<String> response = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(422, response.statusCode(), "При неправильном JSON должна возвращаться 422 ошибка");
            //System.out.println("response "+ response.statusCode());
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
    /*@Test
    void whenPostMovieReturns422AndErrorDetailsIfValidationError() {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST()
                .build();

    }*/
}