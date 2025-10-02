package hexlet.code.controller;

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
        System.out.println("Received URL: " + urlInput); // Debug

        if (urlInput == null || urlInput.trim().isEmpty()) {
            System.out.println("URL is empty"); // Debug
            setFlashAndRedirect(ctx, "URL не может быть пустым", "danger", NamedRoutes.rootPath());
            return;
        }

        URI uri;
        try {
            uri = new URI(urlInput.trim());
            System.out.println("Parsed URI: " + uri); // Debug
        } catch (URISyntaxException e) {
            System.out.println("URI syntax exception: " + e.getMessage()); // Debug
            setFlashAndRedirect(ctx, "Некорректный URL", "danger", NamedRoutes.rootPath());
            return;
        }

        if (!uri.isAbsolute()) {
            System.out.println("URI is not absolute"); // Debug
            setFlashAndRedirect(ctx, "Некорректный URL", "danger", NamedRoutes.rootPath());
            return;
        }

        String normalizedUrl = normalizeUrl(uri);
        System.out.println("Normalized URL: " + normalizedUrl); // Debug

        if (UrlRepository.findByName(normalizedUrl).isPresent()) {
            setFlashAndRedirect(ctx, "Страница уже существует", "info", NamedRoutes.urlsPath());
            return;
        }

        var url = new Url(normalizedUrl);
        UrlRepository.save(url);
        setFlashAndRedirect(ctx, "Страница успешно добавлена", "success", NamedRoutes.urlsPath());
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

            setFlashAndRedirect(ctx, "Страница успешно проверена", "success", NamedRoutes.urlPath(id));

        } catch (UnirestException e) {
            setFlashAndRedirect(ctx, "Ошибка при проверке страницы: "
                    + e.getMessage(), "danger", NamedRoutes.urlPath(id));
        }
    }


    private static void setFlashAndRedirect(Context ctx, String message, String type, String path) {
        ctx.sessionAttribute("flash", message);
        ctx.sessionAttribute("flashType", type);
        System.out.println("Redirecting to: " + path + " with message: " + message); // Debug
        ctx.redirect(path);
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
}

