package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.repository.UrlRepository;
import hexlet.code.util.NamedRoutes;
import io.javalin.http.HttpStatus;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UrlsControllerTest extends BaseTest {

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
            try {
                UrlRepository.save(url);
                var countBefore = UrlRepository.getEntities().size();

                var requestBody = "url=https://example.com";
                var response = client.post(NamedRoutes.urlsPath(), requestBody);

                assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());

                var countAfter = UrlRepository.getEntities().size();
                assertThat(countAfter).isEqualTo(countBefore);
            } catch (SQLException e) {
                throw new RuntimeException("Database error during test", e);
            }
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
}
