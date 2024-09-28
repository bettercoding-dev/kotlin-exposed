---
title: "Tutorial: How to set up Exposed ORM with Flyway Database Migration"
date: "2021-10-18"
categories: 
  - "kotlin"
---

_Almost every application needs to store data in a database. In this tutorial, we will set up a database connection in **Kotlin** using **Exposed** as an ORM and **Flyway** for database migrations._

## Goal of this Tutorial

In this tutorial, we will have a look how to implement a **connection to an SQL database in a Kotlin application**. We will have a look at the following topics:

- Handling database creation and migrations with **Flyway**
- **Generating Exposed table definitions** using a Gradle plugin
- Using **Exposed** as an ORM to abstract the database connection

The code for this tutorial is available on GitHub: [https://github.com/bettercoding-dev/kotlin-exposed](https://github.com/bettercoding-dev/kotlin-exposed).

## Prerequisites

First, we start by creating a new Kotlin application. I will use the **IntelliJ** wizard to do this. You can also check out the `starter` branch from this tutorial's GitHub repository: [https://github.com/bettercoding-dev/kotlin-exposed/tree/starter](https://github.com/bettercoding-dev/kotlin-exposed/tree/starter).

If we run the application using `./gradlew run`, we should see the following output:

```
Hello World!
Program arguments: 
```

## Database Setup and Migrations with Flyway

When using an SQL database, we need to define our tables using **DDL (data definition language)**, e.g. `CREATE TABLE`. Working on environments like **development, staging and production** requires us to keep the structure of our database the same on every environment.

This means, when adding a new table, the table needs to be added on each environment. Updating the database structure is also called **database migration** (migrating one structure to another).

One tool that helps us to keep our database structure up-to-date is **Flyway**. It allows us to specify `.sql`\-Files containing the database structure, which then Flyway executes in correct order to perform the database operations.

We will use **Postgres** as our database server in this example. You can run it using `docker-compose` and this file:

```
version: '3.1'

services:
  db:
    image: postgres:alpine
    environment:
      POSTGRES_DB: testDB
      POSTGRES_USER: test
      POSTGRES_PASSWORD: testpassword
    ports:
      - "5432:5432"
```

### Adding the dependencies

First, we start by adding the dependencies to **Flyway** and **Postgres** to our `build.gradle.kts` file. We add the following block to the file:

```
dependencies {
    implementation("org.flywaydb:flyway-core:8.0.1")
    implementation("org.postgresql:postgresql:42.2.24")
    implementation("com.zaxxer:HikariCP:5.0.0")
}
```

We also added `HikariCP` to our dependencies. This library helps us to create performant connections to the database.

### Adding a database configuration

Next, we need to create a file called `db.properties` in the project's root directory. This file contains all the information about **how to connect to our local database**.

The content of `db.properties` should look like this:

```
jdbcUrl=jdbc:postgresql://localhost:5432/testDB
dataSource.driverClass=org.postgresql.Driver
dataSource.driver=postgresql
dataSource.database=testDB
dataSource.user=test
dataSource.password=testpassword
```

Next, we go to our `main.kt` file and add an `initDatabase()` function and call it in the `main` function:

```
fun initDatabase(){
    val hikariConfig = HikariConfig("db.properties")
    val dataSource = HikariDataSource(hikariConfig)

    val flyway = Flyway.configure().dataSource(dataSource).load()
    flyway.migrate()
}

fun main() {
    initDatabase()
}
```

- **Line 2:** We create a `HikariConfig` that reads the properties from the `db.properties` file that we have created earlier.
- **Line 3:** With the `HikariConfig` we create a `HikariDataSource`.
- **Line 5:** The `dataSource` can then be used to create a new `Flyway` instance.
- **Line 6:** `flyway.migrate()` tells `Flyway` to start the database migration process.

When we now run the application, we should see something like this in the log:

```
Oct 17, 2021 4:45:56 PM org.flywaydb.core.internal.license.VersionPrinter printVersionOnly
INFO: Flyway Community Edition 8.0.1 by Redgate
Oct 17, 2021 4:45:56 PM org.flywaydb.core.internal.database.base.BaseDatabaseType createDatabase
INFO: Database: jdbc:h2:./data/exposed_migrations (H2 1.4)
Oct 17, 2021 4:45:56 PM org.flywaydb.core.internal.command.DbValidate validate
INFO: Successfully validated 0 migrations (execution time 00:00.060s)
Oct 17, 2021 4:45:56 PM org.flywaydb.core.internal.command.DbValidate validate
WARNING: No migrations found. Are your locations set up correctly?
Oct 17, 2021 4:45:56 PM org.flywaydb.core.internal.schemahistory.JdbcTableSchemaHistory create
INFO: Creating Schema History table "PUBLIC"."flyway_schema_history" ...
Oct 17, 2021 4:45:56 PM org.flywaydb.core.internal.command.DbMigrate migrateGroup
INFO: Current version of schema "PUBLIC": << Empty Schema >>
Oct 17, 2021 4:45:56 PM org.flywaydb.core.internal.command.DbMigrate logSummary
INFO: Schema "PUBLIC" is up to date. No migration necessary.

Process finished with exit code 0
```

When we have a look at our database, we see that a `flyway_schema_history` table has been created. In this table, Flyway keeps track of which migration files have been executed.

Next, we create a new directory structure `db/migration` in our `resources` folder. Within `migration` add a file called `V1__create_users_table.sql` and add the following content:

```
CREATE TABLE users
(
    id   INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR NOT NULL UNIQUE,
    age  INTEGER NOT NULL
);
```

Now, if we run our application again, we should see something like this:

```
INFO: Successfully applied 1 migration to schema "PUBLIC", now at version v1 (execution time 00:00.058s)
```

Also, when we look at our database, a table called `users` has been created.

For more information on how Flyway works, have a look at the Flyway documentation: [https://flywaydb.org/documentation/](https://flywaydb.org/documentation/).

## Exposed ORM with Code Generation

Now, that we have a database in place and have Flyway taking care of the migrations, let's have a look at **accessing the data from the database**. We will use Exposed to achieve this.

Exposed is an ORM (object-relational-mapper) by **JetBrains**, that abstracts the way we access the database. In other words, it takes care of different SQL dialects and lets us write database queries in **plain Kotlin**.

For Exposed to be able to map the database tables, it is necessary to also model the tables in Kotlin. Luckily, there is a **Gradle plugin** available that **generates** these definitions for us.

### Setting up code generation

The first thing necessary is that we make some changes in our `build.gradle.kts` file.

First, we add the following line to the `plugins` block in the file:

```
id("com.jetbrains.exposed.gradle.plugin") version "0.2.1"
```

Now a Gradle task `generateExposedCode` is available. Before we can run it, there are some more things to configure.

We need to add a **database configuration**, so the generator knows for which database the files should be generated. The configuration for our example looks like this:

```
exposedCodeGeneratorConfig {
    val dbProperties = loadProperties("${projectDir}/db.properties")
    configFilename = "exposedConf.yml"
    user = dbProperties["dataSource.user"].toString()
    password = dbProperties["dataSource.password"].toString()
    databaseName = dbProperties["dataSource.database"].toString()
    databaseDriver = dbProperties["dataSource.driver"].toString()
}
```

Place this snippet in your `build.gradle.kts` file as well. Note that we have referenced a file called `exposedConf.yml` here. This file is used for some **code-generation-specific settings**. Create the file and add this content:

```
generateSingleFile: false
packageName: dev.bettercoding.generated
```

We tell the generator that every table should be put in a **separate file**. Also, we can specify a **package name** for our generated classes.

Next, we also add these lines to the `build.gradle.kts` file:

```
sourceSets.main {
    java.srcDirs("build/tables")
}

tasks.generateExposedCode {
    dependsOn("clean")
}
```

The generated files are put to the `build/tables` directory. To be able to use the generated classes in our code, we need to **add this directory to our source directories**. This is done with the first block.

The `generateExposedCode` task does not overwrite existing classes. But this is what we want when updating table information. With the second block, we tell Gradle to clean the `build` folder before generating the Exposed code.

Now, when we run `./gradlew generateExposedCode`, we see that there are 2 new files under `build/tables`.

Unfortunately, there is a small bug in the current version of code generation. The `Public.flywaySchemaHistory.kt` file contains a compile error. Since we do not need this file anyway, we will just delete it. (I assume this bug will be fixed in future version and I will update this part here accordingly)

### Working with Exposed

Before we can use the newly generated classes, we need to **add Exposed to our dependencies**. Add these two lines to the `dependencies` block in your `build.gradle.kts` file:

```
implementation("org.jetbrains.exposed:exposed-core:0.35.2")
implementation("org.jetbrains.exposed:exposed-jdbc:0.35.2")
```

Now, in our `Main.kt` file, update the `initDatabase` function to look like this:

```
fun initDatabase(){
    val hikariConfig = HikariConfig("db.properties")
    val dataSource = HikariDataSource(hikariConfig)

    val flyway = Flyway.configure().dataSource(dataSource).load()
    flyway.migrate()

    Database.connect(dataSource)
}
```

The last line is all that is necessary to set up the database connection for Exposed. Luckily, we already have a `dataSource` available that we can reuse now.

Now let's insert and read a user to the database to test our implementation. Exposed transactions are executed using a `transaction` block.

```
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
```

- **Line 4:** The `transaction` block defines the area where we can run database operations
- **Line 5:** Delete all users
- **Lines 7-10:** Insert a new user
- **Lines 12-14:** Print all names from the users in the database

We did it! Now, we should see `Stefan` printed to our standard output.

For all you can do with Exposed, have a look at the official repository at GitHub: [https://github.com/JetBrains/Exposed](https://github.com/JetBrains/Exposed).

## Summary

In this tutorial, we set up a database connection in **Kotlin** using **Flyway** for database migrations and **Exposed** as our ORM library. For generating the table definitions in Exposed we used a **Gradle** plugin to do this job for us.

With this configuration, you have a good basis for building a production-ready application.
