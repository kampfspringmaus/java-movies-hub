package ru.practicum.moviehub.http;

import com.google.gson.*;
import org.junit.jupiter.api.*;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() throws Exception {
        server = new MoviesServer(new MoviesStore(), 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }
/*
Дорогой Вячеслав (надеюсь, это именно ты), прости, не знаю, как ещё написать тебе человеческие слова,
поэтому напишу здесь. Видит бог, я очень чётко осознаю, что это не лучший код в моей жизни. Он совершенно точно
рабочий, хотя тут прорва того, что можно оптимизировать, чем я и займусь в ближайшее время. Буду тебе благодарен
за любые замечания. Просто заранее хотел себя обелить в части того, что понимаю, что повторяемость кода драматичная,
и я займусь оптимизацией этого в любом случае.
Просто обстоятельства складываются так, что кодить я в ближайшие 4 дня не смогу, а доступ к теории следующего спринта
хотелось бы иметь. Поэтому сдаю довольно сырую, но рабочую версию.
Отдельно буду очень благодарен, если ты пояснишь, зачем бы мне мог понадобиться ListOfMoviesTypeToken. Я не нашёл
ему применения.
С наступающим тебя Днём Победы и хороших выходных!
Ура-Ура-Ура!
*/

    @AfterAll
    static void afterAll() {
        if (server != null) { // Защита от NullPointerException
            server.stop();
        }
    }


    @Test
    @Order(1)
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
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    @Order(4)
    void whenGetMoviesAndStoreNotEmptyReturn200AndMovieArray() {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(200, response.statusCode(), "GET /movies должен вернуть 200");
            JsonArray movieArray = JsonParser.parseString(response.body()).getAsJsonArray();
            assertEquals(2, movieArray.size(), "Количество фильмов в хранилище не совпадает с ожидаемым");
        } catch (IOException | InterruptedException e) {
            System.out.println("whenGetMoviesAndStoreNotEmptyReturn200AndMovieArray " + e.getMessage());
        }
    }

    @Test
    @Order(2)
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
            Assertions.assertEquals(201, response.statusCode(), "При добавлении валидного фильма должны получать 201 код");
            JsonElement jsonElement = JsonParser.parseString(response.body());
            if (!jsonElement.isJsonObject()) {
                System.out.println("whenPostMovieReturns201AndMovieIDIfOk: Ответ от сервера не соответствует ожидаемому.");
                return;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            int index = jsonObject.get("index").getAsInt();
            Assertions.assertEquals(1, index, "при добавлении фильма вернулся неправильный индекс");
            JsonObject movieObject = jsonObject.getAsJsonObject("movie");
            Movie movieResponse = gson.fromJson(movieObject, Movie.class);
            Assertions.assertEquals(movie, movieResponse, "Возвращённый фильм не совпадает с опубликованным");

        } catch (IOException | InterruptedException e) {
            System.out.println("whenPostMovieReturns201AndMovieIDIfOk " + e.getMessage());
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
            Assertions.assertEquals(422, response.statusCode(), "При добавлении фильма без названия должна возвращаться ошибка");
            JsonElement jsonElement = JsonParser.parseString(response.body());
            if (!jsonElement.isJsonObject()) {
                System.out.println("whenPostMovieWithBlankTitleReturns422AndValidationError: Ответ от сервера не соответствует ожидаемому.");
                return;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String errorDescription = jsonObject.get("shortDescription").getAsString();
            JsonArray detailsArray = jsonObject.get("details").getAsJsonArray();
            String errorMessage = detailsArray.get(0).getAsString();
            Assertions.assertEquals("название не должно быть пустым", errorMessage, "чё-то не то");
            Assertions.assertEquals("Ошибка валидации", errorDescription, "чё-то не то");
        } catch (IOException | InterruptedException e) {
            System.out.println("whenPostMovieWithBlankTitleReturns422AndValidationError " + e.getMessage());
        }
    }

    @Test
    void whenPostMovieWithLongTitleReturns422AndValidationError() {
        Movie movie = new Movie("shalalalalashalalalalashalalalalashalalalalashalalalalashalalalalasha" +
                "lalalalashalalalalashalalalalashalalalalashalalalala", 1997);
        Gson gson = new Gson();
        String movieBody = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieBody))
                .build();
        try {
            HttpResponse<String> response = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(422, response.statusCode(), "При добавлении фильма " +
                    "c названием больше 100 символов должна возвращаться ошибка");
            JsonElement jsonElement = JsonParser.parseString(response.body());
            if (!jsonElement.isJsonObject()) {
                System.out.println("whenPostMovieWithLongTitleReturns422AndValidationError: Ответ от сервера не соответствует ожидаемому.");
                return;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String errorDescription = jsonObject.get("shortDescription").getAsString();
            JsonArray detailsArray = jsonObject.get("details").getAsJsonArray();
            String errorMessage = detailsArray.get(0).getAsString();
            Assertions.assertEquals("название не может быть таким длинным", errorMessage, "чё-то не то");
            Assertions.assertEquals("Ошибка валидации", errorDescription, "чё-то не то");
        } catch (IOException | InterruptedException e) {
            System.out.println("whenPostMovieWithLongTitleReturns422AndValidationError " + e.getMessage());
        }
    }

    @Test
    void whenPostMovieWithWrongYearReturns422AndValidationError() {
        Movie movieOld = new Movie("Titanic", 1886);
        Movie movieYoung = new Movie("Titanic", LocalDate.now().getYear() + 2);
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
            Assertions.assertEquals(422, responseOld.statusCode(), "При добавлении фильма раньше 1888 года должна возвращаться ошибка");
            Assertions.assertEquals(422, responseYoung.statusCode(), "При добавлении фильма позже текущего года +1 год должна возвращаться ошибка");
            JsonElement jsonElementOld = JsonParser.parseString(responseOld.body());
            JsonElement jsonElementYoung = JsonParser.parseString(responseYoung.body());

            if (!jsonElementOld.isJsonObject() || !jsonElementYoung.isJsonObject()) {
                System.out.println("whenPostMovieWithWrongYearReturns422AndValidationError: Ответ от сервера не соответствует ожидаемому.");
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
            Assertions.assertEquals("год должен быть между 1888 и " + (LocalDate.now().getYear() + 1), errorMessageOld, "Неверный текст сообщения при слишком маленьком годе выпуска фильма");
            Assertions.assertEquals("год должен быть между 1888 и " + (LocalDate.now().getYear() + 1), errorMessageYoung, "Неверный текст сообщения при слишком большом годе выпуска фильма");
            Assertions.assertEquals("Ошибка валидации", errorDescriptionOld, "чё-то не то");
            Assertions.assertEquals("Ошибка валидации", errorDescriptionYoung, "чё-то не то");
        } catch (IOException | InterruptedException e) {
            System.out.println("whenPostMovieWithWrongYearReturns422AndValidationError " + e.getMessage());
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
        } catch (IOException | InterruptedException e) {
            System.out.println("whenPostMovieWithIncorrectContentTypeReturns415AndUnsupportedMediaType " + e.getMessage());
        }
    }

    @Test
    void whenPostMovieWithIncorrectJsonReturns422AndValidationError() {
        Gson gson = new Gson();
        String movieBody = gson.toJson("{ \"title\": \"Фильм \"год\"\" }");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieBody))
                .build();
        try {
            HttpResponse<String> response = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(422, response.statusCode(), "При неправильном JSON должна возвращаться 422 ошибка");
        } catch (IOException | InterruptedException e) {
            System.out.println("whenPostMovieWithIncorrectJsonReturns422AndValidationError " + e.getMessage());
        }
    }

    //Получение фильма по идентификатору
    @Test
    @Order(3)
    void whenMoviefound200AndJsonWithMovie() {
        Movie movie = new Movie("Барбариска", 1997);
        Gson gson = new Gson();
        String movieBody = gson.toJson(movie);
        int index = 0;

        HttpRequest reqPost = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieBody))
                .build();
        try {
            HttpResponse<String> responsePost = client.send(reqPost,
                    HttpResponse.BodyHandlers.ofString());
            JsonElement jsonElement = JsonParser.parseString(responsePost.body());
            if (!jsonElement.isJsonObject()) {
                System.out.println("whenMoviefound200AndJsonWithMovie: Ответ от сервера не соответствует ожидаемому.");
                return;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            index = jsonObject.get("index").getAsInt();

        } catch (IOException | InterruptedException e) {
            System.out.println("whenMoviefound200AndJsonWithMovie " + e.getMessage());
        }
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + index))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(200, response.statusCode(), "При поиске по ID существующего фильма должно возвращаться 200");
        } catch (IOException | InterruptedException e) {
            System.out.println("whenMoviefound200AndJsonWithMovie " + e.getMessage());
        }
    }

    @Test
    void whenMovieNotFound404AndBodyWithMessage() {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/117"))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(404, response.statusCode(), "При запросе несуществующего фильма должна вернуться 404 ошибка");

            String responseBody = JsonParser.parseString(response.body()).getAsString();
            Assertions.assertEquals("Фильм не найден", responseBody, "При запросе несуществующего фильма должна вернуться ошибка Фильм не найден");
        } catch (IOException | InterruptedException e) {
            System.out.println("whenMovieNotFound404AndBodyWithMessage " + e.getMessage());
        }
    }

    @Test
    void whenMovieIdIsNotNumber400AndBodyWithMessage() {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1As17"))
                .build();
        try {
            HttpResponse<String> response = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(400, response.statusCode(), "При запросе по ID, не являющемуся числом" +
                    "должна вернуться 400 ошибка");
            String responseBody = JsonParser.parseString(response.body()).getAsString();
            Assertions.assertEquals("Некорректный ID", responseBody, "При запросе некорректного ID должна вернуться ошибка Некорректный ID");
        } catch (IOException | InterruptedException e) {
            System.out.println("whenMovieIdIsNotNumber400AndBodyWithMessage " + e.getMessage());
        }
    }

    //Удаление фильма
    @Test
    void whenDeleteNonExistentMovieGet404() {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/172"))
                .DELETE()
                .build();
        try {
            HttpResponse<String> response = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(404, response.statusCode(), "При удалении несуществующего фильма должна возвращаться 404 ошибка");
        } catch (IOException | InterruptedException e) {
            System.out.println("whenDeleteNonExistentMovieGet404 " + e.getMessage());
        }
    }

    @Test
    void whenDeleteExistedMovieGet204() {
        Movie movie = new Movie("Бабуся", 1997);
        Gson gson = new Gson();
        String movieBody = gson.toJson(movie);
        int index = 0;

        HttpRequest reqPost = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieBody))
                .build();
        try {
            HttpResponse<String> responsePost = client.send(reqPost,
                    HttpResponse.BodyHandlers.ofString());
            JsonElement jsonElement = JsonParser.parseString(responsePost.body());
            if (!jsonElement.isJsonObject()) {
                System.out.println("whenDeleteExistedMovieGet204: Ответ от сервера не соответствует ожидаемому.");
                return;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            index = jsonObject.get("index").getAsInt();

        } catch (IOException | InterruptedException e) {
            System.out.println("whenDeleteExistedMovieGet204 " + e.getMessage());
        }
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + index))
                .DELETE()
                .build();
        try {
            HttpResponse<String> response = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(204, response.statusCode(), "При удалении существующего фильма должен возвращаться код 204");
        } catch (IOException | InterruptedException e) {
            System.out.println("whenDeleteExistedMovieGet204 " + e.getMessage());
        }
    }

    //Поиск фильма по году выпуска
    @Test
    void whenWrongYearOrYearNotNumber400() {
        HttpRequest reqWrongYear = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1666"))
                .GET()
                .build();
        HttpRequest reqNotNumber = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=YYYY"))
                .GET()
                .build();
        try {
            HttpResponse<String> responseWrongYear = client.send(reqWrongYear,
                    HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> responseNotNumber = client.send(reqNotNumber,
                    HttpResponse.BodyHandlers.ofString());
            String responseBodyWrongYear = JsonParser.parseString(responseWrongYear.body()).getAsString();
            String responseBodyNotNumber = JsonParser.parseString(responseNotNumber.body()).getAsString();
            Assertions.assertEquals(400, responseWrongYear.statusCode(), "При годах раньше 1888 и позже текущего года +1 должна быть 400 ошибка");
            Assertions.assertEquals(400, responseNotNumber.statusCode(), "При указании года, не являющегося числом, должна быть 400 ошибка");
            Assertions.assertEquals("Некорректный параметр запроса — 'year'", responseBodyWrongYear, "При годах раньше 1888 и позже текущего года +1, должно быть сообщение о неверном параметре запроса");
            Assertions.assertEquals("Некорректный параметр запроса — 'year'", responseBodyNotNumber, "При указании года, не являющегося числом, должно быть сообщение о неверном параметре запроса");

        } catch (IOException | InterruptedException e) {
            System.out.println("whenWrongYearOrYearNotNumber400 " + e.getMessage());
        }

    }

    @Test
    void whenNoMoviesInThisYearReturns200AndBlankResponse() {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2017"))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            JsonArray movieArray = JsonParser.parseString(response.body()).getAsJsonArray();
            Assertions.assertEquals(200, response.statusCode(), "При отсутствии фильмов, но валидном году возвращается 200");
            Assertions.assertEquals(0, movieArray.size(), "При отсутствии фильмов, но валидном году возвращается нулевой массив");

        } catch (IOException | InterruptedException e) {
            System.out.println("whenYear2015Returns200AndOneMovie " + e.getMessage());
        }

    }

    @Test
    void whenYear2015Returns200AndOneMovie() {
        Movie movie1 = new Movie("Очпочмок", 2015);
        Movie movie2 = new Movie("Чивапчи", 2015);
        Gson gson = new Gson();
        String movieBody1 = gson.toJson(movie1);
        String movieBody2 = gson.toJson(movie2);

        HttpRequest reqPost1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieBody1))
                .build();
        HttpRequest reqPost2 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(movieBody2))
                .build();
        try {
            HttpResponse<String> responsePost1 = client.send(reqPost1,
                    HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> responsePost2 = client.send(reqPost2,
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            System.out.println("whenYear2015Returns200AndOneMovie" + e.getMessage());
        }

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2015"))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            JsonArray movieArray = JsonParser.parseString(response.body()).getAsJsonArray();
            List<String> movieTitles = gson.fromJson(movieArray, List.class);
            Assertions.assertEquals(200, response.statusCode(), "При наличии фильма в этом году возвращается код 200");
            Assertions.assertEquals("Чивапчи", movieTitles.get(1), "второй элемент массива должен быть Чивапчи");
        } catch (IOException | InterruptedException e) {
            System.out.println("whenYear2015Returns200AndOneMovie " + e.getMessage());
        }
    }
}