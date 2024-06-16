package rahimklaber.me.recurring.payment.presentation

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import rahimklaber.me.recurring.formatAssetAmount
import rahimklaber.me.recurring.payment.domain.Payment
import rahimklaber.me.recurring.payment.domain.PaymentId
import rahimklaber.me.recurring.payment.domain.PaymentService
import rahimklaber.me.recurring.template

fun Routing.confirmMandate(paymentService: PaymentService) {
    get("/confirmmandate") {
        val id = call.request.queryParameters["id"]?.toULong()
        require(id != null) { "id is missing" }

        val payment = paymentService.getPayment(PaymentId(id))
        val interval = paymentService.getInterval(PaymentId(id))
        require(payment != null) { "payment is missing" }
        require(interval != null) { "interval is missing" }

        if (payment.state != "created" || payment.sequence != "first"){
            return@get call.respond(HttpStatusCode.BadRequest)
        }

        call.respondHtml {
            confirmMandatePage(payment, interval)
        }

    }
}

private fun HTML.confirmMandatePage(payment: Payment, interval: String) =
    template {
        script(src ="js/confirmmandate.js"){}
        form(classes = "mx-auto fit") {
            h3(classes = "mb-3") {
                text(payment.description)
            }

            div(classes = "mb-3") {
                text("You will pay ${formatAssetAmount(payment.amount.toString())} ${payment.asset.symbol} every ${interval}")
            }


            button(classes = "btn btn-primary") {
                onClick = "confirmMandate()"
                text("Confirm")
            }
        }
    }