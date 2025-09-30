package hexlet.code;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.repository.BaseRepository;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.rendering.template.JavalinJte;
import io.javalin.testtools.JavalinTest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
    private static MockWebServer mockWebServer;

    @BeforeAll
    static void setUpAll() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDownAll() throws IOException {
        mockWebServer.shutdown();
    }

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
        appTest.post("/urls/{id}/checks", hexlet.code.controller.UrlsController::check);
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

            assertThat(body).contains("Страница уже существует");
        });
    }

    @Test
    void testCreateUrlInvalid() {
        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=invalid-url";
            var response = client.post("/urls", formData);

            assertThat(response.code()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());
            assertThat(response.body().string()).contains("Некорректный URL");
        });
    }

    @Test
    void testCreateUrlRelative() {
        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=/relative/url";
            var response = client.post("/urls", formData);

            assertThat(response.code()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());
            assertThat(response.body().string()).contains("Некорректный URL");
        });
    }

    @Test
    void testShowUrl() {
        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=https://example.com";
            client.post("/urls", formData);

            var urlsResponse = client.get("/urls");
            var urlsBody = urlsResponse.body().string();

            assertThat(urlsBody).contains("https://example.com");
            assertThat(urlsBody).contains("/urls/");
        });
    }

    @Test
    void testShowUrlDetails() {
        JavalinTest.test(appTest, (server, client) -> {
            String urlName = "https://show-details-test.com";
            String formData = "url=" + urlName;
            client.post("/urls", formData);

            var response = client.get("/urls/1");
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());

            String body = response.body().string();

            assertThat(body)
                    .contains("Сайт: " + urlName)
                    .contains("ID")
                    .contains("Имя")
                    .contains("Дата создания")
                    .contains(urlName)
                    .contains("Проверки");
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
            String formData1 = "url=https://example.com";
            client.post("/urls", formData1);

            String formData2 = "url=https://google.com";
            client.post("/urls", formData2);

            var response = client.get("/urls");
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(response.body().string())
                    .contains("https://example.com")
                    .contains("https://google.com")
                    .contains("Сайты")
                    .doesNotContain("Список URL пуст");
        });
    }

    @Test
    void testUrlNormalization() {
        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=https://example.com:443";

            var response = client.post("/urls", formData);
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());

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

            var urlsResponse = client.get("/urls");
            assertThat(urlsResponse.body().string()).contains("https://example.com:8080");
        });
    }




    @Test
    void testUrlCheckSuccess() throws SQLException {
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Page Title</title>
                <meta name="description" content="Test page description">
            </head>
            <body>
                <h1>Test H1 Header</h1>
                <p>Some content</p>
            </body>
            </html>
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(htmlContent)
                .setResponseCode(200));

        String mockUrl = mockWebServer.url("/").toString();

        JavalinTest.test(appTest, (server, client) -> {

            String formData = "url=" + mockUrl;
            var createResponse = client.post("/urls", formData);
            assertThat(createResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var urlsResponse = client.get("/urls");
            var urlsBody = urlsResponse.body().string();
            assertThat(urlsBody).contains(mockUrl.replaceFirst("/$", ""));

            var checkResponse = client.post("/urls/1/checks");
            assertThat(checkResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var showResponse = client.get("/urls/1");
            var showBody = showResponse.body().string();
            assertThat(showBody)
                    .contains("200")
                    .contains("Test Page Title")
                    .contains("Test H1 Header")
                    .contains("Test page description");
        });
    }

    @Test
    void testUrlCheckWith404Error() throws SQLException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        String mockUrl = mockWebServer.url("/").toString();

        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=" + mockUrl;
            client.post("/urls", formData);

            var checkResponse = client.post("/urls/1/checks");
            assertThat(checkResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var showResponse = client.get("/urls/1");
            var showBody = showResponse.body().string();
            assertThat(showBody).contains("404");
        });
    }

    @Test
    void testUrlCheckWithServerError() throws SQLException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        String mockUrl = mockWebServer.url("/").toString();

        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=" + mockUrl;
            client.post("/urls", formData);

            var checkResponse = client.post("/urls/1/checks");
            assertThat(checkResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var showResponse = client.get("/urls/1");
            var showBody = showResponse.body().string();
            assertThat(showBody).contains("500");
        });
    }

    @Test
    void testUrlCheckForNonExistentUrl() {
        JavalinTest.test(appTest, (server, client) -> {
            var response = client.post("/urls/999/checks");
            assertThat(response.code()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
        });
    }

    @Test
    void testMultipleUrlChecks() throws SQLException {
        String htmlContent1 = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>First Check</title>
            </head>
            <body></body>
            </html>
            """;

        String htmlContent2 = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Second Check</title>
            </head>
            <body></body>
            </html>
            """;

        mockWebServer.enqueue(new MockResponse().setBody(htmlContent1).setResponseCode(200));
        mockWebServer.enqueue(new MockResponse().setBody(htmlContent2).setResponseCode(200));

        String mockUrl = mockWebServer.url("/").toString();

        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=" + mockUrl;
            client.post("/urls", formData);

            client.post("/urls/1/checks");

            client.post("/urls/1/checks");

            var showResponse = client.get("/urls/1");
            var showBody = showResponse.body().string();
            assertThat(showBody)
                    .contains("First Check")
                    .contains("Second Check");
        });
    }
}
