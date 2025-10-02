package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.BaseRepository;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class RepositoryTest extends BaseTest {

    @Test
    void testBaseRepositoryDataSource() {
        assertThat(BaseRepository.dataSource).isNotNull();
    }

    @Test
    void testUrlRepositoryFindNonExistent() throws SQLException {
        var result = UrlRepository.find(999999L);
        assertThat(result).isEmpty();
    }

    @Test
    void testUrlRepositoryFindByNameNonExistent() throws SQLException {
        var result = UrlRepository.findByName("https://non-existent-test-url-12345.com");
        assertThat(result).isEmpty();
    }

    @Test
    void testUrlCheckRepositoryFindByNonExistentUrlId() throws SQLException {
        var checks = UrlCheckRepository.findByUrlId(999999L);
        assertThat(checks).isNotNull();
        assertThat(checks.size()).isEqualTo(0);
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
    void testUrlRepositorySaveAndFindWithChecks() throws SQLException {
        var url = new Url("https://complex-test.com");
        UrlRepository.save(url);

        var check1 = new UrlCheck(200, "Title 1", "H1 1", "Desc 1", url.getId());
        var check2 = new UrlCheck(404, "Title 2", "H1 2", "Desc 2", url.getId());

        UrlCheckRepository.save(check1);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        UrlCheckRepository.save(check2);

        var foundUrl = UrlRepository.find(url.getId());
        assertThat(foundUrl).isPresent();
        assertThat(foundUrl.get().getUrlChecks().size()).isEqualTo(2);

        assertThat(foundUrl.get().getUrlChecks().get(0).getTitle()).isEqualTo("Title 2");
        assertThat(foundUrl.get().getUrlChecks().get(1).getTitle()).isEqualTo("Title 1");
    }
}
