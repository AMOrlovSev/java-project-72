package hexlet.code.controller;

import hexlet.code.dto.MainPage;
import hexlet.code.dto.urls.UrlPage;
import hexlet.code.dto.urls.UrlsPage;
import hexlet.code.model.Url;
import hexlet.code.repository.UrlRepository;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

import static io.javalin.rendering.template.TemplateUtil.model;

public class UrlsController {
    public static void index(Context ctx) throws SQLException {
        String flash = ctx.consumeSessionAttribute("flash");
        var urls = UrlRepository.getEntities();
        var page = new UrlsPage(urls, flash);
        ctx.render("urls/index.jte", model("page", page));
    }

    public static void create(Context ctx) throws SQLException {
        var urlInput = ctx.formParam("url");

        if (urlInput == null || urlInput.trim().isEmpty()) {
            ctx.status(400);
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.redirect("/");
            return;
        }

        try {
            var uri = new URI(urlInput.trim());
            var urlObject = uri.toURL();

            if (!uri.isAbsolute()) {
                ctx.status(400);
                ctx.sessionAttribute("flash", "Некорректный URL");
                ctx.redirect("/");
                return;
            }

            String normalizedUrl;
            if (urlObject.getPort() == -1 || urlObject.getPort() == urlObject.getDefaultPort()) {
                normalizedUrl = urlObject.getProtocol() + "://" + urlObject.getHost();
            } else {
                normalizedUrl = urlObject.getProtocol() + "://" + urlObject.getHost() + ":" + urlObject.getPort();
            }

            if (UrlRepository.findByName(normalizedUrl).isPresent()) {
                ctx.status(409);
                ctx.sessionAttribute("flash", "Страница уже существует");
                ctx.redirect("/urls");
                return;
            }

            var url = new Url(normalizedUrl);
            UrlRepository.save(url);
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.redirect("/urls");


        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            ctx.status(400);
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.redirect("/");
        } catch (Exception e) {
            ctx.status(500);
            ctx.sessionAttribute("flash", "Server error: " + e.getMessage());
            ctx.redirect("/");
        }
    }

    public static void show(Context ctx) throws SQLException {
        var id = ctx.pathParamAsClass("id", Long.class).get();
        var url = UrlRepository.find(id)
                .orElseThrow(() -> new NotFoundResponse("Entity with id = " + id + " not found"));
        String flash = ctx.consumeSessionAttribute("flash");
        var page = new UrlPage(url, flash);
        ctx.render("urls/show.jte", model("page", page));
    }
}
