package hexlet.code;

import hexlet.code.util.NamedRoutes;
import io.javalin.http.HttpStatus;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class RootControllerTest extends BaseTest {

    @Test
    public void testMainPage() {
        JavalinTest.test(appTest, (server, client) -> {
            var response = client.get(NamedRoutes.rootPath());
            assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(response.body().string()).contains("Анализатор страниц");
        });
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
}
