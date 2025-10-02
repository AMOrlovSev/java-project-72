package hexlet.code;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.controller.RootController;
import hexlet.code.controller.UrlsController;
import hexlet.code.repository.BaseRepository;
import hexlet.code.util.NamedRoutes;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.sql.SQLException;

import static hexlet.code.App.createTemplateEngine;
import static hexlet.code.App.readResourceFile;

public abstract class BaseTest {
    protected static final String TEST_DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    protected Javalin appTest;
    protected static MockWebServer mockWebServer;

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
}
