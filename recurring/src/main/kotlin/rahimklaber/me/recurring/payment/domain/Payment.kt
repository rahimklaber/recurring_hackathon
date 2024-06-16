package rahimklaber.me.recurring.payment.domain

import me.rahimklaber.stellar.base.Asset
import me.rahimklaber.stellar.base.Network
import me.rahimklaber.stellar.base.xdr.AlphaNum4
import me.rahimklaber.stellar.base.xdr.AssetCode4
import me.rahimklaber.stellar.base.xdr.contractId
import org.jetbrains.exposed.sql.*
import rahimklaber.me.recurring.merchant.domain.MerchantId
import java.math.BigInteger
import java.time.Instant
import java.util.concurrent.TimeUnit
import me.rahimklaber.stellar.base.xdr.Asset as XdrAsset

sealed interface DomainAsset{
    val name: String
    val symbol: String
    val decimals: Int

    data class Soroban(override val name: String, override val symbol: String, override val decimals: Int, val tokenId: String): DomainAsset
    data class Classic(override val name: String, override val symbol: String, override val decimals: Int = 7, val issuer: String): DomainAsset
    data object Native: DomainAsset {
        override val name: String
            get() = "Stellar Lumens"
        override val symbol: String
            get() = "XLM"
        override val decimals: Int
            get() = 7
    }

    fun tokenId(network: Network = Network.TESTNET): String{
        return when(this){
            is Classic -> Asset.AlphaNum(symbol, issuer).toXdr().contractId(network)
            Native -> XdrAsset.Native.contractId(network)
            is Soroban -> tokenId(network)
        }
    }

    fun encodeToString() = when(this){
        is Classic -> "$symbol:$issuer"
        Native -> "native"
        is Soroban -> tokenId
    }

    companion object{
        fun fromString(encoded: String):DomainAsset{
            val lower = encoded.lowercase()

            return if(encoded.startsWith("C") && lower.length == 56){
                Soroban("", "", 7, tokenId = lower)
            } else if(lower == "native"){
                return Native
            }else{
                val (symbol, issuer) = lower.split(":")

                return Classic(symbol, symbol, issuer = issuer)
            }
        }
    }
}

@JvmInline
value class MandateId(val value: ULong)

@JvmInline
value class TransactionHash(val value: String)

data class Mandate(
    val id: MandateId,
    val amount: BigInteger,
    val merchantId: ULong,
    val shoppperAddress: String,
    val transactionHash: TransactionHash,
    val createdAtUtc: Instant,
    val interval: Long,
    val asset: DomainAsset,
    val lastChargedUtc: Instant? = null,
    val revokedAtUtc: Instant? = null,
)

object MandateSchema: Table(){
    val id = ulong("id")

    val amount = ulong("amount") //todo support amounts more than 64 bits
    val merchantId = ulong("merchant_id").index()
    val shoppperAddress = text("shoppper_address").index()
    val transactionHash = text("transaction_hash").index()
    val interval = long("interval")
    val createdAtUtc = long("created_at_utc")
    val revokedAtUtc = long("revoked_at_utc").nullable()
    val lastChargedUtc = long("last_charged_at_utc").default(0)
    val asset = text("asset") // symbol:address or soroban address


    override val primaryKey = PrimaryKey(id)
}

fun getChargeIntervalInEpochSeconds(interval: String): Long{
    val number = interval.takeWhile { it.isDigit() }.toLong()
    return when(interval.last()){
        'd'-> TimeUnit.DAYS.toSeconds(number)
        'm' -> TimeUnit.DAYS.toSeconds(30) * number
        'y' -> TimeUnit.DAYS.toSeconds(365) * number
        's' -> number
        else -> throw IllegalArgumentException("charge interval invalid format")
    }
}

@JvmInline
value class PaymentId(val value: ULong)

data class Payment(
    val id: PaymentId,
    val description: String,
    val amount: BigInteger,
    val asset: DomainAsset,
    val merchantId: MerchantId,
    val shopperAddress: String?,
    val sequence: String /*first|subsequent*/,
    val mandateId: MandateId?,
    val state: String,
    val createdAtUtc: Instant,
    val attempts: Int,
    val reported : Boolean
)

object PaymentSchema: Table(){
    val id = ulong("id").autoIncrement()

    val description = text("description")

    val amount = ulong("amount") //todo support amounts more than 64 bits
    val asset = text("asset") // symbol:address or soroban address

    val merchantId = ulong("merchant_id").index()
    val shoppperAddress = text("shoppper_address").nullable()
    val sequence = text("sequence")

    val mandateId = ulong("mandate_id").nullable()
    val state = text("state").index()
    val createdAtUtc = long("created_at_utc")

    val attempts = integer("attempts").default(0)
    val reported = bool("reported").default(false)

    override val primaryKey = PrimaryKey(id)
}