package rahimklaber.me.recurring.payment.worker

import arrow.core.computations.ResultEffect.bind
import me.rahimklaber.stellar.SorobanClient
import me.rahimklaber.stellar.horizon.Server
import me.rahimklaber.stellar.horizon.toAccount
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import rahimklaber.me.Config
import rahimklaber.me.recurring.payment.domain.*
import rahimklaber.me.recurring.plugins.dbQuery
import rahimklaber.me.recurring.soroban.*
import java.time.Instant

suspend fun chargeMandate(horizon: Server, rpc: SorobanClient, paymentService: PaymentService) {
    val now = Instant.now().epochSecond

    val mandate = dbQuery {
        MandateSchema.select {
           ((MandateSchema.interval plus MandateSchema.lastChargedUtc) less now)
        }.map(::mandateFromRow)
            .firstOrNull()
    }

    if (mandate == null)
        return

   dbQuery {
       val row = PaymentSchema
           .insert {
               it[PaymentSchema.amount] = mandate.amount.toString().toULong() //todo...
               it[PaymentSchema.asset] = mandate.asset.encodeToString()
               it[PaymentSchema.description] = "recurring charge"
               it[PaymentSchema.merchantId] = mandate.merchantId
               it[sequence] = "subsequent"
               it[state] = "created"
               it[createdAtUtc] = Instant.now().epochSecond
           }.resultedValues?.firstOrNull() ?: error("failed creating payment")

       val payment = paymentFromRow(row)

       val account = horizon.accounts().account(Config.key.accountId).toAccount()

       var transaction = createPrepareAndSubmitTx(rpc, account, Config.contractId.value, "charge_mandate", listOf(createU64(mandate.id.value)), Config.network.value){
           it.sign(Config.key)
       }

       MandateSchema.update({ MandateSchema.id eq mandate.id.value } ){
           it[MandateSchema.lastChargedUtc] = Instant.now().epochSecond
       }

       PaymentSchema.update({ PaymentSchema.id eq payment.id.value }) {
           it[state] = "success"
       }
   }
}

fun mandateFromRow(row: ResultRow): Mandate{
    return Mandate(
        MandateId(row[MandateSchema.id]),
       row[MandateSchema.amount].toLong().toBigInteger(),
       row[MandateSchema.merchantId],
       row[MandateSchema.shoppperAddress],
       TransactionHash(row[MandateSchema.transactionHash]),
        Instant.ofEpochSecond(row[MandateSchema.createdAtUtc]),
        row[MandateSchema.interval],
        DomainAsset.fromString(row[MandateSchema.asset]),
        row[MandateSchema.lastChargedUtc]?.let{Instant.ofEpochSecond(it)},
        row[MandateSchema.revokedAtUtc]?.let{Instant.ofEpochSecond(it)},
    )
}