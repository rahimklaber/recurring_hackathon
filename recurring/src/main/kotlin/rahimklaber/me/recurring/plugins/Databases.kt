package rahimklaber.me.recurring.plugins

import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import rahimklaber.me.recurring.payment.domain.MandateSchema
import rahimklaber.me.recurring.payment.domain.PaymentSchema

object KVSchema: Table("kv") {
    val paymentId = ulong("payment_id").index()
    val name = text("name")
    val value = text("value")
}

fun Application.configureDatabases() {
    Database.connect("jdbc:sqlite:yeet_db.sqlite")

    transaction {
        SchemaUtils.createMissingTablesAndColumns(PaymentSchema, KVSchema, MandateSchema)    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }