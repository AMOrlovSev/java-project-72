package hexlet.code;

import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import hexlet.code.util.NamedRoutes;
import io.javalin.http.HttpStatus;
import io.javalin.testtools.JavalinTest;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UrlCheckControllerTest extends BaseTest {

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
}
