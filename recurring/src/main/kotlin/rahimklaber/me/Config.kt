package rahimklaber.me

import kotlinx.coroutines.flow.MutableStateFlow
import me.rahimklaber.stellar.base.KeyPair
import me.rahimklaber.stellar.base.Network
import me.rahimklaber.stellar.base.fromSecretSeed

object Config {
    val network = MutableStateFlow(Network.TESTNET)
    val contractId = MutableStateFlow("CA5UEGQQAIM3FB6WI6ISRZ4B3YMC7A6EKYKDLKBTT6W6453G7VUSDSX7")
    val key = KeyPair.fromSecretSeed("SDTSXOBTO56QZSR6MMUC4TQV47Y2HLWFPKHOJJJOBYSZJHIKQOISA4JT")
}