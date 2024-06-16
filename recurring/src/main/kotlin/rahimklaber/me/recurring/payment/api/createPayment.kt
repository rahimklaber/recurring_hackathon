package rahimklaber.me.recurring.payment.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import rahimklaber.me.recurring.merchant.domain.MerchantId
import rahimklaber.me.recurring.payment.domain.DomainAsset
import rahimklaber.me.recurring.payment.domain.MandateId
import rahimklaber.me.recurring.payment.domain.PaymentService
import rahimklaber.me.recurring.plugins.KVSchema.paymentId

@JsonClassDiscriminator("type")
@Serializable()
sealed class CreatePaymentRequest{
    abstract val type: String

    @SerialName("first")
    @Serializable
    data class CreatFirstPaymentRequest(
        val amount: String,
        val asset: String,
        val chargeInterval: String,
        val description: String = "", override val type: String,
    ): CreatePaymentRequest() {
        init {
            require(amount.length < 50)
            require(asset.length < 120)
            require(description.length < 256)
            require(Regex("[0-9]+[myds]").matches(chargeInterval))
        }
    }

    @SerialName("subsequent")
    @Serializable
    data class CreatSubsequentPaymentRequest(
        val mandateId: ULong, override val type: String,
    ): CreatePaymentRequest() {

    }
}

@Serializable
data class CreatePaymentResponse(
    val paymentId: String,
    val redirectUrl: String,
)

fun Routing.createPayment(service: PaymentService) {
    post("/payment") {
        val merchantId = MerchantId(1uL)

        val params : CreatePaymentRequest = call.receive();

        if(params is CreatePaymentRequest.CreatFirstPaymentRequest) {
            val payment = service.createFirstPayment(
                params.amount.toBigInteger(),
                DomainAsset.fromString(params.asset),
                merchantId,
                params.description,
                params.chargeInterval,
            )

            call.respond(HttpStatusCode.Created, CreatePaymentResponse(payment.id.value.toString(), "http://localhost:8080/confirmmandate?id=${payment.id.value}"))
        }else{

        }


    }
}

