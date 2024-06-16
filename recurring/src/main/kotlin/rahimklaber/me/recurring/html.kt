package rahimklaber.me.recurring

import kotlinx.html.*

fun HTML.template(content: HtmlBlockTag.() -> Unit) {
    head {
        link("https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css", "stylesheet")
        script("text/javascript", "https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/js/bootstrap.bundle.min.js") {}


        script("text/javascript", "https://cdnjs.cloudflare.com/ajax/libs/stellar-freighter-api/2.0.0/index.min.js") {}
        link("/css/template.css", "stylesheet")

    }
    body {
        content()
    }
}