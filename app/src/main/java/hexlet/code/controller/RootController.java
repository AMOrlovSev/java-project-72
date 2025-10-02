package hexlet.code.controller;

import hexlet.code.dto.MainPage;
import io.javalin.http.Context;

import static io.javalin.rendering.template.TemplateUtil.model;

public class RootController {
    public static void index(Context ctx) {
        String flash = ctx.consumeSessionAttribute("flash");
        String flashType = ctx.consumeSessionAttribute("flashType");
        var page = new MainPage(flash, flashType);
        ctx.render("index.jte", model("page", page));
    }
}
