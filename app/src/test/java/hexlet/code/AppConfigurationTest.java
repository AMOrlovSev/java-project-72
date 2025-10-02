package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.repository.UrlRepository;
import hexlet.code.util.NamedRoutes;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AppConfigurationTest extends BaseTest {

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
    void testAppReadResourceFile() throws IOException {
        var content = App.readResourceFile("schema.sql");
        assertThat(content).isNotNull();
        assertThat(content).contains("CREATE TABLE");
        assertThat(content).contains("urls");
        assertThat(content).contains("url_checks");
    }
}
