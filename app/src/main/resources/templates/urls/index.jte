@import hexlet.code.dto.urls.UrlsPage
@import java.time.format.DateTimeFormatter
@param UrlsPage page

@template.layout.page(
content = @`
    <div class="container mt-4">
        <h1>Список URL</h1>

        @if(page.getUrls().isEmpty())
            <div class="alert alert-info mt-3" role="alert">
                Список URL пуст
            </div>
        @else
            <div class="table-responsive">
                <table class="table table-striped table-hover">
                    <thead class="table-dark">
                    <tr>
                        <th>ID</th>
                        <th>URL</th>
                        <th>Дата создания</th>
                        <th>Действия</th>
                    </tr>
                    </thead>
                    <tbody>
                    @for(var url : page.getUrls())
                        <tr>
                            <td class="fw-bold">${url.getId()}</td>
                            <td>
                                <a href="${url.getName()}"
                                   target="_blank"
                                   class="text-decoration-none">
                                    ${url.getName()}
                                </a>
                            </td>
                            <td>
                                ${url.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}
                            </td>
                            <td>
                                <a href="/urls/${url.getId()}"
                                   class="btn btn-outline-primary btn-sm">
                                    Просмотр
                                </a>
                            </td>
                        </tr>
                    @endfor
                    </tbody>
                </table>
            </div>
        @endif
    </div>
`
)