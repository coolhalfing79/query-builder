package querybuilder.api;

import static querybuilder.api.FilterBuilder.*;
import static querybuilder.api.QueryBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class QueryBuilderTest {

    @Test
    void selectAll() {
        var q = select("*").from("users").build();
        assertEquals("SELECT * FROM users", q.query());
        assertTrue(q.values().isEmpty());
    }

    @Test
    void selectDistinctAllColumns() {
        var q = selectDistinct("*").from("users").build();
        assertEquals("SELECT DISTINCT * FROM users", q.query());
    }

    @Test
    void selectDistinctSpecificColumns() {
        var q = selectDistinct("city", "country").from("users").build();
        assertEquals("SELECT DISTINCT city, country FROM users", q.query());
    }

    @Test
    void selectDistinctWithWhere() {
        var q = selectDistinct("status").from("users")
                .where(gt("age", 21))
                .build();
        assertEquals("SELECT DISTINCT status FROM users WHERE age > ?", q.query());
        assertEquals(List.of(21), q.values());
    }

    @Test
    void selectDistinctWithOrderBy() {
        var q = selectDistinct("name").from("users")
                .orderBy("name").build();
        assertEquals("SELECT DISTINCT name FROM users ORDER BY name", q.query());
    }

    @Test
    void selectWhereEq() {
        var q = select("*").from("users").where(eq("id", 1)).build();
        assertEquals("SELECT * FROM users WHERE id = ?", q.query());
        assertEquals(List.of(1), q.values());
    }

    @Test
    void selectWhereNotEq() {
        var q = select("*").from("users").where(not_eq("id", 1)).build();
        assertEquals("SELECT * FROM users WHERE NOT id = ?", q.query());
        assertEquals(List.of(1), q.values());
    }

    @Test
    void selectWhereLike() {
        var q = select("*").from("users").where(like("name", "j%")).build();
        assertEquals("SELECT * FROM users WHERE name LIKE ?", q.query());
        assertEquals(List.of("j%"), q.values());
    }

    @Test
    void selectWhereNotLike() {
        var q = select("*").from("users").where(not_like("name", "j%")).build();
        assertEquals("SELECT * FROM users WHERE name NOT LIKE ?", q.query());
        assertEquals(List.of("j%"), q.values());
    }

    @Test
    void selectWhereIn() {
        var q = select("*").from("users").where(in("id", 1, 2, 3)).build();
        assertEquals("SELECT * FROM users WHERE id IN (?, ?, ?)", q.query());
        assertEquals(List.of(1, 2, 3), q.values());
    }

    @Test
    void selectWhereInSubquery() {
        var q = select("*").from("users")
                .where(in("id", select("id").from("orders")))
                .build();
        assertEquals("SELECT * FROM users WHERE id IN (SELECT id FROM orders)", q.query());
    }

    @Test
    void selectWhereNotIn() {
        var q = select("*").from("users").where(not_in("id", 1, 2)).build();
        assertEquals("SELECT * FROM users WHERE id NOT IN (?, ?)", q.query());
        assertEquals(List.of(1, 2), q.values());
    }

    @Test
    void selectWhereNotInSubquery() {
        var q = select("*").from("users")
                .where(not_in("id", select("id").from("orders")))
                .build();
        assertEquals("SELECT * FROM users WHERE id NOT IN (SELECT id FROM orders)", q.query());
    }

    @Test
    void selectWhereBetween() {
        var q = select("*").from("users").where(between("id", 1, 10)).build();
        assertEquals("SELECT * FROM users WHERE id BETWEEN ? AND ?", q.query());
        assertEquals(List.of(1, 10), q.values());
    }

    @Test
    void selectWhereNotBetween() {
        var q = select("*").from("users").where(not_between("id", 1, 10)).build();
        assertEquals("SELECT * FROM users WHERE id NOT BETWEEN ? AND ?", q.query());
        assertEquals(List.of(1, 10), q.values());
    }

    @Test
    void selectWhereAnd() {
        var q = select("*").from("users")
                .where(and(eq("id", 1), eq("name", "jon")))
                .build();
        assertEquals("SELECT * FROM users WHERE ( id = ? ) AND ( name = ? )", q.query());
        assertEquals(List.of(1, "jon"), q.values());
    }

    @Test
    void selectWhereOr() {
        var q = select("*").from("users")
                .where(or(eq("id", 1), eq("id", 2)))
                .build();
        assertEquals("SELECT * FROM users WHERE ( id = ? ) OR ( id = ? )", q.query());
        assertEquals(List.of(1, 2), q.values());
    }

    @Test
    void selectWhereNot() {
        var q = select("*").from("users")
                .where(not(eq("name", "jon")))
                .build();
        assertEquals("SELECT * FROM users WHERE NOT ( name = ? )", q.query());
        assertEquals(List.of("jon"), q.values());
    }

    @Test
    void selectWhereChained() {
        var q = select("*").from("users")
                .where(eq("first_name", "jon").and().eq("last_name", "doe"))
                .build();
        assertEquals("SELECT * FROM users WHERE first_name = ? AND last_name = ?", q.query());
        assertEquals(List.of("jon", "doe"), q.values());
    }

    @Test
    void selectWhereChainedOr() {
        var q = select("*").from("users")
                .where(eq("first_name", "jon").or().eq("first_name", "jane"))
                .build();
        assertEquals("SELECT * FROM users WHERE first_name = ? OR first_name = ?", q.query());
        assertEquals(List.of("jon", "jane"), q.values());
    }

    @Test
    void selectWhereChainedInstanceConditions() {
        var q = select("*").from("users")
                .where(eq("first_name", "jon")
                        .and().like("last_name", "d%")
                        .and().in("age", 20, 30)
                        .and().between("id", 1, 100))
                .build();
        assertEquals("SELECT * FROM users WHERE first_name = ? AND last_name LIKE ? AND age IN (?, ?) AND id BETWEEN ? AND ?",
                q.query());
        assertEquals(List.of("jon", "d%", 20, 30, 1, 100), q.values());
    }

    @Test
    void selectWhereComplexNested() {
        var q = select("*").from("users")
                .where(or(
                        and(eq("first_name", "jon"), eq("last_name", "doe")),
                        and(eq("first_name", "jane"), eq("last_name", "doe"))))
                .build();
        assertEquals(
                "SELECT * FROM users WHERE ( ( first_name = ? ) AND ( last_name = ? ) ) OR ( ( first_name = ? ) AND ( last_name = ? ) )",
                q.query());
        assertEquals(List.of("jon", "doe", "jane", "doe"), q.values());
    }

    @Test
    void selectOrderBy() {
        var q = select("*").from("users").orderBy("last_name").build();
        assertEquals("SELECT * FROM users ORDER BY last_name", q.query());
    }

    @Test
    void selectOrderByDesc() {
        var q = select("*").from("users").orderBy("last_name").desc().build();
        assertEquals("SELECT * FROM users ORDER BY last_name DESC", q.query());
    }

    @Test
    void selectOrderByAsc() {
        var q = select("*").from("users").orderBy("last_name").asc().build();
        assertEquals("SELECT * FROM users ORDER BY last_name ASC", q.query());
    }

    @Test
    void selectLimit() {
        var q = select("*").from("users").limit(10).build();
        assertEquals("SELECT * FROM users FETCH NEXT ? ROWS ONLY", q.query());
        assertEquals(List.of(10), q.values());
    }

    @Test
    void selectOffset() {
        var q = select("*").from("users").orderBy("name").desc().offset(5).limit(10).build();
        assertEquals("SELECT * FROM users ORDER BY name DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", q.query());
        assertEquals(List.of(5, 10), q.values());
    }

    @Test
    void selectOrderByLimit() {
        var q = select("*").from("users").orderBy("name").limit(5).build();
        assertEquals("SELECT * FROM users ORDER BY name FETCH NEXT ? ROWS ONLY", q.query());
        assertEquals(List.of(5), q.values());
    }

    @Test
    void selectJoin() {
        var q = select("*").from("users")
                .join("orders").on(colEq("users.id", "orders.user_id"))
                .build();
        assertEquals("SELECT * FROM users JOIN orders ON users.id = orders.user_id", q.query());
    }

    @Test
    void deleteQuery() {
        var q = QueryBuilder.delete().from("users").where(eq("id", 1)).build();
        assertEquals("DELETE FROM users WHERE id = ?", q.query());
        assertEquals(List.of(1), q.values());
    }

    @Test
    void insertQuery() {
        var q = QueryBuilder.insertInto("users", "first_name", "last_name")
                .values("jon", "doe")
                .build();
        assertEquals("INSERT INTO users (first_name, last_name) VALUES (?, ?)", q.query());
        assertEquals(List.of("jon", "doe"), q.values());
    }

    @Test
    void insertMultiRow() {
        var q = QueryBuilder.insertInto("users", "name")
                .values("jon")
                .values("jane")
                .build();
        assertEquals("INSERT INTO users (name) VALUES (?) , (?)", q.query());
        assertEquals(List.of("jon", "jane"), q.values());
    }

    @Test
    void insertSelect() {
        var q = QueryBuilder.insertInto("archive", "id", "name")
                .select("id", "name").from("users")
                .where(eq("status", "inactive"))
                .build();
        assertEquals("INSERT INTO archive (id, name) SELECT id, name FROM users WHERE status = ?",
                q.query());
        assertEquals(List.of("inactive"), q.values());
    }

    @Test
    void insertSelectDistinct() {
        var q = QueryBuilder.insertInto("active_names", "name")
                .selectDistinct("name").from("users")
                .where(eq("status", "active"))
                .build();
        assertEquals("INSERT INTO active_names (name) SELECT DISTINCT name FROM users WHERE status = ?",
                q.query());
        assertEquals(List.of("active"), q.values());
    }

    @Test
    void insertSelectWithJoin() {
        var q = QueryBuilder.insertInto("active_orders", "user_name", "product")
                .select("users.name", "orders.product")
                .from("users")
                .join("orders").on(colEq("users.id", "orders.customer_id"))
                .where(eq("users.status", "active"))
                .build();
        assertEquals(
                "INSERT INTO active_orders (user_name, product) SELECT users.name, orders.product FROM users JOIN orders ON users.id = orders.customer_id WHERE users.status = ?",
                q.query());
        assertEquals(List.of("active"), q.values());
    }

    @Test
    void insertSelectOrderBy() {
        var q = QueryBuilder.insertInto("sorted", "name")
                .select("name").from("users")
                .orderBy("name").desc()
                .build();
        assertEquals("INSERT INTO sorted (name) SELECT name FROM users ORDER BY name DESC",
                q.query());
    }

    @Test
    void updateQuery() {
        var q = QueryBuilder.update("users")
                .set("first_name", "jon")
                .where(eq("id", 1))
                .build();
        assertEquals("UPDATE users SET first_name = ? WHERE id = ?", q.query());
        assertEquals(List.of("jon", 1), q.values());
    }

    @Test
    void updateMultiSet() {
        var q = QueryBuilder.update("users")
                .set("first_name", "jon")
                .set("last_name", "doe")
                .where(eq("id", 1))
                .build();
        assertEquals("UPDATE users SET first_name = ? , last_name = ? WHERE id = ?", q.query());
        assertEquals(List.of("jon", "doe", 1), q.values());
    }

    @Test
    void selectJoinMultipleOnConditions() {
        var q = select("*").from("orders")
                .join("order_details")
                .on(colEq("orders.id", "order_details.order_id").and().colEq("orders.product", "order_details.product"))
                .build();
        assertEquals(
                "SELECT * FROM orders JOIN order_details ON orders.id = order_details.order_id AND orders.product = order_details.product",
                q.query());
    }

    @Test
    void selectJoinWithAdditionalFilter() {
        var q = select("*").from("users")
                .join("orders").on(colEq("users.id", "orders.customer_id").and().gt("orders.amount", 100))
                .build();
        assertEquals(
                "SELECT * FROM users JOIN orders ON users.id = orders.customer_id AND orders.amount > ?",
                q.query());
        assertEquals(List.of(100), q.values());
    }

    @Test
    void selectJoinOnWithOr() {
        var q = select("*").from("users")
                .join("orders")
                .on(colEq("users.id", "orders.customer_id").or().colEq("users.id", "orders.customer_id"))
                .build();
        assertEquals(
                "SELECT * FROM users JOIN orders ON users.id = orders.customer_id OR users.id = orders.customer_id",
                q.query());
    }

    @Test
    void selectJoinOnWithEqAndWhere() {
        var q = select("*").from("users")
                .join("orders").on(colEq("users.id", "orders.customer_id"))
                .where(and(eq("users.status", "active"), gt("orders.amount", 50)))
                .build();
        assertEquals(
                "SELECT * FROM users JOIN orders ON users.id = orders.customer_id WHERE ( users.status = ? ) AND ( orders.amount > ? )",
                q.query());
        assertEquals(List.of("active", 50), q.values());
    }

    @Test
    void selectJoinOnWithNot() {
        var q = select("*").from("users")
                .join("orders").on(not(colEq("users.id", "orders.customer_id")))
                .build();
        assertEquals("SELECT * FROM users JOIN orders ON NOT ( users.id = orders.customer_id )",
                q.query());
    }

    @Test
    void deleteWithJoin() {
        // DELETE does not support JOIN via this API — only DELETE FROM ... WHERE
        var q = QueryBuilder.delete().from("users")
                .where(in("id", select("customer_id").from("orders").where(eq("product", "Widget"))))
                .build();
        assertEquals("DELETE FROM users WHERE id IN (SELECT customer_id FROM orders WHERE product = ?)",
                q.query());
        assertEquals(List.of("Widget"), q.values());
    }

    @Test
    void insertWithSubqueryValues() {
        // INSERT INTO ... SELECT ... is not supported via values() — this is a TODO
        var q = QueryBuilder.insertInto("archive", "id", "name")
                .values(1, "archived")
                .build();
        assertEquals("INSERT INTO archive (id, name) VALUES (?, ?)", q.query());
        assertEquals(List.of(1, "archived"), q.values());
    }

    @Test
    void whereAfterJoin() {
        var q = select("*").from("users")
                .join("orders").on(colEq("users.id", "orders.user_id"))
                .where(eq("users.name", "jon"))
                .build();
        assertEquals("SELECT * FROM users JOIN orders ON users.id = orders.user_id WHERE users.name = ?",
                q.query());
        assertEquals(List.of("jon"), q.values());
    }

    @Test
    void selectInnerJoin() {
        var q = select("*").from("users")
                .innerJoin("orders").on(colEq("users.id", "orders.customer_id"))
                .build();
        assertEquals("SELECT * FROM users INNER JOIN orders ON users.id = orders.customer_id",
                q.query());
    }

    @Test
    void selectLeftJoin() {
        var q = select("*").from("users")
                .leftJoin("orders").on(colEq("users.id", "orders.customer_id"))
                .build();
        assertEquals("SELECT * FROM users LEFT JOIN orders ON users.id = orders.customer_id",
                q.query());
    }

    @Test
    void selectRightJoin() {
        var q = select("*").from("users")
                .rightJoin("orders").on(colEq("users.id", "orders.customer_id"))
                .build();
        assertEquals("SELECT * FROM users RIGHT JOIN orders ON users.id = orders.customer_id",
                q.query());
    }

    @Test
    void selectFullJoin() {
        var q = select("*").from("users")
                .fullJoin("orders").on(colEq("users.id", "orders.customer_id"))
                .build();
        assertEquals("SELECT * FROM users FULL JOIN orders ON users.id = orders.customer_id",
                q.query());
    }

    @Test
    void selectCrossJoin() {
        var q = select("*").from("users")
                .crossJoin("orders")
                .build();
        assertEquals("SELECT * FROM users CROSS JOIN orders",
                q.query());
    }

    @Test
    void selectCrossJoinWithOn() {
        var q = select("*").from("users")
                .crossJoin("orders").on(colEq("users.id", "orders.customer_id"))
                .build();
        assertEquals("SELECT * FROM users CROSS JOIN orders ON users.id = orders.customer_id",
                q.query());
    }

    @Test
    void selectNaturalJoin() {
        var q = select("*").from("users")
                .naturalJoin("orders")
                .build();
        assertEquals("SELECT * FROM users NATURAL JOIN orders",
                q.query());
    }

    @Test
    void selectMultipleJoins() {
        var q = select("*").from("users")
                .join("orders").on(colEq("users.id", "orders.customer_id"))
                .join("order_details").on(
                        and(colEq("orders.id", "order_details.order_id"),
                                colEq("orders.product", "order_details.product")))
                .build();
        assertEquals(
                "SELECT * FROM users JOIN orders ON users.id = orders.customer_id JOIN order_details ON ( orders.id = order_details.order_id ) AND ( orders.product = order_details.product )",
                q.query());
    }

    @Test
    void selectMixedJoinQualifiers() {
        var q = select("*").from("users")
                .leftJoin("orders").on(colEq("users.id", "orders.customer_id"))
                .innerJoin("order_details").on(
                        and(colEq("orders.id", "order_details.order_id"),
                                colEq("orders.product", "order_details.product")))
                .build();
        assertEquals(
                "SELECT * FROM users LEFT JOIN orders ON users.id = orders.customer_id INNER JOIN order_details ON ( orders.id = order_details.order_id ) AND ( orders.product = order_details.product )",
                q.query());
    }

    @Test
    void selectMultipleJoinsWithWhere() {
        var q = select("*").from("users")
                .leftJoin("orders").on(colEq("users.id", "orders.customer_id"))
                .join("order_details").on(
                        and(colEq("orders.id", "order_details.order_id"),
                                colEq("orders.product", "order_details.product")))
                .where(eq("users.status", "active"))
                .build();
        assertEquals(
                "SELECT * FROM users LEFT JOIN orders ON users.id = orders.customer_id JOIN order_details ON ( orders.id = order_details.order_id ) AND ( orders.product = order_details.product ) WHERE users.status = ?",
                q.query());
        assertEquals(List.of("active"), q.values());
    }

    // ---- Subquery in FROM / JOIN ----

    @Test
    void fromSubquery() {
        var sub = select("id", "name").from("users").where(gt("age", 21));
        var q = select("*").from(sub).build();
        assertEquals(
                "SELECT * FROM (SELECT id, name FROM users WHERE age > ?)",
                q.query());
        assertEquals(List.of(21), q.values());
    }

    @Test
    void fromSubqueryWithAs() {
        var sub = select("id", "name").from("users");
        var q = select("*").from(sub).as("u").build();
        assertEquals(
                "SELECT * FROM (SELECT id, name FROM users) AS u",
                q.query());
    }

    @Test
    void fromSubqueryWithWhere() {
        var sub = select("id", "name").from("users").where(gt("age", 18));
        var q = select("*").from(sub).as("adults")
                .where(like("name", "A%"))
                .build();
        assertEquals(
                "SELECT * FROM (SELECT id, name FROM users WHERE age > ?) AS adults WHERE name LIKE ?",
                q.query());
        assertEquals(List.of(18, "A%"), q.values());
    }

    @Test
    void fromSubqueryWithJoin() {
        var active = select("id", "name").from("users").where(eq("status", "active"));
        var q = select("*").from(active).as("u")
                .join("orders").on(colEq("u.id", "orders.customer_id"))
                .build();
        assertEquals(
                "SELECT * FROM (SELECT id, name FROM users WHERE status = ?) AS u JOIN orders ON u.id = orders.customer_id",
                q.query());
        assertEquals(List.of("active"), q.values());
    }

    @Test
    void joinSubquery() {
        var topProducts = select("customer_id", "product", "amount")
                .from("orders").where(gt("amount", 100));
        var q = select("*").from("users")
                .join(topProducts).as("big")
                .on(colEq("users.id", "big.customer_id"))
                .build();
        assertEquals(
                "SELECT * FROM users JOIN (SELECT customer_id, product, amount FROM orders WHERE amount > ?) AS big ON users.id = big.customer_id",
                q.query());
        assertEquals(List.of(100), q.values());
    }

    @Test
    void leftJoinSubquery() {
        var topCustomers = select("customer_id", "COUNT(*) AS cnt")
                .from("orders").where(gt("amount", 10));
        var q = select("*").from("users")
                .leftJoin(topCustomers).as("os")
                .on(colEq("users.id", "os.customer_id"))
                .build();
        assertEquals(
                "SELECT * FROM users LEFT JOIN (SELECT customer_id, COUNT(*) AS cnt FROM orders WHERE amount > ?) AS os ON users.id = os.customer_id",
                q.query());
        assertEquals(List.of(10), q.values());
    }

    @Test
    void joinSubqueryWithWhere() {
        var expensive = select("customer_id", "product")
                .from("orders").where(gt("amount", 50));
        var q = select("*").from("users")
                .join(expensive).as("e")
                .on(colEq("users.id", "e.customer_id"))
                .where(eq("users.status", "active"))
                .build();
        assertEquals(
                "SELECT * FROM users JOIN (SELECT customer_id, product FROM orders WHERE amount > ?) AS e ON users.id = e.customer_id WHERE users.status = ?",
                q.query());
        assertEquals(List.of(50, "active"), q.values());
    }

    @Test
    void insertSelectFromSubquery() {
        var source = select("name", "age").from("users").where(eq("status", "active"));
        var q = QueryBuilder.insertInto("active_users", "name", "age")
                .select("name", "age").from(source)
                .build();
        assertEquals(
                "INSERT INTO active_users (name, age) SELECT name, age FROM (SELECT name, age FROM users WHERE status = ?)",
                q.query());
        assertEquals(List.of("active"), q.values());
    }

    // ---------------------------------------------------------------------------
    // Complicated / nested SQL
    // ---------------------------------------------------------------------------

    @Test
    void nestedSubqueryInWhere() {
        var q = select("*").from("customers")
                .where(in("id",
                        select("CustomerID").from("orders")
                                .where(in("ProductID",
                                        select("ID").from("products")
                                                .where(gt("price", 100))))))
                .build();
        assertEquals("SELECT * FROM customers WHERE id IN (SELECT CustomerID FROM orders WHERE ProductID IN (SELECT ID FROM products WHERE price > ?))",
                q.query());
        assertEquals(List.of(100), q.values());
    }

    @Test
    void complexNestedAndOrNot() {
        var q = select("*").from("users")
                .where(not(
                        and(
                                or(eq("status", "inactive"), eq("role", "guest")),
                                and(eq("deleted", 1), eq("flag", 0)))))
                .build();
        assertEquals(
                "SELECT * FROM users WHERE NOT ( ( ( status = ? ) OR ( role = ? ) ) AND ( ( deleted = ? ) AND ( flag = ? ) ) )",
                q.query());
        assertEquals(List.of("inactive", "guest", 1, 0), q.values());
    }

    @Test
    void multipleOrderByColumns() {
        var q = select("*").from("users").orderBy("last_name", "first_name").desc().build();
        assertEquals("SELECT * FROM users ORDER BY last_name, first_name DESC", q.query());
    }

    @Test
    void selectWithWhereNotChained() {
        var q = select("*").from("users")
                .where(not(eq("status", "deleted")).and().eq("active", 1))
                .build();
        assertEquals("SELECT * FROM users WHERE NOT ( status = ? ) AND active = ?", q.query());
        assertEquals(List.of("deleted", 1), q.values());
    }

    @Test
    void whereWithChainedCombinerAndInline() {
        var q = select("*").from("users")
                .where(eq("a", 1).and().and(eq("b", 2), eq("c", 3)).and().eq("d", 4))
                .build();
        assertEquals("SELECT * FROM users WHERE a = ? AND (( b = ? ) AND ( c = ? )) AND d = ?", q.query());
        assertEquals(List.of(1, 2, 3, 4), q.values());
    }

    @Test
    void updateWithMultipleSetsAndWhereNot() {
        var q = QueryBuilder.update("users")
                .set("name", "new_name")
                .set("email", "new_email")
                .where(not(eq("id", 1)))
                .build();
        assertEquals("UPDATE users SET name = ? , email = ? WHERE NOT ( id = ? )", q.query());
        assertEquals(List.of("new_name", "new_email", 1), q.values());
    }

    @Test
    void deleteWithComplexWhere() {
        var q = QueryBuilder.delete().from("logs")
                .where(and(
                        between("created_at", "2024-01-01", "2024-12-31"),
                        or(eq("level", "DEBUG"), eq("level", "TRACE"))))
                .build();
        assertEquals(
                "DELETE FROM logs WHERE ( created_at BETWEEN ? AND ? ) AND ( ( level = ? ) OR ( level = ? ) )",
                q.query());
        assertEquals(List.of("2024-01-01", "2024-12-31", "DEBUG", "TRACE"), q.values());
    }

    @Test
    void selectAggregateColumnsAsStrings() {
        var q = select("COUNT(*)", "AVG(salary)", "MAX(bonus)")
                .from("employees")
                .where(gt("salary", 0))
                .build();
        assertEquals("SELECT COUNT(*), AVG(salary), MAX(bonus) FROM employees WHERE salary > ?", q.query());
        assertEquals(List.of(0), q.values());
    }

    @Test
    void chainedWhereAllConditionTypes() {
        var q = select("*").from("items")
                .where(eq("type", "book")
                        .and().not_eq("status", "archived")
                        .and().like("title", "%java%")
                        .and().not_like("author", "%foo%")
                        .and().in("category", 1, 2, 3)
                        .and().not_in("tags", "old", "deprecated")
                        .and().between("price", 10, 100)
                        .and().not_between("rating", 0, 1)
                        .and().lt("qty", 10)
                        .and().not_lt("min", 5)
                        .and().gt("score", 50)
                        .and().not_gt("max", 200))
                .build();
        assertEquals("SELECT * FROM items WHERE type = ? AND NOT status = ? AND title LIKE ? AND author NOT LIKE ? AND category IN (?, ?, ?) AND tags NOT IN (?, ?) AND price BETWEEN ? AND ? AND rating NOT BETWEEN ? AND ? AND qty < ? AND NOT min < ? AND score > ? AND NOT max > ?",
                q.query());
        assertEquals(List.of("book", "archived", "%java%", "%foo%", 1, 2, 3, "old", "deprecated", 10, 100, 0, 1, 10, 5, 50, 200), q.values());
    }

    // ---------------------------------------------------------------------------
    // lt/gt/not_lt/not_gt in WHERE
    // ---------------------------------------------------------------------------

    @Test
    void selectWhereLt() {
        var q = select("*").from("users").where(lt("id", 5)).build();
        assertEquals("SELECT * FROM users WHERE id < ?", q.query());
        assertEquals(List.of(5), q.values());
    }

    @Test
    void selectWhereGt() {
        var q = select("*").from("users").where(gt("id", 5)).build();
        assertEquals("SELECT * FROM users WHERE id > ?", q.query());
        assertEquals(List.of(5), q.values());
    }

    @Test
    void selectWhereNotLt() {
        var q = select("*").from("users").where(not_lt("id", 5)).build();
        assertEquals("SELECT * FROM users WHERE NOT id < ?", q.query());
        assertEquals(List.of(5), q.values());
    }

    @Test
    void selectWhereNotGt() {
        var q = select("*").from("users").where(not_gt("id", 5)).build();
        assertEquals("SELECT * FROM users WHERE NOT id > ?", q.query());
        assertEquals(List.of(5), q.values());
    }

    @Test
    void selectWhereAndLtGt() {
        var q = select("*").from("users")
                .where(and(lt("a", 1), gt("b", 2)))
                .build();
        assertEquals("SELECT * FROM users WHERE ( a < ? ) AND ( b > ? )", q.query());
        assertEquals(List.of(1, 2), q.values());
    }

    @Test
    void selectWhereChainedLtGt() {
        var q = select("*").from("users")
                .where(lt("a", 1).and().gt("b", 2))
                .build();
        assertEquals("SELECT * FROM users WHERE a < ? AND b > ?", q.query());
        assertEquals(List.of(1, 2), q.values());
    }

    @Test
    void selectWhereChainedLtGtWithCombiner() {
        var q = select("*").from("users")
                .where(lt("a", 1).and().and(lt("b", 2), gt("c", 3)))
                .build();
        assertEquals("SELECT * FROM users WHERE a < ? AND (( b < ? ) AND ( c > ? ))", q.query());
        assertEquals(List.of(1, 2, 3), q.values());
    }

    @Test
    void deleteWhereLtGt() {
        var q = QueryBuilder.delete().from("logs")
                .where(or(lt("id", 100), gt("id", 200)))
                .build();
        assertEquals("DELETE FROM logs WHERE ( id < ? ) OR ( id > ? )", q.query());
        assertEquals(List.of(100, 200), q.values());
    }

    @Test
    void updateWhereLt() {
        var q = QueryBuilder.update("products")
                .set("price", 0)
                .where(lt("stock", 1))
                .build();
        assertEquals("UPDATE products SET price = ? WHERE stock < ?", q.query());
        assertEquals(List.of(0, 1), q.values());
    }

    // ---- IS NULL / IS NOT NULL / EXISTS / NOT EXISTS ----

    @Test
    void selectWhereIsNull() {
        var q = select("*").from("users").where(isNull("email")).build();
        assertEquals("SELECT * FROM users WHERE email IS NULL", q.query());
        assertTrue(q.values().isEmpty());
    }

    @Test
    void selectWhereIsNotNull() {
        var q = select("*").from("users").where(isNotNull("email")).build();
        assertEquals("SELECT * FROM users WHERE email IS NOT NULL", q.query());
        assertTrue(q.values().isEmpty());
    }

    @Test
    void selectWhereExists() {
        var sub = select("*").from("orders").where(colEq("orders.customer_id", "users.id"));
        var q = select("*").from("users").where(exists(sub)).build();
        assertEquals(
                "SELECT * FROM users WHERE EXISTS (SELECT * FROM orders WHERE orders.customer_id = users.id)",
                q.query());
        assertTrue(q.values().isEmpty());
    }

    @Test
    void selectWhereNotExists() {
        var sub = select("*").from("orders").where(colEq("orders.customer_id", "users.id"));
        var q = select("*").from("users").where(notExists(sub)).build();
        assertEquals(
                "SELECT * FROM users WHERE NOT EXISTS (SELECT * FROM orders WHERE orders.customer_id = users.id)",
                q.query());
        assertTrue(q.values().isEmpty());
    }

    @Test
    void selectWhereExistsWithParams() {
        var sub = select("*").from("orders")
                .where(and(colEq("orders.customer_id", "users.id"), gt("orders.amount", 100)));
        var q = select("*").from("users").where(exists(sub)).build();
        assertEquals(
                "SELECT * FROM users WHERE EXISTS (SELECT * FROM orders WHERE ( orders.customer_id = users.id ) AND ( orders.amount > ? ))",
                q.query());
        assertEquals(List.of(100), q.values());
    }

    @Test
    void selectWhereAndIsNull() {
        var q = select("*").from("users")
                .where(and(isNull("email"), eq("status", "inactive")))
                .build();
        assertEquals("SELECT * FROM users WHERE ( email IS NULL ) AND ( status = ? )", q.query());
        assertEquals(List.of("inactive"), q.values());
    }

    @Test
    void selectWhereNotIsNull() {
        var q = select("*").from("users")
                .where(not(isNull("email")))
                .build();
        assertEquals("SELECT * FROM users WHERE NOT ( email IS NULL )", q.query());
    }

    // ---------------------------------------------------------------------------
    // Edge cases
    // ---------------------------------------------------------------------------

    @Test
    void selectWithNullValues() {
        var q = select("*").from("users").where(eq("name", null)).build();
        assertEquals("SELECT * FROM users WHERE name = ?", q.query());
        assertEquals(java.util.Collections.singletonList(null), q.values());
    }

    @Test
    void selectWithEmptyInClause() {
        var q = select("*").from("users").where(in("id")).build();
        assertEquals("SELECT * FROM users WHERE id IN ()", q.query());
        assertTrue(q.values().isEmpty());
    }

    @Test
    void selectWithZeroLimit() {
        var q = select("*").from("users").limit(0).build();
        assertEquals("SELECT * FROM users FETCH NEXT ? ROWS ONLY", q.query());
        assertEquals(List.of(0), q.values());
    }

    @Test
    void selectWithNegativeOffset() {
        var q = select("*").from("users").orderBy("id").desc().offset(-5).limit(10).build();
        assertEquals("SELECT * FROM users ORDER BY id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", q.query());
        assertEquals(List.of(-5, 10), q.values());
    }

    // ---- GROUP BY ----

    @Test
    void selectGroupBy() {
        var q = select("status", "COUNT(*)").from("users")
                .groupBy("status")
                .build();
        assertEquals("SELECT status, COUNT(*) FROM users GROUP BY status", q.query());
    }

    @Test
    void selectGroupByMultipleColumns() {
        var q = select("status", "age", "COUNT(*)").from("users")
                .groupBy("status", "age")
                .build();
        assertEquals("SELECT status, age, COUNT(*) FROM users GROUP BY status, age", q.query());
    }

    @Test
    void selectWhereGroupBy() {
        var q = select("status", "COUNT(*)").from("users")
                .where(gt("age", 20))
                .groupBy("status")
                .build();
        assertEquals("SELECT status, COUNT(*) FROM users WHERE age > ? GROUP BY status",
                q.query());
        assertEquals(List.of(20), q.values());
    }

    @Test
    void selectGroupByOrderBy() {
        var q = select("status", "COUNT(*)").from("users")
                .groupBy("status")
                .orderBy("status")
                .build();
        assertEquals("SELECT status, COUNT(*) FROM users GROUP BY status ORDER BY status",
                q.query());
    }

    @Test
    void selectGroupByLimit() {
        var q = select("status", "COUNT(*)").from("users")
                .groupBy("status")
                .limit(5)
                .build();
        assertEquals("SELECT status, COUNT(*) FROM users GROUP BY status FETCH NEXT ? ROWS ONLY",
                q.query());
        assertEquals(List.of(5), q.values());
    }

    @Test
    void selectWhereGroupByOrderByLimit() {
        var q = select("status", "COUNT(*)").from("users")
                .where(gt("age", 18))
                .groupBy("status")
                .orderBy("status").desc()
                .limit(10)
                .build();
        assertEquals(
                "SELECT status, COUNT(*) FROM users WHERE age > ? GROUP BY status ORDER BY status DESC FETCH NEXT ? ROWS ONLY",
                q.query());
        assertEquals(List.of(18, 10), q.values());
    }

    @Test
    void selectGroupByWithJoin() {
        var q = select("users.status", "COUNT(orders.id)")
                .from("users")
                .leftJoin("orders").on(colEq("users.id", "orders.customer_id"))
                .groupBy("users.status")
                .build();
        assertEquals(
                "SELECT users.status, COUNT(orders.id) FROM users LEFT JOIN orders ON users.id = orders.customer_id GROUP BY users.status",
                q.query());
    }

    @Test
    void insertSelectWithGroupBy() {
        var q = QueryBuilder.insertInto("status_counts", "status", "cnt")
                .select("status", "COUNT(*)").from("users")
                .groupBy("status")
                .build();
        assertEquals(
                "INSERT INTO status_counts (status, cnt) SELECT status, COUNT(*) FROM users GROUP BY status",
                q.query());
    }

    // ---- HAVING ----

    @Test
    void selectHaving() {
        var q = select("department", "COUNT(*)").from("employees")
                .groupBy("department")
                .having(gt("COUNT(*)", 5))
                .build();
        assertEquals("SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > ?",
                q.query());
        assertEquals(List.of(5), q.values());
    }

    @Test
    void selectWhereGroupByHaving() {
        var q = select("department", "COUNT(*)").from("employees")
                .where(gt("salary", 50000))
                .groupBy("department")
                .having(gt("COUNT(*)", 3))
                .build();
        assertEquals("SELECT department, COUNT(*) FROM employees WHERE salary > ? GROUP BY department HAVING COUNT(*) > ?",
                q.query());
        assertEquals(List.of(50000, 3), q.values());
    }

    @Test
    void selectGroupByHavingOrderBy() {
        var q = select("department", "COUNT(*)").from("employees")
                .groupBy("department")
                .having(gt("COUNT(*)", 5))
                .orderBy("department").desc()
                .build();
        assertEquals("SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > ? ORDER BY department DESC",
                q.query());
        assertEquals(List.of(5), q.values());
    }

    @Test
    void selectGroupByHavingLimit() {
        var q = select("department", "COUNT(*)").from("employees")
                .groupBy("department")
                .having(gt("COUNT(*)", 5))
                .limit(10)
                .build();
        assertEquals("SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > ? FETCH NEXT ? ROWS ONLY",
                q.query());
        assertEquals(List.of(5, 10), q.values());
    }

    // ---- Self-join with table alias ----

    @Test
    void selectFromWithAlias() {
        var q = select("e.name", "m.name").from("employees").as("e")
                .join("employees").as("m").on(colEq("e.manager_id", "m.id"))
                .build();
        assertEquals("SELECT e.name, m.name FROM employees AS e JOIN employees AS m ON e.manager_id = m.id",
                q.query());
    }

    @Test
    void selectJoinWithAlias() {
        var q = select("*").from("orders")
                .leftJoin("customers").as("c").on(colEq("orders.customer_id", "c.id"))
                .build();
        assertEquals("SELECT * FROM orders LEFT JOIN customers AS c ON orders.customer_id = c.id",
                q.query());
    }

    @Test
    void selectFromSubqueryWithAlias() {
        var q = select("sq.*").from(select("*").from("users")).as("sq")
                .build();
        assertEquals("SELECT sq.* FROM (SELECT * FROM users) AS sq", q.query());
    }

    @Test
    void selectJoinSubqueryWithAlias() {
        var q = select("*").from("orders")
                .join(select("id").from("customers").where(gt("id", 0))).as("active")
                .on(colEq("orders.customer_id", "active.id"))
                .build();
        assertEquals("SELECT * FROM orders JOIN (SELECT id FROM customers WHERE id > ?) AS active ON orders.customer_id = active.id",
                q.query());
        assertEquals(List.of(0), q.values());
    }

    // ---- USING ----

    @Test
    void selectJoinUsing() {
        var q = select("*").from("a")
                .join("b").using("id")
                .build();
        assertEquals("SELECT * FROM a JOIN b USING (id)", q.query());
    }

    @Test
    void selectJoinUsingMultipleColumns() {
        var q = select("*").from("a")
                .leftJoin("b").using("x", "y")
                .build();
        assertEquals("SELECT * FROM a LEFT JOIN b USING (x, y)", q.query());
    }

    // ---- UNION / INTERSECT / EXCEPT ----

    @Test
    void selectUnion() {
        var q = select("*").from("a")
                .union(select("*").from("b"))
                .build();
        assertEquals("SELECT * FROM a UNION SELECT * FROM b", q.query());
    }

    @Test
    void selectUnionWithValues() {
        var q = select("name").from("users")
                .where(eq("active", true))
                .union(select("name").from("archived").where(eq("active", true)))
                .build();
        assertEquals("SELECT name FROM users WHERE active = ? UNION SELECT name FROM archived WHERE active = ?",
                q.query());
        assertEquals(List.of(true, true), q.values());
    }

    @Test
    void selectIntersect() {
        var q = select("*").from("a")
                .intersect(select("*").from("b"))
                .build();
        assertEquals("SELECT * FROM a INTERSECT SELECT * FROM b", q.query());
    }

    @Test
    void selectExcept() {
        var q = select("*").from("a")
                .except(select("*").from("b"))
                .build();
        assertEquals("SELECT * FROM a EXCEPT SELECT * FROM b", q.query());
    }

    @Test
    void selectUnionOrderBy() {
        var q = select("*").from("a")
                .union(select("*").from("b"))
                .orderBy("id")
                .build();
        assertEquals("SELECT * FROM a UNION SELECT * FROM b ORDER BY id", q.query());
    }

    @Test
    void selectChainedUnion() {
        var q = select("*").from("a")
                .union(select("*").from("b"))
                .union(select("*").from("c"))
                .build();
        assertEquals("SELECT * FROM a UNION SELECT * FROM b UNION SELECT * FROM c", q.query());
    }
}
