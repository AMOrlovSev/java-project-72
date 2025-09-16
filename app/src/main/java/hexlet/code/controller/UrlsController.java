package hexlet.code.controller;

import hexlet.code.dto.urls.UrlsPage;
import hexlet.code.model.Url;
import hexlet.code.repository.UrlRepository;
import io.javalin.http.Context;

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
        var name = urlInput;
        var url = new Url(name);
        UrlRepository.save(url);
        ctx.redirect("/urls");
    }
}
