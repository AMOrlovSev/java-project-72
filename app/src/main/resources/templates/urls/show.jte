@import hexlet.code.dto.urls.UrlPage
@import java.time.format.DateTimeFormatter
@param UrlPage page

@template.layout.page(
content = @`
    <div class="container mt-4">
        <h1>URL</h1>
        <main>
            <h1>${page.getUrl().getId()}</h1>
            <p>${page.getUrl().getName()}</p>
            <p>${page.getUrl().getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}</p>
        </main>
    </div>
`
)