package rahimklaber.me.recurring.payment.presentation


import arrow.core.computations.ResultEffect.bind
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import me.rahimklaber.stellar.SorobanClient
import me.rahimklaber.stellar.base.Network
import me.rahimklaber.stellar.base.xdr.*
import me.rahimklaber.stellar.base.xdr.soroban.SCVal
import me.rahimklaber.stellar.horizon.Server
import me.rahimklaber.stellar.horizon.toAccount
import me.rahimklaber.stellar.sorobanClient
import rahimklaber.me.Config
import rahimklaber.me.recurring.payment.domain.*
import rahimklaber.me.recurring.soroban.*
import rahimklaber.me.recurring.template
import java.time.Instant

@OptIn(ExperimentalStdlibApi::class)
fun Routing.signMandate(paymentService: PaymentService) {
    val horizon = Server("https://horizon-testnet.stellar.org")

    val rpcClient = sorobanClient("https://soroban-testnet.stellar.org")

    post("/signmandate") {
        val id = call.request.queryParameters["id"]?.toULong()
        require(id != null) { "id is missing" }

        val paymentId = PaymentId(id)

        val txBlob = call.request.queryParameters["tx"]
        require(txBlob != null) { "tx is missing" }

        val address = call.request.queryParameters["address"]
        require(address != null) { "address is missing" }

        val (sendResult, getResult) = submitWithRpc(rpcClient, txBlob).bind()

        val transactionMeta = TransactionMeta.decodeFromString(getResult.resultMetaXdr!!) as TransactionMeta.V3
        val returnValue = (getReturnValue(transactionMeta) as SCVal.U64).value

        paymentService.setFirstPaymentToSuccess(
            paymentId,
            MandateId(returnValue),
            TransactionHash(sendResult.hash),
            address
        )

        call.respondRedirect("https://giphy.com/embed/dmmBhPUnCSF9ibuTEo")


    }

    get("/signmandate") {
        val id = call.request.queryParameters["id"]?.toULong()
        require(id != null) { "id is missing" }
        val address = call.request.queryParameters["address"]
        require(address != null) { "address is missing" }

        val payment = paymentService.getPayment(PaymentId(id))
        val interval = paymentService.getInterval(PaymentId(id))
        require(payment != null) { "payment is missing" }
        require(interval != null) { "interval is missing" }
        require(payment.state == "created")

        val account = horizon.accounts().account(address).toAccount()

        val sorobanMandate = SorobanMandate(payment.amount, Instant.now(), Instant.ofEpochSecond(0), getChargeIntervalInEpochSeconds(interval).toULong(), address, Config.key.accountId, payment.asset.tokenId())

        var transaction = createSorobanTransaction(account, Config.contractId.value, "create_mandate", listOf(sorobanMandate.toScVal(), createU32((rpcClient.getLatestLedger().sequence + 100000).toUInt())), Config.network.value)

        transaction = prepareTransaction(rpcClient, transaction).bind()

        call.respondHtml {
            signMandatePage(transaction.toEnvelopeXdr().toXdrString(), Config.network.value)
        }

    }
}

private fun HTML.signMandatePage(blob: String, network: Network) =
    template {
        onLoad = "signmandate(\"$blob\", \"${network.name()}\")"
        script(src ="js/signmandate.js"){}
    }