# query-builder

Java JPMS module (`module querybuilder`), single module, no build system beyond Maven.

## Commands

| Command         | Action                                                      |
|-----------------|-------------------------------------------------------------|
| `mvn compile`   | Compile sources into `target/classes/`.                     |
| `mvn test`      | Run 58 tests via JUnit 5 (`QueryBuilderTest`, `FilterBuilderTest`). |
| `mvn package`   | Create `target/querybuilder-1.0-SNAPSHOT.jar` (includes source and javadoc JARs). |
| `mvn clean`     | Remove `target/`.                                           |

No linter, no formatter, no CI.

## Structure

- `src/main/java/querybuilder/` — module root (`module-info.java` lives *inside* the module directory, not at the `src/main/java/` root)
- `src/main/java/querybuilder/Main.java` — entry point (run after packaging: `java -jar target/querybuilder-1.0-SNAPSHOT.jar`; was removed when the project became an API-only library)
- `src/test/java/querybuilder/api/` — tests (`QueryBuilderTest`, `FilterBuilderTest`)
- `src/main/java/querybuilder/api/` — public API (`QueryBuilder`, `FilterBuilder`)
- `module-info.java` — `requires java.sql`; exports `querybuilder.api`
- `.classpath`, `.project`, `.settings/` — Eclipse project metadata, committed

## API Architecture

### QueryBuilder — fluent SQL builder

Entry points (static):
- `QueryBuilder.select(String... columns)` → `Select`
- `QueryBuilder.delete()` → `Delete`
- `QueryBuilder.insertInto(String table, String... columns)` → `Insert`
- `QueryBuilder.update(String table)` → `Update`

Each returns an inner-class builder step. Chain methods like `.from()`, `.where()`, `.set()`, `.values()`, `.orderBy()`, `.limit()`, `.offset()`, `.join().on().eq()`.

Call `.build()` on a terminal clause (`TerminalClause`) to get `record Query(String query, List<Object> values)`. The query string uses `?` placeholders for all values — JDBC prepared-statement friendly. No trailing semicolon.

### FilterBuilder — filter conditions

Static methods return `Condition`: `eq()`, `not_eq()`, `like()`, `not_like()`, `in()`, `not_in()`, `between()`, `not_between()`, `and()`, `or()`, `not()`.

`Condition` also has instance methods (`.and().eq(...)`, `.or().like(...)`, etc.) for chaining inline predicates. Use `and(Condition, Condition)` / `or(Condition, Condition)` to combine two conditions.

### Subqueries

`in(name, TerminalClause)` and `not_in(name, TerminalClause)` accept a subquery. `From extends TerminalClause`, so `select(...).from(...)` can be passed directly:
```java
in("CustomerID", select("CustomerID").from("Orders"))
```

### Conditions are single-use

Each condition internally holds a `StringJoiner` and `values` list. The `build()` call (package-private) consumes them — do not reuse a `Condition` after it has been built.
