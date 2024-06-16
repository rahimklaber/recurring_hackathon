package rahimklaber.me

import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.rahimklaber.stellar.horizon.Server
import me.rahimklaber.stellar.sorobanClient
import rahimklaber.me.recurring.payment.api.paymentRoutes
import rahimklaber.me.recurring.payment.domain.PaymentService
import rahimklaber.me.recurring.payment.presentation.paymentViews
import rahimklaber.me.recurring.payment.worker.chargeMandate
import rahimklaber.me.recurring.plugins.*

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureSerialization()
    configureDatabases()
    configureRouting()

    val paymentService = PaymentService()

    routing {
        paymentRoutes(paymentService)
        paymentViews(paymentService)
    }

    GlobalScope.launch {
        val horizon = Server("https://horizon-testnet.stellar.org")

        val rpcClient = sorobanClient("https://soroban-testnet.stellar.org")

        while (true) {
            delay(10_000)
            try {
                chargeMandate(horizon, rpcClient, paymentService)
            }catch (e: Exception){
                println("Error: ${e.message}")
            }
        }
    }
}
