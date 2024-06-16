package rahimklaber.me.recurring.payment.api

import io.ktor.server.routing.*
import rahimklaber.me.recurring.payment.domain.PaymentService

fun Routing.paymentRoutes(paymentService: PaymentService) {
    createPayment(paymentService)
}