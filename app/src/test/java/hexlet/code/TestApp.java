package hexlet.code;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.controller.RootController;
import hexlet.code.controller.UrlsController;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
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
import java.time.LocalDateTime;

import static hexlet.code.App.createTemplateEngine;
import static hexlet.code.App.readResourceFile;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
            var response = client.get(NamedRoutes.rootPath());
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(response.body().string()).contains("Анализатор страниц");
        });
    }

    @Test
    void testUrlsPageEmpty() {
        JavalinTest.test(appTest, (server, client) -> {
            var response = client.get(NamedRoutes.urlsPath());
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
            var response = client.post(NamedRoutes.urlsPath(), requestBody);

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
            var response = client.post(NamedRoutes.urlsPath(), requestBody);

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
            var response = client.post(NamedRoutes.urlsPath(), requestBody);

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
            var response = client.post(NamedRoutes.urlsPath(), requestBody);

            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());

            var countAfter = UrlRepository.getEntities().size();
            assertThat(countAfter).isEqualTo(countBefore);
        });
    }

    @Test
    void testShowUrl() {
        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=https://example.com";
            client.post(NamedRoutes.urlsPath(), formData);

            var urlsResponse = client.get(NamedRoutes.urlsPath());
            var urlsBody = urlsResponse.body().string();

            assertThat(urlsBody).contains("https://example.com");
            assertThat(urlsBody).contains(NamedRoutes.urlsPath() + "/");
        });
    }

    @Test
    void testShowUrlDetails() {
        JavalinTest.test(appTest, (server, client) -> {
            // Сначала создаем URL и получаем его ID
            String urlName = "https://show-details-test.com";
            var url = new Url(urlName);
            try {
                UrlRepository.save(url);
                Long urlId = url.getId();

                var response = client.get(NamedRoutes.urlPath(urlId));
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
            var response = client.get(NamedRoutes.urlPath(999999L));
            assertThat(response.code()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
        });
    }

    @Test
    void testUrlsPageWithData() {
        JavalinTest.test(appTest, (server, client) -> {
            String formData1 = "url=https://example.com";
            client.post(NamedRoutes.urlsPath(), formData1);

            String formData2 = "url=https://google.com";
            client.post(NamedRoutes.urlsPath(), formData2);

            var response = client.get(NamedRoutes.urlsPath());
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

            var response = client.post(NamedRoutes.urlsPath(), formData);
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());

            var urlsResponse = client.get(NamedRoutes.urlsPath());
            assertThat(urlsResponse.body().string()).contains("https://example.com");
        });
    }

    @Test
    void testUrlNormalizationWithNonStandardPort() {
        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=https://example.com:8080";

            var response = client.post(NamedRoutes.urlsPath(), formData);
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());

            var urlsResponse = client.get(NamedRoutes.urlsPath());
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
            var createResponse = client.post(NamedRoutes.urlsPath(), formData);
            assertThat(createResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var savedUrl = UrlRepository.findByName(mockUrl.replaceFirst("/$", ""))
                    .orElseThrow(() -> new RuntimeException("URL not found after save"));
            Long urlId = savedUrl.getId();

            var checkResponse = client.post(NamedRoutes.urlChecksPath(urlId));
            assertThat(checkResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var checks = UrlCheckRepository.findByUrlId(urlId);
            assertThat(checks.size()).isEqualTo(1);

            var check = checks.get(0);
            assertThat(check.getStatusCode()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(check.getTitle()).isEqualTo("Test Page Title");
            assertThat(check.getH1()).isEqualTo("Test H1 Header");
            assertThat(check.getDescription()).isEqualTo("Test page description");

            var showResponse = client.get(NamedRoutes.urlPath(urlId));
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
            client.post(NamedRoutes.urlsPath(), formData);

            var savedUrl = UrlRepository.findByName(mockUrl.replaceFirst("/$", ""))
                    .orElseThrow(() -> new RuntimeException("URL not found after save"));
            Long urlId = savedUrl.getId();

            var checkResponse = client.post(NamedRoutes.urlChecksPath(urlId));
            assertThat(checkResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var checks = UrlCheckRepository.findByUrlId(urlId);
            assertThat(checks.size()).isEqualTo(1);
            assertThat(checks.get(0).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.getCode());

            var showResponse = client.get(NamedRoutes.urlPath(urlId));
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
            client.post(NamedRoutes.urlsPath(), formData);

            var savedUrl = UrlRepository.findByName(mockUrl.replaceFirst("/$", ""))
                    .orElseThrow(() -> new RuntimeException("URL not found after save"));
            Long urlId = savedUrl.getId();

            var checkResponse = client.post(NamedRoutes.urlChecksPath(urlId));
            assertThat(checkResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var checks = UrlCheckRepository.findByUrlId(urlId);
            assertThat(checks.size()).isEqualTo(1);
            assertThat(checks.get(0).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getCode());

            var showResponse = client.get(NamedRoutes.urlPath(urlId));
            var showBody = showResponse.body().string();
            assertThat(showBody).contains("500");
        });
    }

    @Test
    void testUrlCheckForNonExistentUrl() {
        JavalinTest.test(appTest, (server, client) -> {
            var response = client.post(NamedRoutes.urlChecksPath(999999L));
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
            client.post(NamedRoutes.urlsPath(), formData);

            var savedUrl = UrlRepository.findByName(mockUrl.replaceFirst("/$", ""))
                    .orElseThrow(() -> new RuntimeException("URL not found after save"));
            Long urlId = savedUrl.getId();

            client.post(NamedRoutes.urlChecksPath(urlId));
            client.post(NamedRoutes.urlChecksPath(urlId));

            var checks = UrlCheckRepository.findByUrlId(urlId);
            assertThat(checks.size()).isEqualTo(2);

            boolean hasFirstCheck = checks.stream().anyMatch(check -> "First Check".equals(check.getTitle()));
            boolean hasSecondCheck = checks.stream().anyMatch(check -> "Second Check".equals(check.getTitle()));
            assertThat(hasFirstCheck).isTrue();
            assertThat(hasSecondCheck).isTrue();

            var showResponse = client.get(NamedRoutes.urlPath(urlId));
            var showBody = showResponse.body().string();
            assertThat(showBody)
                    .contains("First Check")
                    .contains("Second Check");
        });
    }

    @Test
    void testUrlCheckWithMissingSeoElements() throws SQLException {
        String htmlContent = """
                <!DOCTYPE html>
                <html>
                <body>
                    <p>Page without title, h1 and description</p>
                </body>
                </html>
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(htmlContent)
                .setResponseCode(HttpStatus.OK.getCode()));

        String mockUrl = mockWebServer.url("/").toString();

        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=" + mockUrl;
            client.post(NamedRoutes.urlsPath(), formData);

            var savedUrl = UrlRepository.findByName(mockUrl.replaceFirst("/$", ""))
                    .orElseThrow(() -> new RuntimeException("URL not found after save"));
            Long urlId = savedUrl.getId();

            var checkResponse = client.post(NamedRoutes.urlChecksPath(urlId));
            assertThat(checkResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var checks = UrlCheckRepository.findByUrlId(urlId);
            assertThat(checks.size()).isEqualTo(1);

            var check = checks.get(0);
            assertThat(check.getStatusCode()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(check.getTitle()).isEmpty();
            assertThat(check.getH1()).isEmpty();
            assertThat(check.getDescription()).isEmpty();

            var showResponse = client.get(NamedRoutes.urlPath(urlId));
            var showBody = showResponse.body().string();
            assertThat(showBody)
                    .contains("200")
                    .doesNotContain("Test Page Title")
                    .doesNotContain("Test H1 Header")
                    .doesNotContain("Test page description");
        });
    }

    @Test
    void testUrlCheckWithRedirect() throws SQLException {
        String finalHtmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Final Page After Redirect</title>
                </head>
                <body>
                    <h1>Redirected Page</h1>
                </body>
                </html>
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.MOVED_PERMANENTLY.getCode())
                .setHeader("Location", mockWebServer.url("/final")));

        mockWebServer.enqueue(new MockResponse()
                .setBody(finalHtmlContent)
                .setResponseCode(HttpStatus.OK.getCode()));

        String mockUrl = mockWebServer.url("/").toString();

        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=" + mockUrl;
            client.post(NamedRoutes.urlsPath(), formData);

            var savedUrl = UrlRepository.findByName(mockUrl.replaceFirst("/$", ""))
                    .orElseThrow(() -> new RuntimeException("URL not found after save"));
            Long urlId = savedUrl.getId();

            var checkResponse = client.post(NamedRoutes.urlChecksPath(urlId));
            assertThat(checkResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var checks = UrlCheckRepository.findByUrlId(urlId);
            assertThat(checks.size()).isEqualTo(1);

            var check = checks.get(0);
            assertThat(check.getStatusCode()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(check.getTitle()).isEqualTo("Final Page After Redirect");
            assertThat(check.getH1()).isEqualTo("Redirected Page");
        });
    }

    @Test
    void testUrlCheckOrdering() throws SQLException {
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

        String htmlContent3 = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Third Check</title>
                </head>
                <body></body>
                </html>
                """;

        mockWebServer.enqueue(new MockResponse().setBody(htmlContent1).setResponseCode(HttpStatus.OK.getCode()));
        mockWebServer.enqueue(new MockResponse().setBody(htmlContent2).setResponseCode(HttpStatus.OK.getCode()));
        mockWebServer.enqueue(new MockResponse().setBody(htmlContent3).setResponseCode(HttpStatus.OK.getCode()));

        String mockUrl = mockWebServer.url("/").toString();

        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=" + mockUrl;
            client.post(NamedRoutes.urlsPath(), formData);

            var savedUrl = UrlRepository.findByName(mockUrl.replaceFirst("/$", ""))
                    .orElseThrow(() -> new RuntimeException("URL not found after save"));
            Long urlId = savedUrl.getId();

            client.post(NamedRoutes.urlChecksPath(urlId));
            client.post(NamedRoutes.urlChecksPath(urlId));
            client.post(NamedRoutes.urlChecksPath(urlId));

            var checks = UrlCheckRepository.findByUrlId(urlId);
            assertThat(checks.size()).isEqualTo(3);

            assertThat(checks.get(0).getTitle()).isEqualTo("Third Check");
            assertThat(checks.get(1).getTitle()).isEqualTo("Second Check");
            assertThat(checks.get(2).getTitle()).isEqualTo("First Check");
        });
    }

    @Test
    void testUrlWithSpecialCharacters() throws SQLException {
        String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Страница с кириллицей &amp; символами</title>
                    <meta name="description" content="Описание с émojis 🚀 и <тегами>">
                </head>
                <body>
                    <h1>Заголовок с 𝕌nicode</h1>
                </body>
                </html>
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(htmlContent)
                .setResponseCode(HttpStatus.OK.getCode()));

        String mockUrl = mockWebServer.url("/").toString();

        JavalinTest.test(appTest, (server, client) -> {
            String formData = "url=" + mockUrl;
            client.post(NamedRoutes.urlsPath(), formData);

            var savedUrl = UrlRepository.findByName(mockUrl.replaceFirst("/$", ""))
                    .orElseThrow(() -> new RuntimeException("URL not found after save"));
            Long urlId = savedUrl.getId();

            var checkResponse = client.post(NamedRoutes.urlChecksPath(urlId));
            assertThat(checkResponse.code()).isEqualTo(HttpStatus.OK.getCode());

            var checks = UrlCheckRepository.findByUrlId(urlId);
            assertThat(checks.size()).isEqualTo(1);

            var check = checks.get(0);
            assertThat(check.getTitle()).isEqualTo("Страница с кириллицей & символами");
            assertThat(check.getH1()).isEqualTo("Заголовок с 𝕌nicode");
            assertThat(check.getDescription()).isEqualTo("Описание с émojis 🚀 и <тегами>");
        });
    }

    @Test
    void testBaseRepositoryDataSource() {
        assertThat(BaseRepository.dataSource).isNotNull();

        JavalinTest.test(appTest, (server, client) -> {
            try {
                var connection = BaseRepository.dataSource.getConnection();
                assertThat(connection).isNotNull();
                assertThat(connection.isValid(1)).isTrue();
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testAppReadResourceFile() throws IOException {
        var content = App.readResourceFile("schema.sql");
        assertThat(content).isNotNull();
        assertThat(content).contains("CREATE TABLE");
        assertThat(content).contains("urls");
        assertThat(content).contains("url_checks");
    }

    @Test
    void testUrlModel() {
        var url = new Url("https://test-model.com");

        assertThat(url.getName()).isEqualTo("https://test-model.com");
        assertThat(url.getUrlChecks()).isNotNull();
        assertThat(url.getUrlChecks().size()).isEqualTo(0);

        url.setId(1L);
        url.setCreatedAt(LocalDateTime.now());

        assertThat(url.getId()).isEqualTo(1L);
        assertThat(url.getCreatedAt()).isNotNull();

        var check = new UrlCheck(200, "Title", "H1", "Description", 1L);
        url.addCheck(check);

        assertThat(url.getUrlChecks().size()).isEqualTo(1);
        assertThat(url.getUrlChecks().get(0)).isEqualTo(check);
        assertThat(check.getUrlId()).isEqualTo(1L);
    }

    @Test
    void testRootControllerWithFlashMessages() {
        JavalinTest.test(appTest, (server, client) -> {
            var response = client.get(NamedRoutes.rootPath());
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());

            var body = response.body().string();
            assertThat(body).contains("Анализатор страниц");
            assertThat(body).contains("Бесплатно проверяйте сайты на SEO пригодность");
            assertThat(body).contains("ссылка");
            assertThat(body).contains("Проверить");
        });
    }

    @Test
    void testNamedRoutesAllMethods() {
        assertThat(NamedRoutes.rootPath()).isEqualTo("/");
        assertThat(NamedRoutes.urlsPath()).isEqualTo("/urls");

        assertThat(NamedRoutes.urlPath(123L)).isEqualTo("/urls/123");
        assertThat(NamedRoutes.urlChecksPath(456L)).isEqualTo("/urls/456/checks");

        assertThat(NamedRoutes.urlPath("789")).isEqualTo("/urls/789");
        assertThat(NamedRoutes.urlChecksPath("999")).isEqualTo("/urls/999/checks");
    }

    @Test
    void testAppGetApp() throws IOException, SQLException {
        assertDoesNotThrow(() -> {
            Javalin app = App.getApp();
            assertThat(app).isNotNull();

            JavalinTest.test(app, (server, client) -> {

                var rootResponse = client.get(NamedRoutes.rootPath());
                assertThat(rootResponse.code()).isEqualTo(HttpStatus.OK.getCode());

                var urlsResponse = client.get(NamedRoutes.urlsPath());
                assertThat(urlsResponse.code()).isEqualTo(HttpStatus.OK.getCode());

                assertThat(rootResponse.header("Content-Type")).contains("text/html");
                assertThat(rootResponse.header("Content-Type")).contains("charset=utf-8");
            });
        });
    }

    @Test
    void testAppMainMethodIntegration() {
        assertDoesNotThrow(() -> {
            Javalin app = App.getApp();

            JavalinTest.test(app, (server, client) -> {
                assertThat(client.get(NamedRoutes.rootPath()).code()).isEqualTo(HttpStatus.OK.getCode());
                assertThat(client.get(NamedRoutes.urlsPath()).code()).isEqualTo(HttpStatus.OK.getCode());

                assertThat(client.get("/nonexistent").code()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
            });
        });
    }

    @Test
    void testAppContentTypeConfiguration() {
        JavalinTest.test(appTest, (server, client) -> {
            var url = new Url("https://content-type-test.com");
            try {
                UrlRepository.save(url);
                Long urlId = url.getId();

                var response = client.get(NamedRoutes.rootPath());
                assertThat(response.header("Content-Type")).isEqualTo("text/html;charset=utf-8");

                var urlsResponse = client.get(NamedRoutes.urlsPath());
                assertThat(urlsResponse.header("Content-Type")).isEqualTo("text/html;charset=utf-8");

                var urlResponse = client.get(NamedRoutes.urlPath(urlId));
                assertThat(urlResponse.header("Content-Type")).isEqualTo("text/html;charset=utf-8");

                var notFoundResponse = client.get(NamedRoutes.urlPath(999999L));
                assertThat(notFoundResponse.header("Content-Type")).isEqualTo("text/plain;charset=utf-8");

            } catch (SQLException e) {
                throw new RuntimeException("Database error during test", e);
            }
        });
    }
}
