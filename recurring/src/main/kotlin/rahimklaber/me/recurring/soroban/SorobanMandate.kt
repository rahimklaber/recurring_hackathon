package rahimklaber.me.recurring.soroban

import me.rahimklaber.stellar.base.xdr.soroban.SCMap
import me.rahimklaber.stellar.base.xdr.soroban.SCMapEntry
import me.rahimklaber.stellar.base.xdr.soroban.SCVal
import java.math.BigInteger
import java.time.Instant

data class SorobanMandate(
    val amount: BigInteger,
    val start: Instant,
    val lastCharged: Instant,
    val chargeInterval: ULong,
    val shopper: String,
    val merchant: String,
    val tokenId: String,

    val id: ULong = 0u // not actually part of the model
) {
    companion object {}
}

fun SorobanMandate.toScVal(): SCVal{
    return SCVal.Map(
        SCMap(
            listOf(
                SCMapEntry(createSymbol("amount"), createI128(amount.toString())),
                SCMapEntry(createSymbol("charge_interval"), createU64(chargeInterval)),
                SCMapEntry(createSymbol("last_charged"), createU64(lastCharged.epochSecond.toULong())),
                SCMapEntry(createSymbol("merchant"), createAddressXdr(merchant)),
                SCMapEntry(createSymbol("shopper"), createAddressXdr(shopper)),
                SCMapEntry(createSymbol("start"), createU64(start.epochSecond.toULong())),
                SCMapEntry(createSymbol("token_id"), createAddressXdr(tokenId)),
            )
        )
    )
}