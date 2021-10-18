import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.bettercoding.generated.Users
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun initDatabase(){
    val hikariConfig = HikariConfig("db.properties")
    val dataSource = HikariDataSource(hikariConfig)

    val flyway = Flyway.configure().dataSource(dataSource).load()
    flyway.migrate()

    Database.connect(dataSource)
}

fun main() {
    initDatabase()

    transaction {
        Users.deleteAll()

        Users.insert {
            it[name] = "Stefan"
            it[age] = 30
        }

        Users.selectAll().forEach {
            println(it[Users.name])
        }
    }
}
