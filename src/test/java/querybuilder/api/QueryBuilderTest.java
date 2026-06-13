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
                .join("orders").on("users.id").eq("orders.user_id")
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
    void whereAfterJoin() {
        var q = select("*").from("users")
                .join("orders").on("users.id").eq("orders.user_id")
                .where(eq("users.name", "jon"))
                .build();
        assertEquals("SELECT * FROM users JOIN orders ON users.id = orders.user_id WHERE users.name = ?",
                q.query());
        assertEquals(List.of("jon"), q.values());
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

    // ---------------------------------------------------------------------------
    // TODO: Missing features (not yet supported — tracked as disabled tests)
    // ---------------------------------------------------------------------------

    // TODO: GROUP BY clause
    // Example: select("*").from("users").groupBy("department") → "SELECT * FROM users GROUP BY department"
    // Missing: GroupBy class, groupBy() method on Filterable / Orderable

    // TODO: HAVING clause
    // Example: select("department", "COUNT(*)").from("employees").groupBy("department").having(gt("COUNT(*)", 5))
    // Missing: Having class, having() method

    // TODO: SELECT DISTINCT
    // Example: selectDistinct("city").from("users") → "SELECT DISTINCT city FROM users"
    // Missing: selectDistinct() entry point, or distinct modifier

    // TODO: Multiple JOINs
    // Example: from("a").join("b").on("a.id").eq("b.a_id").join("c").on("b.id").eq("c.b_id")
    // Missing: join() method on Eq/Filterable (currently only on From)

    // TODO: JOIN type qualifiers (INNER, LEFT, RIGHT, FULL, CROSS)
    // Example: leftJoin("b").on("a.id").eq("b.a_id") → "LEFT JOIN b ON a.id = b.a_id"

    // TODO: EXISTS / NOT EXISTS conditions
    // Example: exists(select("*").from("orders").where(eq("customer_id", 1))) → "EXISTS (SELECT * FROM orders WHERE customer_id = ?)"
    // Missing: FilterBuilder.exists(), FilterBuilder.not_exists()

    // TODO: IS NULL / IS NOT NULL conditions
    // Example: isNull("deleted_at") → "deleted_at IS NULL"
    // Missing: FilterBuilder.isNull(), FilterBuilder.isNotNull()

    // TODO: Subquery IN FROM clause
    // Example: from(select("*").from("sub")).alias("t") — needs From to accept TerminalClause, not just String

    // TODO: UNION / INTERSECT / EXCEPT
    // Example: select("*").from("a").union(select("*").from("b"))
    // Missing: union(), intersect(), except() builders



    @Test
    @org.junit.jupiter.api.Disabled("GROUP BY not yet implemented")
    void selectGroupBy() {
        // select("*").from("users").groupBy("department").build();
    }

    @Test
    @org.junit.jupiter.api.Disabled("HAVING not yet implemented")
    void selectHaving() {
        // select("department", "COUNT(*)").from("employees").groupBy("department").having(gt("COUNT(*)", 5)).build();
    }
}
