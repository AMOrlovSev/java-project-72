package hexlet.code;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.repository.BaseRepository;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.rendering.template.JavalinJte;
import io.javalin.testtools.JavalinTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;

import static hexlet.code.App.createTemplateEngine;
import static hexlet.code.App.readResourceFile;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
public class TestApp {
    private static final String TEST_DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private Javalin appTest;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(TEST_DB_URL);

        var dataSource = new HikariDataSource(hikariConfig);

        var sql = readResourceFile("schema.sql");
        BaseRepository.dataSource = dataSource;

        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute(sql);
        }

        appTest = Javalin.create(config -> {
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinJte(createTemplateEngine()));
        });

        appTest.before(ctx -> {
            ctx.contentType("text/html; charset=utf-8");
        });

        appTest.get("/", hexlet.code.controller.RootController::index);
        appTest.get("/urls", hexlet.code.controller.UrlsController::index);
        appTest.post("/urls", hexlet.code.controller.UrlsController::create);
        appTest.get("/urls/{id}", hexlet.code.controller.UrlsController::show);
    }

    @Test
    public void testMainPage() {
        JavalinTest.test(appTest, (server, client) -> {
            var response = client.get("/");
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(response.body().string()).contains("Анализатор страниц");
        });
    }

    @Test
    void testUrlsPageEmpty() {
        JavalinTest.test(appTest, (server, client) -> {
            var response = client.get("/urls");
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(response.body().string())
                    .contains("Сайты")
                    .contains("Список пуст");
        });
    }

    @Test
    void testCreateUrlSuccess() {
        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=https://example.com";

            var response = client.post("/urls", formData);

            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());

            var redirectResponse = client.get("/urls");
            var responseBody = redirectResponse.body().string();
            assertThat(responseBody).contains("https://example.com");
        });
    }

    @Test
    void testCreateUrlDuplicate() {
        JavalinTest.test(appTest, (server, client) -> {
            String formData1 = "url=https://example.com";
            client.post("/urls", formData1);

            String formData2 = "url=https://example.com";
            var response = client.post("/urls", formData2);

            assertThat(response.code()).isEqualTo(HttpStatus.CONFLICT.getCode());

            String body = response.body().string();
            System.out.println("Body contains flash: " + body.contains("Страница уже существует"));
            System.out.println("Full body: " + body);

            assertThat(body).contains("Страница уже существует");
        });
    }

    @Test
    void testCreateUrlInvalid() {
        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=invalid-url";

            var response = client.post("/urls", formData);
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(response.body().string()).contains("Некорректный URL");

//            var mainResponse = client.get("/");
//            assertThat(mainResponse.body().string()).contains("Некорректный URL");
        });
    }

    @Test
    void testCreateUrlRelative() {
        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=/relative/path";

            var response = client.post("/urls", formData);
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());

            var mainResponse = client.get("/");
            assertThat(mainResponse.body().string()).contains("URL must be absolute");
        });
    }

    @Test
    void testShowUrl() {
        JavalinTest.test(appTest, (server, client) -> {
            // Сначала добавляем URL через форму
            String formData = "url=https://example.com";
            client.post("/urls", formData);

            // Получаем список URL чтобы узнать ID
            var urlsResponse = client.get("/urls");
            var urlsBody = urlsResponse.body().string();

            // Ищем ID в HTML (простая проверка что страница отображается)
            assertThat(urlsBody).contains("https://example.com");

            // Проверяем что есть ссылка на детальную страницу
            assertThat(urlsBody).contains("/urls/");
        });
    }

    @Test
    void testShowUrlNotFound() {
        JavalinTest.test(appTest, (server, client) -> {
            var response = client.get("/urls/999");
            assertThat(response.code()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
        });
    }

    @Test
    void testUrlsPageWithData() {
        JavalinTest.test(appTest, (server, client) -> {
            // Добавляем URL
            String formData1 = "url=https://example.com";
            client.post("/urls", formData1);

            String formData2 = "url=https://google.com";
            client.post("/urls", formData2);

            var response = client.get("/urls");
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(response.body().string())
                    .contains("https://example.com")
                    .contains("https://google.com")
                    .contains("Список URL")
                    .doesNotContain("Список URL пуст");
        });
    }

    @Test
    void testUrlNormalization() {
        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=https://example.com:443";

            var response = client.post("/urls", formData);
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());

            // Проверяем что нормализованный URL отображается
            var urlsResponse = client.get("/urls");
            assertThat(urlsResponse.body().string()).contains("https://example.com");
        });
    }

    @Test
    void testUrlNormalizationWithNonStandardPort() {
        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=https://example.com:8080";

            var response = client.post("/urls", formData);
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());

            // Проверяем что URL с портом отображается
            var urlsResponse = client.get("/urls");
            assertThat(urlsResponse.body().string()).contains("https://example.com:8080");
        });
    }
}
