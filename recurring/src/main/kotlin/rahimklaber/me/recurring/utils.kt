package rahimklaber.me.recurring

fun formatAssetAmount(amount: String): String{
    return if (amount.length > 7)
        amount.take(amount.length - 7) + "." + amount.takeLast(7)
            .take(2)
    else{
        "0." + amount.take(2).padStart(7 - amount.length, '0')
    }
}