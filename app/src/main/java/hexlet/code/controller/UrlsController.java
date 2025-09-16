package hexlet.code.controller;

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
        var urls = UrlRepository.getEntities();
        var page = new UrlsPage(urls);
        ctx.render("urls/index.jte", model("page", page));
    }

    public static void create(Context ctx) throws SQLException {
        var urlInput = ctx.formParam("url");

        if (urlInput == null || urlInput.trim().isEmpty()) {
            ctx.status(400);
            ctx.result("URL cannot be empty");
            return;
        }

        try {
            var uri = new URI(urlInput.trim());
            var urlObject = uri.toURL();

            if (!uri.isAbsolute()) {
                ctx.status(400);
                ctx.result("URL must be absolute (include protocol like http:// or https://)");
                return;
            }

            String normalizedUrl;
            if (urlObject.getPort() == -1 || urlObject.getPort() == urlObject.getDefaultPort()) {
                normalizedUrl = urlObject.getProtocol() + "://" + urlObject.getHost();
            } else {
                normalizedUrl = urlObject.getProtocol() + "://" + urlObject.getHost() + ":" + urlObject.getPort();
            }

            if (UrlRepository.findByName(normalizedUrl).isEmpty())
            {
                var url = new Url(normalizedUrl);
                UrlRepository.save(url);
                ctx.redirect("/urls");
            } else {
                ctx.status(400);
                ctx.result("Уже есть в БД");
            }


        } catch (URISyntaxException e) {
            ctx.status(400);
            ctx.result("Invalid URL syntax: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            ctx.status(400);
            ctx.result("Invalid URL: " + e.getMessage());
        } catch (MalformedURLException e) {
            ctx.status(400);
            ctx.result("Malformed URL: " + e.getMessage());
        } catch (Exception e) {
            ctx.status(500);
            ctx.result("Server error: " + e.getMessage());
        }
    }

    public static void show(Context ctx) throws SQLException {
        var id = ctx.pathParamAsClass("id", Long.class).get();
        var url = UrlRepository.find(id)
                .orElseThrow(() -> new NotFoundResponse("Entity with id = " + id + " not found"));
        var page = new UrlPage(url);
        ctx.render("urls/show.jte", model("page", page));
    }
}
