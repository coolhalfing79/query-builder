package querybuilder.api;

import static querybuilder.api.FilterBuilder.*;
import static querybuilder.api.QueryBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryBuilderIntegrationTest {

    private Connection conn;

    @BeforeEach
    void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:");
        try (var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE users (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT, " +
                    "age INTEGER NOT NULL, " +
                    "status TEXT NOT NULL)");
            stmt.execute("CREATE TABLE orders (" +
                    "id INTEGER PRIMARY KEY, " +
                    "customer_id INTEGER NOT NULL, " +
                    "product TEXT NOT NULL, " +
                    "amount REAL NOT NULL)");
            stmt.execute("CREATE TABLE order_details (" +
                    "order_id INTEGER NOT NULL, " +
                    "product TEXT NOT NULL, " +
                    "qty INTEGER NOT NULL)");
        }
        try (var stmt = conn.prepareStatement(
                "INSERT INTO users (id, name, email, age, status) VALUES (?, ?, ?, ?, ?)")) {
            Object[][] users = {
                {1, "Alice", "alice@example.com", 30, "active"},
                {2, "Bob", "bob@example.com", 25, "active"},
                {3, "Charlie", "charlie@example.com", 35, "inactive"},
                {4, "Diana", "diana@example.com", 28, "active"},
                {5, "Eve", "eve@example.com", 22, "inactive"}};
            for (var u : users) {
                stmt.setInt(1, (int) u[0]);
                stmt.setString(2, (String) u[1]);
                stmt.setString(3, (String) u[2]);
                stmt.setInt(4, (int) u[3]);
                stmt.setString(5, (String) u[4]);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
        try (var stmt = conn.prepareStatement(
                "INSERT INTO orders (id, customer_id, product, amount) VALUES (?, ?, ?, ?)")) {
            Object[][] orders = {
                {1, 1, "Widget", 10.99},
                {2, 1, "Gadget", 24.99},
                {3, 2, "Widget", 10.99},
                {4, 3, "Doohickey", 5.99}};
            for (var o : orders) {
                stmt.setInt(1, (int) o[0]);
                stmt.setInt(2, (int) o[1]);
                stmt.setString(3, (String) o[2]);
                stmt.setDouble(4, (double) o[3]);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
        try (var stmt = conn.prepareStatement(
                "INSERT INTO order_details (order_id, product, qty) VALUES (?, ?, ?)")) {
            Object[][] details = {
                {1, "Widget", 2},
                {2, "Gadget", 1},
                {3, "Widget", 3},
                {4, "Doohickey", 5}};
            for (var d : details) {
                stmt.setInt(1, (int) d[0]);
                stmt.setString(2, (String) d[1]);
                stmt.setInt(3, (int) d[2]);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (conn != null) conn.close();
    }

    private ResultSet execute(QueryBuilder.Query q) throws Exception {
        var pstmt = conn.prepareStatement(q.query());
        for (int i = 0; i < q.values().size(); i++) {
            pstmt.setObject(i + 1, q.values().get(i));
        }
        return pstmt.executeQuery();
    }

    private int executeUpdate(QueryBuilder.Query q) throws Exception {
        var pstmt = conn.prepareStatement(q.query());
        for (int i = 0; i < q.values().size(); i++) {
            pstmt.setObject(i + 1, q.values().get(i));
        }
        return pstmt.executeUpdate();
    }

    // ---- SELECT ----

    @Test
    void selectAll() throws Exception {
        var q = select("*").from("users").build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(5, count);
    }

    @Test
    void selectDistinctStatus() throws Exception {
        var q = selectDistinct("status").from("users").build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(2, count);
    }

    @Test
    void selectFromSubquery() throws Exception {
        var sub = select("*").from("users").where(eq("status", "active"));
        var q = select("*").from(sub).build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(3, count);
    }

    @Test
    void selectFromSubqueryWithAlias() throws Exception {
        var sub = select("id", "name").from("users").where(eq("status", "active"));
        var q = select("*").from(sub).as("u")
                .where(like("u.name", "A%"))
                .build();
        var rs = execute(q);
        assertTrue(rs.next());
        assertEquals(1, rs.getInt("id"));
        assertEquals("Alice", rs.getString("name"));
        assertFalse(rs.next());
    }

    @Test
    void selectJoinSubquery() throws Exception {
        var sub = select("customer_id", "product")
                .from("orders").where(eq("product", "Widget"));
        var q = select("*").from("users")
                .join(sub).as("high")
                .on(colEq("users.id", "high.customer_id"))
                .where(eq("users.name", "Alice"))
                .build();
        var rs = execute(q);
        assertTrue(rs.next());
        assertEquals("Widget", rs.getString("product"));
        assertFalse(rs.next());
    }

    @Test
    void selectWhereEq() throws Exception {
        var q = select("*").from("users").where(eq("name", "Alice")).build();
        var rs = execute(q);
        assertTrue(rs.next());
        assertEquals(1, rs.getInt("id"));
        assertFalse(rs.next());
    }

    @Test
    void selectWhereNotEq() throws Exception {
        var q = select("*").from("users").where(not_eq("name", "Alice")).build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(4, count);
    }

    @Test
    void selectWhereAnd() throws Exception {
        var q = select("*").from("users")
                .where(and(eq("status", "active"), eq("age", 30)))
                .build();
        var rs = execute(q);
        assertTrue(rs.next());
        assertEquals("Alice", rs.getString("name"));
        assertFalse(rs.next());
    }

    @Test
    void selectWhereOr() throws Exception {
        var q = select("*").from("users")
                .where(or(eq("name", "Alice"), eq("name", "Bob")))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(2, count);
    }

    @Test
    void selectWhereNot() throws Exception {
        var q = select("*").from("users")
                .where(not(eq("status", "inactive")))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(3, count);
    }

    @Test
    void selectWhereIn() throws Exception {
        var q = select("*").from("users")
                .where(in("id", 1, 3, 5))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(3, count);
    }

    @Test
    void selectWhereBetween() throws Exception {
        var q = select("*").from("users")
                .where(between("age", 25, 30))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(3, count);
    }

    @Test
    void selectWhereLike() throws Exception {
        var q = select("*").from("users")
                .where(like("name", "A%"))
                .build();
        var rs = execute(q);
        assertTrue(rs.next());
        assertEquals("Alice", rs.getString("name"));
        assertFalse(rs.next());
    }

    @Test
    void selectWhereLt() throws Exception {
        var q = select("*").from("users").where(lt("age", 30)).build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(3, count);
    }

    @Test
    void selectWhereGt() throws Exception {
        var q = select("*").from("users").where(gt("age", 30)).build();
        var rs = execute(q);
        assertTrue(rs.next());
        assertEquals("Charlie", rs.getString("name"));
        assertFalse(rs.next());
    }

    @Test
    void selectWhereChained() throws Exception {
        var q = select("*").from("users")
                .where(eq("status", "active").and().eq("age", 30))
                .build();
        var rs = execute(q);
        assertTrue(rs.next());
        assertEquals("Alice", rs.getString("name"));
        assertFalse(rs.next());
    }

    @Test
    void selectWhereComplexNested() throws Exception {
        var q = select("*").from("users")
                .where(or(
                        and(eq("status", "active"), gt("age", 30)),
                        and(eq("status", "inactive"), lt("age", 30))))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(1, count);
    }

    @Test
    void selectWhereIsNull() throws Exception {
        // All users have non-null emails, so should return 0
        var q = select("*").from("users")
                .where(isNull("email"))
                .build();
        var rs = execute(q);
        assertFalse(rs.next());
    }

    @Test
    void selectWhereIsNotNull() throws Exception {
        var q = select("*").from("users")
                .where(isNotNull("email"))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(5, count);
    }

    @Test
    void selectWhereExists() throws Exception {
        var sub = select("*").from("orders")
                .where(colEq("orders.customer_id", "users.id"));
        var q = select("*").from("users")
                .where(exists(sub))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        // Users who have at least one order: Alice, Bob, Charlie
        assertEquals(3, count);
    }

    @Test
    void selectWhereNotExists() throws Exception {
        var sub = select("*").from("orders")
                .where(colEq("orders.customer_id", "users.id"));
        var q = select("*").from("users")
                .where(notExists(sub))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        // Users with no orders: Diana, Eve
        assertEquals(2, count);
    }

    @Test
    void selectWhereExistsWithParam() throws Exception {
        var sub = select("*").from("orders")
                .where(and(colEq("orders.customer_id", "users.id"),
                        gt("orders.amount", 15)));
        var q = select("*").from("users")
                .where(exists(sub))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        // Users with amount>15 orders: Alice has Gadget(24.99), others ≤10.99
        assertEquals(1, count);
    }

    @Test
    void selectOrderBy() throws Exception {
        var q = select("*").from("users").orderBy("name").build();
        var rs = execute(q);
        assertTrue(rs.next());
        assertEquals("Alice", rs.getString("name"));
        assertTrue(rs.next());
        assertEquals("Bob", rs.getString("name"));
    }

    @Test
    void selectOrderByDesc() throws Exception {
        var q = select("*").from("users").orderBy("name").desc().build();
        var rs = execute(q);
        assertTrue(rs.next());
        assertEquals("Eve", rs.getString("name"));
    }

    // ---- GROUP BY ----

    @Test
    void selectGroupBy() throws Exception {
        var q = select("status", "COUNT(*) AS cnt").from("users")
                .groupBy("status")
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(2, count);
    }

    @Test
    void selectWhereGroupByOrderBy() throws Exception {
        var q = select("status", "COUNT(*) AS cnt").from("users")
                .where(gt("age", 20))
                .groupBy("status")
                .orderBy("status")
                .build();
        var rs = execute(q);
        assertTrue(rs.next());
        assertEquals("active", rs.getString("status"));
        assertTrue(rs.next());
        assertEquals("inactive", rs.getString("status"));
        assertFalse(rs.next());
    }

    @Test
    void selectJoinMultiColumn() throws Exception {
        var q = select("*").from("orders")
                .join("order_details")
                .on(and(colEq("orders.id", "order_details.order_id"),
                        colEq("orders.product", "order_details.product")))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(4, count);
    }

    @Test
    void selectJoinWithAmountFilter() throws Exception {
        var q = select("*").from("users")
                .join("orders")
                .on(and(colEq("users.id", "orders.customer_id"),
                        gt("orders.amount", 10)))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        // Alice: Widget(10.99>10 ✓), Gadget(24.99>10 ✓) → 2
        // Bob:   Widget(10.99>10 ✓)                     → 1
        // Charlie: Doohickey(5.99>10 ✗)                 → 0
        assertEquals(3, count);
    }

    @Test
    void selectJoinOnOr() throws Exception {
        var q = select("*").from("orders")
                .join("order_details")
                .on(or(colEq("orders.id", "order_details.order_id"),
                        colEq("orders.product", "order_details.product")))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertTrue(count >= 4);
    }

    @Test
    void selectJoinWithWhereAndAfterOn() throws Exception {
        var q = select("*").from("users")
                .join("orders").on(colEq("users.id", "orders.customer_id"))
                .where(and(eq("users.status", "active"), gt("orders.amount", 15)))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        // Alice (active) has Gadget (24.99>15 ✓) and Widget (10.99>15 ✗) → 1
        // Bob (active) has Widget (10.99>15 ✗)                          → 0
        assertEquals(1, count);
    }

    @Test
    void selectJoin() throws Exception {
        var q = select("*").from("users")
                .join("orders").on(colEq("users.id", "orders.customer_id"))
                .where(eq("users.name", "Alice"))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(2, count);
    }

    @Test
    void selectInnerJoin() throws Exception {
        var q = select("*").from("users")
                .innerJoin("orders").on(colEq("users.id", "orders.customer_id"))
                .where(eq("users.name", "Alice"))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(2, count);
    }

    @Test
    void selectLeftJoin() throws Exception {
        var q = select("*").from("users")
                .leftJoin("orders").on(colEq("users.id", "orders.customer_id"))
                .build();
        var rs = execute(q);
        int count = 0;
        int nullCount = 0;
        while (rs.next()) {
            count++;
            if (rs.getObject("customer_id") == null) nullCount++;
        }
        // 5 users; Alice(2orders), Bob(1), Charlie(1), Diana(0), Eve(0) → 6 rows
        assertEquals(6, count);
        // Diana and Eve have no orders → customer_id is null
        assertEquals(2, nullCount);
    }

    @Test
    void selectCrossJoin() throws Exception {
        var q = select("*").from("users")
                .crossJoin("orders")
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(20, count);
    }

    @Test
    void selectMultipleJoins() throws Exception {
        var q = select("*").from("users")
                .leftJoin("orders").on(colEq("users.id", "orders.customer_id"))
                .join("order_details")
                .on(and(colEq("orders.id", "order_details.order_id"),
                        colEq("orders.product", "order_details.product")))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        // Alice has orders 1 (Widget) + 2 (Gadget); order 2 has no matching detail → inner join drops it
        // Actually the second join is a plain JOIN (INNER), so only rows with matching order_details survive:
        //   Alice+order1+Widget matches detail(1,Widget) ✓
        //   Alice+order2+Gadget → no matching detail (detail has order_id=2,product=Gadget) → dropped
        //   Bob+order3+Widget → no matching detail (detail has order_id=3,product=Widget → wait let me re-check)

        // order_details: (1,Widget,2), (2,Gadget,1), (3,Widget,3), (4,Doohickey,5)
        // orders: 1(Alice,Widget), 2(Alice,Gadget), 3(Bob,Widget), 4(Charlie,Doohickey)
        // LEFT JOIN users→orders on customer_id keeps all users
        //   Alice + order1(Widget)  → matches detail(1,Widget) ✓
        //   Alice + order2(Gadget)  → no matching detail (order_id=2,Gadget exists) ✓
        //   Bob   + order3(Widget)  → no matching detail (order_id=3,Widget exists) ✓
        //   Charlie + order4(...)   → matches detail(4,Doohickey) ✓
        //   Diana + null              → second JOIN drops (null.id = ?)
        //   Eve   + null              → second JOIN drops
        // Hmm, the second JOIN's ON clause references orders.id, which is null for Diana/Eve.
        // In SQLite, null comparisons in ON are false, so those rows are dropped.
        assertEquals(4, count);
    }

    @Test
    void selectSubquery() throws Exception {
        var q = select("*").from("users")
                .where(in("id",
                        select("customer_id").from("orders")
                                .where(eq("product", "Widget"))))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(2, count);
    }

    @Test
    @org.junit.jupiter.api.Disabled("SQLite does not support SQL:2008 FETCH NEXT syntax; use PostgreSQL, H2, or Derby for LIMIT/OFFSET tests")
    void selectLimit() throws Exception {
        var q = select("*").from("users").orderBy("id").limit(3).build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        assertEquals(3, count);
    }

    @Test
    @org.junit.jupiter.api.Disabled("SQLite does not support SQL:2008 OFFSET...FETCH NEXT syntax; use PostgreSQL, H2, or Derby for OFFSET tests")
    void selectOffset() throws Exception {
        var q = select("*").from("users").orderBy("id").desc().offset(2).limit(2).build();
        var rs = execute(q);
        assertTrue(rs.next());
        assertEquals(3, rs.getInt("id"));
        assertTrue(rs.next());
        assertEquals(2, rs.getInt("id"));
        assertFalse(rs.next());
    }

    // ---- DELETE ----

    @Test
    void deleteWhere() throws Exception {
        var q = QueryBuilder.delete().from("users").where(eq("name", "Eve")).build();
        int deleted = executeUpdate(q);
        assertEquals(1, deleted);
        var rs = execute(select("*").from("users").build());
        int count = 0;
        while (rs.next()) count++;
        assertEquals(4, count);
    }

    // ---- UPDATE ----

    @Test
    void updateWhere() throws Exception {
        var q = QueryBuilder.update("users")
                .set("status", "inactive")
                .where(eq("name", "Alice"))
                .build();
        int updated = executeUpdate(q);
        assertEquals(1, updated);
        var rs = execute(select("*").from("users").where(eq("name", "Alice")).build());
        assertTrue(rs.next());
        assertEquals("inactive", rs.getString("status"));
    }

    @Test
    void updateMultiSet() throws Exception {
        var q = QueryBuilder.update("users")
                .set("status", "active")
                .set("age", 30)
                .where(eq("name", "Eve"))
                .build();
        int updated = executeUpdate(q);
        assertEquals(1, updated);
        var rs = execute(select("*").from("users").where(eq("name", "Eve")).build());
        assertTrue(rs.next());
        assertEquals("active", rs.getString("status"));
        assertEquals(30, rs.getInt("age"));
    }

    // ---- INSERT ... SELECT ----

    @Test
    void insertSelect() throws Exception {
        try (var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE inactive_users (name TEXT, email TEXT)");
        }
        var q = QueryBuilder.insertInto("inactive_users", "name", "email")
                .select("name", "email").from("users")
                .where(eq("status", "inactive"))
                .build();
        int inserted = executeUpdate(q);
        assertEquals(2, inserted);
        var rs = execute(select("*").from("inactive_users").build());
        int count = 0;
        while (rs.next()) count++;
        assertEquals(2, count);
    }

    @Test
    void insertSelectDistinct() throws Exception {
        // Create a temp table, insert distinct statuses from users
        try (var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE statuses (status TEXT)");
        }
        var q = QueryBuilder.insertInto("statuses", "status")
                .selectDistinct("status").from("users")
                .build();
        int inserted = executeUpdate(q);
        assertEquals(2, inserted);
    }

    @Test
    void selectHaving() throws Exception {
        var q = select("status", "COUNT(*) AS cnt").from("users")
                .groupBy("status")
                .having(gt("COUNT(*)", 2))
                .build();
        var rs = execute(q);
        // active=3, inactive=2 → only active has COUNT(*) > 2
        assertTrue(rs.next());
        assertEquals("active", rs.getString("status"));
        assertEquals(3, rs.getInt("cnt"));
        assertFalse(rs.next());
    }

    @Test
    void selectUnion() throws Exception {
        try (var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE tmp (name TEXT)");
            stmt.execute("INSERT INTO tmp (name) SELECT name FROM users WHERE status = 'active'");
        }
        var q = select("name").from("users").where(eq("status", "inactive"))
                .union(select("name").from("tmp"))
                .build();
        var rs = execute(q);
        int count = 0;
        while (rs.next()) count++;
        // inactive: Charlie, Eve (2) + active: Alice, Bob, Diana (3) = 5
        assertEquals(5, count);
    }

}
