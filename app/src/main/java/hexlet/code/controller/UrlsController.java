package hexlet.code.controller;

import hexlet.code.dto.MainPage;
import hexlet.code.dto.urls.UrlPage;
import hexlet.code.dto.urls.UrlsPage;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import hexlet.code.util.NamedRoutes;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

import static io.javalin.rendering.template.TemplateUtil.model;

public class UrlsController {
    public static void index(Context ctx) throws SQLException {
        String flash = ctx.consumeSessionAttribute("flash");
        String flashType = ctx.consumeSessionAttribute("flashType");
        var urls = UrlRepository.getEntities();
        var page = new UrlsPage(urls, flash, flashType);
        ctx.render("urls/index.jte", model("page", page));
    }

    public static void create(Context ctx) throws SQLException {
        var urlInput = ctx.formParam("url");

        URI uri;
        try {
            uri = new URI(urlInput.trim());
        } catch (URISyntaxException e) {
            renderMainPageWithError(ctx, 400, "Некорректный URL");
            return;
        }

        if (!uri.isAbsolute()) {
            renderMainPageWithError(ctx, 400, "Некорректный URL");
            return;
        }

        String normalizedUrl = normalizeUrl(uri);

        if (UrlRepository.findByName(normalizedUrl).isPresent()) {
            renderUrlsPageWithInfo(ctx, 409, "Страница уже существует");
            return;
        }

        var url = new Url(normalizedUrl);
        UrlRepository.save(url);
        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flashType", "success");
        ctx.redirect(NamedRoutes.urlsPath());
    }

    public static void show(Context ctx) throws SQLException {
        var id = ctx.pathParamAsClass("id", Long.class).get();
        var url = UrlRepository.find(id)
                .orElseThrow(() -> new NotFoundResponse("Entity with id = " + id + " not found"));
        String flash = ctx.consumeSessionAttribute("flash");
        String flashType = ctx.consumeSessionAttribute("flashType");
        var page = new UrlPage(url, flash, flashType);
        ctx.render("urls/show.jte", model("page", page));
    }

    public static void check(Context ctx) throws SQLException {
        var id = ctx.pathParamAsClass("id", Long.class).get();

        var url = UrlRepository.find(id)
                .orElseThrow(() -> new NotFoundResponse("Entity with id = " + id + " not found"));

        HttpResponse<String> response;
        try {
            response = Unirest.get(url.getName()).asString();
        } catch (UnirestException e) {
            ctx.sessionAttribute("flash", "Ошибка при проверке страницы: " + e.getMessage());
            ctx.sessionAttribute("flashType", "danger");
            ctx.redirect(NamedRoutes.urlPath(id));
            return;
        }

        int statusCode = response.getStatus();
        String htmlContent = response.getBody();

        Document doc = Jsoup.parse(htmlContent);

        String title = "";
        Element titleElement = doc.selectFirst("title");
        if (titleElement != null) {
            title = titleElement.text().trim();
        }

        String h1 = "";
        Element h1Element = doc.selectFirst("h1");
        if (h1Element != null) {
            h1 = h1Element.text().trim();
        }

        String description = "";
        Element metaDescription = doc.selectFirst("meta[name=description]");
        if (metaDescription != null) {
            description = metaDescription.attr("content").trim();
        }

        var urlCheck = new UrlCheck(statusCode, title, h1, description, id);
        UrlCheckRepository.save(urlCheck);

        ctx.sessionAttribute("flash", "Страница успешно проверена");
        ctx.sessionAttribute("flashType", "success");
        ctx.redirect(NamedRoutes.urlPath(id));
    }




    private static String normalizeUrl(URI uri) {
        String scheme = uri.getScheme().toLowerCase();
        String host = uri.getHost().toLowerCase();
        int port = uri.getPort();

        if (port == -1 || port == getDefaultPort(scheme)) {
            return scheme + "://" + host;
        } else {
            return scheme + "://" + host + ":" + port;
        }
    }

    private static int getDefaultPort(String scheme) {
        return scheme.equals("https") ? 443 : 80;
    }

    private static void renderMainPageWithError(Context ctx, int status, String errorMessage) {
        ctx.status(status);
        var page = new MainPage(errorMessage, "danger");
        ctx.render("index.jte", model("page", page));
    }

    private static void renderUrlsPageWithInfo(Context ctx, int status, String errorMessage) throws SQLException {
        ctx.status(status);
        var urls = UrlRepository.getEntities();
        var page = new UrlsPage(urls, errorMessage, "info");
        ctx.render("urls/index.jte", model("page", page));
    }
}

