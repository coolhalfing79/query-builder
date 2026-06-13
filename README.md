# query-builder

A fluent SQL query builder for Java with JDBC prepared-statement support. Generates parameterized SQL with `?` placeholders and a matching list of values.

## Usage

Add the dependency (once published to Maven Central):

```xml
<dependency>
  <groupId>io.github.coolhalfing79</groupId>
  <artifactId>querybuilder</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

The module name is `querybuilder` and the public API is in package `querybuilder.api`.

### SELECT

```java
import static querybuilder.api.QueryBuilder.*;
import static querybuilder.api.FilterBuilder.*;

var q = select("id", "name").from("users")
        .where(and(eq("active", true), gt("age", 21)))
        .orderBy("name").desc()
        .limit(10)
        .build();

// q.query()  -> "SELECT id, name FROM users WHERE active = ? AND age > ? ORDER BY name DESC FETCH NEXT ? ROWS ONLY"
// q.values() -> [true, 21, 10]
```

Use with JDBC:

```java
try (var pstmt = conn.prepareStatement(q.query())) {
    for (int i = 0; i < q.values().size(); i++) {
        pstmt.setObject(i + 1, q.values().get(i));
    }
    try (var rs = pstmt.executeQuery()) {
        while (rs.next()) { ... }
    }
}
```

### SELECT DISTINCT

```java
selectDistinct("status").from("users").where(gt("age", 18)).build();
// "SELECT DISTINCT status FROM users WHERE age > ?"
```

### DELETE

```java
delete().from("users").where(eq("status", "inactive")).build();
// "DELETE FROM users WHERE status = ?"
```

### INSERT

```java
insertInto("users", "name", "email").values("Alice", "alice@example.com").build();
// "INSERT INTO users (name, email) VALUES (?, ?)"
```

### INSERT ... SELECT

```java
insertInto("archive", "id", "name")
    .select("id", "name").from("users").where(eq("active", false))
    .build();
// "INSERT INTO archive (id, name) SELECT id, name FROM users WHERE active = ?"
```

### UPDATE

```java
update("users").set("status", "active").where(eq("id", 5)).build();
// "UPDATE users SET status = ? WHERE id = ?"
```

### JOINs

```java
select("*").from("users")
    .join("orders").on(colEq("users.id", "orders.customer_id"))
    .where(eq("users.status", "active"))
    .build();
// "SELECT * FROM users JOIN orders ON users.id = orders.customer_id WHERE users.status = ?"
```

Supported join types: `join`, `innerJoin`, `leftJoin`, `rightJoin`, `fullJoin`, `crossJoin`, `outerJoin`, `naturalJoin`. Each accepts a table name or a subquery (`TerminalClause`).

### Self-join with table alias

```java
select("e.name", "m.name")
    .from("employees", "e")
    .join("employees", "m").on(colEq("e.manager_id", "m.id"))
    .build();
// "SELECT e.name, m.name FROM employees e JOIN employees m ON e.manager_id = m.id"
```

### USING clause

```java
select("*").from("a").join("b").using("id").build();
// "SELECT * FROM a JOIN b USING (id)"
```

### Subqueries in FROM / JOIN

```java
select("sq.*").from(select("*").from("users")).as("sq").build();
// "SELECT sq.* FROM (SELECT * FROM users) AS sq"
```

### GROUP BY / HAVING

```java
select("department", "COUNT(*)").from("employees")
    .groupBy("department")
    .having(gt("COUNT(*)", 5))
    .orderBy("department")
    .build();
// "SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > ? ORDER BY department"
```

### UNION / INTERSECT / EXCEPT

```java
select("*").from("a").union(select("*").from("b")).build();
// "SELECT * FROM a UNION SELECT * FROM b"

