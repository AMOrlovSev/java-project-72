package hexlet.code.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Url {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    List<UrlCheck> urlChecks;

    public Url(String name) {
        this.name = name;
        this.urlChecks = new ArrayList<>();
    }

    public final void addCheck(UrlCheck urlCheck) {
        urlChecks.add(urlCheck);
        urlCheck.setUrlId(id);
    }
}
