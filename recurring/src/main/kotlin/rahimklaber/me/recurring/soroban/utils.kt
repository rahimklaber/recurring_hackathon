package rahimklaber.me.recurring.soroban

import arrow.core.Either
import arrow.core.computations.ResultEffect.bind
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.raise.catch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import me.rahimklaber.stellar.GetTransactionResponse
import me.rahimklaber.stellar.SendTransactionResponse
import me.rahimklaber.stellar.SorobanClient
import me.rahimklaber.stellar.base.*
import me.rahimklaber.stellar.base.Transaction
import me.rahimklaber.stellar.base.operations.InvokeHostFunction
import me.rahimklaber.stellar.base.xdr.*
import me.rahimklaber.stellar.base.xdr.soroban.*
import me.rahimklaber.stellar.base.xdr.toXdrString
import java.math.BigInteger

val zeroAccount = Account(StrKey.encodeAccountId(ByteArray(32) { 0b0 }), 0L)

fun getReturnValue(transactionMeta: TransactionMeta) = (transactionMeta as TransactionMeta.V3).v3.sorobanMeta!!.returnValue

fun createSymbol(value: String) = SCVal.Symbol(SCSymbol(value))

fun createAddressXdr(value: String) = catch({
    SCVal.Address(ScAddress.Account(StrKey.encodeToAccountIDXDR(value)))
}) {
    SCVal.Address(ScAddress.Contract(Hash(StrKey.decodeContractAddress(value))))
}

fun addressFromScVal(value: SCVal.Address) = when(value.address){
    is ScAddress.Account -> StrKey.encodeAccountId(((value.address as ScAddress.Account).accountId.publicKey as PublicKey.PublicKeyEd25519).ed25519.byteArray)
    is ScAddress.Contract -> StrKey.encodeCheck(VersionByte.CONTRACT, (value.address as ScAddress.Contract).contractId.byteArray)
}


fun createI128(value: String): SCVal.I128 {


    //TODO return the full 128 bits

    return SCVal.I128(
        Int128Parts(
            low = value.toULong(),
            hi = 0
        )
    )
}

fun SCVal.I128.stringValue() = i128.low.toString()

fun createU64(value: ULong) = SCVal.U64(value)
fun createU32(value: UInt) = SCVal.U32(value)

inline operator fun SCMap.get(key: String) = mapEntries.find { it.key == createSymbol(key) }?.value

fun createSorobanTransaction(source: Account, contractId: String, function: String, args: List<SCVal> = listOf(), network: Network, fee: UInt = 50_0000_000u): Transaction {
    return transactionBuilder(source, network){
        addOperation(
            InvokeHostFunction(
                InvokeHostFunctionOp(
                    HostFunction.InvokeContract(
                        InvokeContractArgs(
                            StrKey.encodeToScAddress(contractId),
                            createSymbol(function).symbol,
                            args
                        ),
                    ),
                    listOf(),
                ),
            )
        )
        setFee(fee)
    }
}

/**
 * 1. Simulates the transactions
 * 2. Adds the sorobandata and authentry to transaction
 */
suspend fun prepareTransaction(sorobanClient: SorobanClient, transaction: Transaction): Either<Throwable, Transaction> = either{
    val simulateRes = sorobanClient.simulateTransaction(transaction.toEnvelopeXdr().toXdrString())

    println("min resource fee = ${simulateRes.minResourceFee}")

    ensure(simulateRes.error == null){
        IllegalStateException("Rpc call returned error: ${simulateRes.error}")
    }

    ensureNotNull(simulateRes.transactionData){
        IllegalStateException("Simulate rpc call does not have any sorobanData")
    }

    var newTx = transaction
        .withSorobanData(SorobanTransactionData.decodeFromString(simulateRes.transactionData!!))
        .copy(fee = simulateRes.minResourceFee!!.toUInt() + 100_000_000u)

    newTx = catch({
        newTx.withAuthEntry(SorobanAuthorizationEntry.decodeFromString(simulateRes.results!!.first().auth.first()))
    }){
        newTx
    }

    newTx
}

/**
 * 1. Creates the tx
 * 2. Simulates the tx
 * 3. adds the sorobandata and authentry to the tx
 * 4. submit the tx
 */
suspend fun createPrepareAndSubmitTx(sorobanClient: SorobanClient, source: Account, contractId: String, function: String, args: List<SCVal> = listOf(), network: Network, sign: suspend (Transaction) -> Unit) = run{
    var tx =
        createSorobanTransaction(source, contractId, function, args, network)

    tx = prepareTransaction(sorobanClient, tx).bind()

    sign(tx)

    val (_, submitResponse) = submitWithRpc(sorobanClient, tx.toEnvelopeXdr().toXdrString()).bind()

    val transactionMeta = TransactionMeta.decodeFromString(submitResponse.resultMetaXdr!!) as TransactionMeta.V3

    getReturnValue(transactionMeta)
}

suspend fun submitWithRpc(rpc: SorobanClient, txBlob: String) = either<Throwable, Pair<SendTransactionResponse, GetTransactionResponse>> {
    val result = rpc.sendTransaction(txBlob)

    ensure(result.status == "PENDING"){
        IllegalStateException("Submission failed when sending transaction $result")
    }

    val ok = withTimeoutOrNull(10_000){
        while (true){
            delay(1000)

            val getTransactionResponse = rpc.getTransaction(result.hash)

            when(getTransactionResponse.status){
                "FAILED" -> raise(IllegalStateException("Submission failed $getTransactionResponse"))
                "SUCCESS" -> return@withTimeoutOrNull getTransactionResponse
            }
        }
    }

    if(ok == null){
        raise(IllegalStateException("Submission failed"))
    }

    result to ok as GetTransactionResponse
}

fun Network.name() = when(this){
    Network.TESTNET -> "TESTNET"
    Network.PUBLIC -> "PUBLIC"
    else -> error("unknown network $this")
}