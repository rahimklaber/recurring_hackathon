package rahimklaber.me.recurring.merchant.domain



@JvmInline
value class MerchantId(val value: ULong)

data class Merchant(
    val id: MerchantId
)
