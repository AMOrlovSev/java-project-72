@import hexlet.code.dto.urls.UrlsPage
@import java.time.format.DateTimeFormatter
@param UrlsPage page

@template.layout.page(
content = @`
    <section>
        <div class="container-lg mt-5">
            <h1>Сайты</h1>
            @if(page.getUrls().isEmpty())
                <div class="alert alert-secondary mt-3 rounded-0 m-0 fade show" role="alert">
                    Список пуст
                </div>
            @else
                <table class="table table-bordered table-striped table-hover table-responsive mt-3">
                    <thead class="table-dark">
                    <tr>
                        <th class="col-1">ID</th>
                        <th>Имя</th>
                        <th class="col-2">Последняя проверка</th>
                        <th class="col-1">Код ответа</th>
                    </tr>
                    </thead>
                    <tbody>
                    @for(var url : page.getUrls())
                        <tr>
                            <td>
                                ${url.getId()}
                            </td>
                            <td>
                                <a href="${"/urls/"+url.getId()}" class="text-decoration-none">
                                    ${url.getName()}
                                </a>
                            </td>
                            <td>
                                @if(!url.getUrlChecks().isEmpty())
                                    ${url.getUrlChecks().get(0).getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}
                                @endif
                            </td>
                            <td>
                                @if(!url.getUrlChecks().isEmpty())
                                    ${url.getUrlChecks().get(0).getStatusCode()}
                                @endif
                            </td>
                        </tr>
                    @endfor
                    </tbody>
                </table>
            @endif
        </div>
    </section>
`,
flash = page.getFlash(),
flashType = page.getFlashType()
)