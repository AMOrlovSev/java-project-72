package hexlet.code;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.controller.RootController;
import hexlet.code.controller.UrlsController;
import hexlet.code.model.Url;
import hexlet.code.repository.BaseRepository;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import hexlet.code.util.NamedRoutes;
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
    final void setUp() throws SQLException, IOException {
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

        appTest.get(NamedRoutes.rootPath(), RootController::index);

        appTest.get(NamedRoutes.urlsPath(), UrlsController::index);
        appTest.post(NamedRoutes.urlsPath(), UrlsController::create);
        appTest.get(NamedRoutes.urlPath("{id}"), UrlsController::show);

        appTest.post(NamedRoutes.urlChecksPath("{id}"), UrlsController::check);
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
            var requestBody = "url=https://example.com";
            var response = client.post("/urls", requestBody);

            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(response.body().string()).contains("https://example.com");

            try {
                var url = UrlRepository.findByName("https://example.com");
                assertThat(url).isPresent();
                assertThat(url.get().getName()).isEqualTo("https://example.com");
            } catch (SQLException e) {
                throw new RuntimeException("Database error during test", e);
            }
        });
    }

    @Test
    public void testCreateUrlWithInvalidUrl() {
        JavalinTest.test(appTest, (server, client) -> {
            var requestBody = "url=invalid-url";
            var response = client.post("/urls", requestBody);

            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());

            try {
                var url = UrlRepository.findByName("invalid-url");
                assertThat(url).isEmpty();
            } catch (SQLException e) {
                throw new RuntimeException("Database error during test", e);
            }
        });
    }

    @Test
    public void testCreateUrlWithEmptyUrl() {
        JavalinTest.test(appTest, (server, client) -> {
            var requestBody = "url=";
            var response = client.post("/urls", requestBody);

            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            try {
                var url = UrlRepository.findByName("");
                assertThat(url).isEmpty();
            } catch (SQLException e) {
                throw new RuntimeException("Database error during test", e);
            }
        });
    }

    @Test
    public void testCreateDuplicateUrl() {
        JavalinTest.test(appTest, (server, client) -> {
            var url = new Url("https://example.com");
            UrlRepository.save(url);
            var countBefore = UrlRepository.getEntities().size();

            var requestBody = "url=https://example.com";
            var response = client.post("/urls", requestBody);

            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());

            var countAfter = UrlRepository.getEntities().size();
            assertThat(countAfter).isEqualTo(countBefore);
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
            var url = new Url(urlName);
            try {
                UrlRepository.save(url);
                Long urlId = url.getId();

                var response = client.get("/urls/" + urlId);
                assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());

                String body = response.body().string();

                assertThat(body)
                        .contains(urlName)
                        .contains("ID")
                        .contains("Имя")
                        .contains("Дата создания")
                        .contains("Проверки");
            } catch (SQLException e) {
                throw new RuntimeException("Database error during test", e);
            }
        });
    }

    @Test
    void testShowUrlNotFound() {
        JavalinTest.test(appTest, (server, client) -> {
            var response = client.get("/urls/999999");
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
                .setResponseCode(HttpStatus.OK.getCode()));

        String mockUrl = mockWebServer.url("/").toString();

        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=" + mockUrl;
            var createResponse = client.post("/urls", formData);
            assertThat(createResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var savedUrl = UrlRepository.findByName(mockUrl.replaceFirst("/$", ""))
                    .orElseThrow(() -> new RuntimeException("URL not found after save"));
            Long urlId = savedUrl.getId();

            var checkResponse = client.post("/urls/" + urlId + "/checks");
            assertThat(checkResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var checks = UrlCheckRepository.findByUrlId(urlId);
            assertThat(checks.size()).isEqualTo(1);

            var check = checks.get(0);
            assertThat(check.getStatusCode()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(check.getTitle()).isEqualTo("Test Page Title");
            assertThat(check.getH1()).isEqualTo("Test H1 Header");
            assertThat(check.getDescription()).isEqualTo("Test page description");

            var showResponse = client.get("/urls/" + urlId);
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
        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.getCode()));

        String mockUrl = mockWebServer.url("/").toString();

        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=" + mockUrl;
            client.post("/urls", formData);

            var savedUrl = UrlRepository.findByName(mockUrl.replaceFirst("/$", ""))
                    .orElseThrow(() -> new RuntimeException("URL not found after save"));
            Long urlId = savedUrl.getId();

            var checkResponse = client.post("/urls/" + urlId + "/checks");
            assertThat(checkResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var checks = UrlCheckRepository.findByUrlId(urlId);
            assertThat(checks.size()).isEqualTo(1);
            assertThat(checks.get(0).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.getCode());

            var showResponse = client.get("/urls/" + urlId);
            var showBody = showResponse.body().string();
            assertThat(showBody).contains("404");
        });
    }

    @Test
    void testUrlCheckWithServerError() throws SQLException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.getCode()));

        String mockUrl = mockWebServer.url("/").toString();

        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=" + mockUrl;
            client.post("/urls", formData);

            var savedUrl = UrlRepository.findByName(mockUrl.replaceFirst("/$", ""))
                    .orElseThrow(() -> new RuntimeException("URL not found after save"));
            Long urlId = savedUrl.getId();

            var checkResponse = client.post("/urls/" + urlId + "/checks");
            assertThat(checkResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var checks = UrlCheckRepository.findByUrlId(urlId);
            assertThat(checks.size()).isEqualTo(1);
            assertThat(checks.get(0).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getCode());

            var showResponse = client.get("/urls/" + urlId);
            var showBody = showResponse.body().string();
            assertThat(showBody).contains("500");
        });
    }

    @Test
    void testUrlCheckForNonExistentUrl() {
        JavalinTest.test(appTest, (server, client) -> {
            var response = client.post("/urls/999999/checks");
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

        mockWebServer.enqueue(new MockResponse().setBody(htmlContent1).setResponseCode(HttpStatus.OK.getCode()));
        mockWebServer.enqueue(new MockResponse().setBody(htmlContent2).setResponseCode(HttpStatus.OK.getCode()));

        String mockUrl = mockWebServer.url("/").toString();

        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=" + mockUrl;
            client.post("/urls", formData);

            var savedUrl = UrlRepository.findByName(mockUrl.replaceFirst("/$", ""))
                    .orElseThrow(() -> new RuntimeException("URL not found after save"));
            Long urlId = savedUrl.getId();

            client.post("/urls/" + urlId + "/checks");
            client.post("/urls/" + urlId + "/checks");

            var checks = UrlCheckRepository.findByUrlId(urlId);
            assertThat(checks.size()).isEqualTo(2);

            boolean hasFirstCheck = checks.stream().anyMatch(check -> "First Check".equals(check.getTitle()));
            boolean hasSecondCheck = checks.stream().anyMatch(check -> "Second Check".equals(check.getTitle()));
            assertThat(hasFirstCheck).isTrue();
            assertThat(hasSecondCheck).isTrue();

            var showResponse = client.get("/urls/" + urlId);
            var showBody = showResponse.body().string();
            assertThat(showBody)
                    .contains("First Check")
                    .contains("Second Check");
        });
    }
}