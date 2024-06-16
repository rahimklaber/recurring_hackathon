package rahimklaber.me.recurring.payment.domain

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import rahimklaber.me.recurring.merchant.domain.MerchantId
import rahimklaber.me.recurring.plugins.KVSchema
import rahimklaber.me.recurring.plugins.dbQuery
import java.math.BigInteger
import java.time.Instant

class PaymentService {

    suspend fun createFirstPayment(
        amount: BigInteger,
        asset: DomainAsset,
        merchantId: MerchantId,
        description: String,
        interval: String,
    ): Payment {

        return dbQuery {
            val row = PaymentSchema
                .insert {
                    it[PaymentSchema.amount] = amount.toString().toULong() //todo...
                    it[PaymentSchema.asset] = asset.encodeToString()
                    it[PaymentSchema.description] = description
                    it[PaymentSchema.merchantId] = merchantId.value
                    it[sequence] = "first"
                    it[state] = "created"
                    it[createdAtUtc] = Instant.now().epochSecond
                }.resultedValues?.firstOrNull() ?: error("failed creating payment")

            val payment = paymentFromRow(row)

            KVSchema.insert {
                it[paymentId] = payment.id.value
                it[name] = "charge_interval"
                it[value] = interval
            }

            payment
        }

    }

    // Note: The "first" payment is not an actual payment. its just proof of the mandate creation
    suspend fun setFirstPaymentToSuccess(paymentId: PaymentId, mandateId: MandateId, txHash: TransactionHash, address: String){
        val payment = getPayment(paymentId)
        require(payment != null)
        require(payment.state == "created" )

        val interval = getInterval(paymentId)
        require(interval != null)

        dbQuery {
            MandateSchema.insert {
                it[id] = mandateId.value
                it[amount] = payment.amount.toLong().toULong()
                it[merchantId] = payment.merchantId.value
                it[shoppperAddress] = address
                it[transactionHash] = txHash.value
                it[createdAtUtc] = Instant.now().epochSecond
                it[MandateSchema.interval] = getChargeIntervalInEpochSeconds(interval)
                it[MandateSchema.asset] = payment.asset.encodeToString()
            }

            PaymentSchema.update({ PaymentSchema.id eq paymentId.value }) {
                it[state] = "success"
            }
        }
    }

    suspend fun getPayment(
        id: PaymentId,
    ): Payment? {
        return dbQuery {
            PaymentSchema
                .select(PaymentSchema.id eq id.value)
                .firstOrNull()
                ?.let {
                    paymentFromRow(it)
                }
        }
    }

    suspend fun getInterval(id: PaymentId): String?{
        return dbQuery {
            KVSchema
                .select((KVSchema.paymentId eq id.value) and (KVSchema.name eq "charge_interval"))
                .firstOrNull()?.get(KVSchema.value)
        }
    }

}

fun paymentFromRow(row: ResultRow): Payment {
    return Payment(
        PaymentId(row[PaymentSchema.id]),
        row[PaymentSchema.description],
        row[PaymentSchema.amount].toString().toBigInteger(), //todo
        DomainAsset.fromString(row[PaymentSchema.asset]),
        MerchantId(row[PaymentSchema.merchantId]),
        row[PaymentSchema.shoppperAddress],
        row[PaymentSchema.sequence],
        row[PaymentSchema.mandateId]?.let { MandateId(it) },
        row[PaymentSchema.state],
        Instant.ofEpochSecond(row[PaymentSchema.createdAtUtc]),
        row[PaymentSchema.attempts],
        row[PaymentSchema.reported]
    )
}
