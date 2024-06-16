package rahimklaber.me.recurring.payment.presentation

import io.ktor.server.routing.*
import rahimklaber.me.recurring.payment.domain.PaymentService

fun Routing.paymentViews(paymentService: PaymentService) {
    confirmMandate(paymentService)
    signMandate(paymentService)
}