select("*").from("a").union(select("*").from("b")).orderBy("id").build();
// "SELECT * FROM a UNION SELECT * FROM b ORDER BY id"
```

### EXISTS / NOT EXISTS

```java
where(exists(select("*").from("orders").where(colEq("orders.customer_id", "users.id"))))
// "WHERE EXISTS (SELECT * FROM orders WHERE orders.customer_id = users.id)"
```

### IS NULL / IS NOT NULL

```java
where(isNull("deleted_at"))
// "WHERE deleted_at IS NULL"
```

### Inline predicate chaining

```java
where(eq("status", "active").and().eq("type", "admin"))
// "WHERE status = ? AND type = ?"

where(eq("status", "active").and(eq("role", "admin"), eq("level", 5)))
// "WHERE status = ? AND ((role = ?) AND (level = ?))"
```

## Filters

The `FilterBuilder` class provides static methods for building `Condition` objects. All values are parameterized with `?` except `colEq()` which emits bare column references.

| Static method | SQL output |
|---|---|
| `eq(name, val)` | `name = ?` |
| `not_eq(name, val)` | `NOT name = ?` |
| `lt(name, val)` | `name < ?` |
| `not_lt(name, val)` | `NOT name < ?` |
| `gt(name, val)` | `name > ?` |
| `not_gt(name, val)` | `NOT name > ?` |
| `like(name, pattern)` | `name LIKE ?` |
| `not_like(name, pattern)` | `name NOT LIKE ?` |
| `in(name, vals...)` | `name IN (?, ?, ...)` |
| `in(name, subquery)` | `name IN (subquery)` |
| `not_in(name, vals...)` | `name NOT IN (?, ?, ...)` |
| `not_in(name, subquery)` | `name NOT IN (subquery)` |
| `between(name, a, b)` | `name BETWEEN ? AND ?` |
| `not_between(name, a, b)` | `name NOT BETWEEN ? AND ?` |
| `colEq(col1, col2)` | `col1 = col2` (no `?`) |
| `isNull(name)` | `name IS NULL` |
| `isNotNull(name)` | `name IS NOT NULL` |
| `exists(subquery)` | `EXISTS (subquery)` |
| `notExists(subquery)` | `NOT EXISTS (subquery)` |
| `and(c1, c2)` | `(c1) AND (c2)` |
| `or(c1, c2)` | `(c1) OR (c2)` |
| `not(c)` | `NOT (c)` |

## Quirks

- **Conditions are single-use.** Each `Condition` holds an internal buffer that is consumed when passed to `where()`, `on()`, `having()`, or a combiner method. Do not reuse a `Condition` after building.
- **No trailing semicolon.** The generated SQL does not include a trailing `;`. Add one if your JDBC driver requires it.
- **LIMIT uses SQL:2008 syntax.** The clause `FETCH NEXT ? ROWS ONLY` is standard SQL but is not supported by SQLite 3.x. Use PostgreSQL, H2, or Derby for LIMIT integration tests.
- **`DISTINCT` in `SELECT DISTINCT`.** The space after `DISTINCT` is part of the keyword concatenation: `SELECT DISTINCT col1, col2`.
- **`Join extends TerminalClause`.** A bare `crossJoin("t").build()` compiles without an `.on()` call since `Join` is itself a terminal clause.
- **`Union` replaces internal state.** Calling `build()` on a `Union` captures the combined query. The `Union` constructor eagerly consumes both its own prior state and the argument query.
- **Type promotion.** Instance methods `eq()`, `exists()`, and `notExists()` on `Condition` return a `CombinedCondition` to expose the two-argument `and(c1,c2)` / `or(c1,c2)` combiners. Other methods return `this` preserving the runtime type.

## Build

```bash
JAVA_HOME=/usr/lib/jvm/java-26 mvn compile    # compile sources
JAVA_HOME=/usr/lib/jvm/java-26 mvn test       # run 227 tests
JAVA_HOME=/usr/lib/jvm/java-26 mvn package    # create JAR + source + javadoc JARs
JAVA_HOME=/usr/lib/jvm/java-26 mvn clean      # remove target/
```

The project requires Java 26 (released March 2026). Default `mvn` uses JDK 25 on some systems; set `JAVA_HOME` explicitly.

## Module

The project is a Java JPMS module (`module querybuilder`). It requires `java.sql` and exports `querybuilder.api`.

## License

MIT
