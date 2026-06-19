package querybuilder.api;

import java.util.StringJoiner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import querybuilder.api.FilterBuilder.*;

/**
 * Fluent SQL query builder.
 *
 * <p>Entry points:
 * <ul>
 *   <li>{@link #select(String...)} / {@link #selectDistinct(String...)}</li>
 *   <li>{@link #delete()}</li>
 *   <li>{@link #insertInto(String, String...)}</li>
 *   <li>{@link #update(String)}</li>
 * </ul>
 *
 * <p>Each returns a builder-step inner class. Chain methods in SQL clause order
 * and call {@link TerminalClause#build()} to produce a {@link Query} record
 * containing the SQL string (with {@code ?} placeholders) and the list of
 * parameter values.
 *
 * <p>The generated SQL uses {@code ?} placeholders for all values (JDBC
 * prepared-statement friendly) and omits the trailing semicolon.
 */
public class QueryBuilder {
    record Filter(String sql, List<Object> params) {
    }

    class SQLFilterBuilderStategy implements FilterBuilder.BuilderStrategy<Filter> {

        @Override
        public Filter build(IExpression expr) {
            return switch (expr) {
                case ColEq(var left, var right) -> new Filter(left + " = " + right, List.of());
                case Equals(var key, var val) -> new Filter(key + " = ?", List.of(val));
                case Like(var key, var val) -> new Filter(key + " LIKE ?", List.of(val));
                case IsNotNull(var key) -> new Filter(key + " IS NOT NULL", List.of());
                case IsNull(var key) -> new Filter(key + " IS NULL", List.of());
                case GreaterThan(var key, var val) -> new Filter(key + " > ?", List.of(val));
                case LessThan(var key, var val) -> new Filter(key + " < ?", List.of(val));
                case And(var exprs) -> buildCompound(") AND (", exprs);
                case Or(var exprs) -> buildCompound(") OR (", exprs);
                case In(var key, var vals) -> buildIn(key, vals);
                case InSubquery(var key, var subquery) -> buildInSubquery(key, subquery);
                case Between(var key, var first, var second) -> buildBetween(key, first, second);
                case Not(var inner) -> buildNot(inner);
                case Exists(var subquery) -> buildExists(subquery);
                case NotExists(var subquery) -> buildNotExists(subquery);
            };
        }

        private Filter buildExists(TerminalClause subquery) {
            var q = subquery.build();
            return new Filter("EXISTS (" + q.query() + ")", q.values());
        }

        private Filter buildNotExists(TerminalClause subquery) {
            var q = subquery.build();
            return new Filter("NOT EXISTS (" + q.query() + ")", q.values());
        }

        private Filter buildNot(IExpression expr) {
            var f = build(expr);
            return new Filter("NOT (" + f.sql() + ")", f.params());
        }

        private Filter buildIn(String key, List<Object> vals) {
            var placeholders = String.join(", ", Collections.nCopies(vals.size(), "?"));
            return new Filter(key + " IN (" + placeholders + ")", vals);
        }

        private Filter buildBetween(String key, Object first, Object second) {
            return new Filter(key + " BETWEEN ? AND ?", List.of(first, second));
        }

        private Filter buildInSubquery(String key, TerminalClause subquery) {
            var q = subquery.build();
            return new Filter(key + " IN (" + q.query() + ")", q.values());
        }

        private Filter buildCompound(String op, List<IExpression> expressions) {
            var params = new ArrayList<>();
            var joiner = new StringJoiner(op, "(", ")");
            for (var expr : expressions) {
                var f = build(expr);
                params.addAll(f.params());
                joiner.add(f.sql());
            }
            return new Filter(joiner.toString(), params);
        }
    }

    StringJoiner buf = new StringJoiner(" ");
    List<Object> values = new ArrayList<>();
    SQLFilterBuilderStategy strat = new SQLFilterBuilderStategy();

    private QueryBuilder() {
    }

    /**
     * A built query: a SQL string with {@code ?} placeholders and a list of
     * parameter values in binding order.
     */
    public record Query(String query, List<Object> values) {
    }

    /**
     * Starts a {@code SELECT} statement.
     * @param columns the column(s) to select (e.g. {@code "*"}, {@code "id", "name"})
     */
    public static Select select(String ...columns) {
        var builder = new QueryBuilder();
        return builder.new Select(columns);
    }

    /**
     * Starts a {@code SELECT DISTINCT} statement.
     * @param columns the column(s) to select
     */
    public static Select selectDistinct(String ...columns) {
        var builder = new QueryBuilder();
        return builder.new Select(true, columns);
    }

    /**
     * Starts a {@code DELETE} statement (must call {@code .from(...)} next).
     */
    public static Delete delete() {
        var builder = new QueryBuilder();
        return builder.new Delete();
    }

    /**
     * Starts an {@code INSERT INTO} statement.
     * @param table  the target table name
     * @param columns the column names to insert into
     */
    public static Insert insertInto(String table, String ...columns) {
        var builder = new QueryBuilder();
        return builder.new Insert(table, columns);
    }

    /**
     * Starts an {@code UPDATE} statement.
     * @param table the table name
     */
    public static Update update(String table) {
        var builder = new QueryBuilder();
        return builder.new Update(table);
    }

    // ---- inner classes -------------------------------------------------------

    /**
     * Builder step for the {@code SELECT} clause.
     * <p>A {@code From} step is required next ({@link #from(String)} or
     * {@link #from(TerminalClause)}).
     */
    public class Select {
        Select(boolean distinct, String ...columns) {
            Objects.requireNonNull(columns);
            buf.add("SELECT" + (distinct ? " DISTINCT " : " ") + String.join(", ", columns));
        }

        Select(String ...columns) {
            this(false, columns);
        }

        /**
         * Sets the FROM clause to a table.
         * @param table the table name
         */
        public From from(String table) {
            return new From(table);
        }

        /**
         * Sets the FROM clause to a table with an alias.
         * <p>Equivalent to {@code from(table).as(alias)} but omits the {@code AS}
         * keyword in the output.
         */
        public From from(String table, String alias) {
            return new From(table, alias);
        }

        /**
         * Sets the FROM clause to a subquery.
         * <p>Use {@link From#as(String)} to assign a subquery alias.
         * @param terminalClause the subquery (e.g. {@code select(...).from(...)})
         */
        public From from(TerminalClause terminalClause) {
            return new From(terminalClause);
        }
    }

    /**
     * Builder step for the {@code DELETE} clause.
     * <p>A {@code From} step is required next.
     */
    public class Delete {
        /**
         * Sets the FROM clause for a DELETE statement.
         * @param table the table name
         */
        public From from(String table) {
            buf.add("DELETE");
            return new From(table);
        }

        /**
         * Sets the FROM clause for a DELETE statement with a table alias.
         */
        public From from(String table, String alias) {
            buf.add("DELETE");
            return new From(table, alias);
        }
    }

    /**
     * Builder step for the {@code INSERT INTO} clause.
     * <p>Follow with {@link #values(Object...)} for inline values or
     * {@link #select(String...)} / {@link #selectDistinct(String...)} for an
     * INSERT ... SELECT.
     */
    public class Insert {
        Insert(String table, String ...columns) {
            Objects.requireNonNull(table);
            Objects.requireNonNull(columns);
            buf.add("INSERT INTO " + table + " (" + String.join(", ", columns) + ")");
        }

        /**
         * Adds a {@code VALUES} clause with the given row of values.
         * @param values the values to insert
         */
        public Values values(Object ...values) {
            buf.add("VALUES");
            return new Values(values);
        }

        /**
         * Starts an INSERT ... SELECT with the given columns.
         * <p>The returned {@link Select} must be chained with {@code .from(...)}.
         */
        public Select select(String ...columns) {
            return new Select(columns);
        }

        /**
         * Starts an INSERT ... SELECT DISTINCT with the given columns.
         */
        public Select selectDistinct(String ...columns) {
            return new Select(true, columns);
        }
    }

    /**
     * Builder step for the {@code UPDATE} clause.
     * <p>Follow with {@link #set(String, Object)}.
     */
    public class Update {
        Update(String table) {
            Objects.requireNonNull(table);
            buf.add("UPDATE").add(table);
        }

        /**
         * Adds a {@code SET} assignment.
         * @param col the column name
         * @param val the value (parameterized with {@code ?})
         */
        public Set set(String col, Object val) {
            return new Set(col, val);
        }
    }

    /**
     * Builder step for the {@code SET} clause of an UPDATE.
     * <p>Additional columns can be set via {@link #set(String, Object)}.
     * After all columns are set, optional {@code WHERE} / {@code ORDER BY} /
     * {@code LIMIT} clauses may follow (see inherited chain).
     */
    public class Set extends Filterable {
        Set(String col, Object val) {
            Objects.requireNonNull(col);
            Objects.requireNonNull(val);
            values.add(val);
            buf.add("SET").add(col).add("= ?");
        }

        /**
         * Adds another column assignment to the SET clause.
         * @param col the column name
         * @param val the value
         */
        public Set set(String col, Object val) {
            Objects.requireNonNull(col);
            Objects.requireNonNull(val);
            values.add(val);
            buf.add(",").add(col).add("= ?");
            return this;
        }
    }

    /**
     * Builder step for the {@code VALUES} clause of an INSERT.
     * <p>Additional value rows can be added via {@link #values(Object...)}.
     * Call {@link #build()} to finish.
     */
    public class Values extends TerminalClause {
        Values(Object ...vals) {
            Objects.requireNonNull(vals);
            values.addAll(Arrays.asList(vals));
            buf.add("(" + String.join(", ", Collections.nCopies(vals.length, "?")) + ")");
        }

        /**
         * Adds another value row (comma-separated).
         * @param vals the values for the next row
         */
        public Values values(Object ...vals) {
            buf.add(",");
            return new Values(vals);
        }
    }

    /**
     * Builder step for the {@code FROM} clause.
     * <p>After FROM, call {@link #as(String)} to alias a subquery, or proceed
     * directly to JOIN / WHERE / GROUP BY / ORDER BY / LIMIT clauses.
     */
    public class From extends Joinable {
        From(String table) {
            Objects.requireNonNull(table);
            buf.add("FROM " + table);
        }

        From(String table, String alias) {
            Objects.requireNonNull(table);
            Objects.requireNonNull(alias);
            buf.add("FROM " + table + " " + alias);
        }

        From(TerminalClause terminalClause) {
            var query = terminalClause.build();
            values.addAll(query.values());
            buf.add("FROM (" + query.query() + ")");
        }

        /**
         * Assigns a subquery alias with the {@code AS} keyword.
         * <p>For table aliases, prefer the two-argument
         * {@link Select#from(String, String)} which omits {@code AS}.
         * @param alias the alias name
         */
        public From as(String alias) {
            Objects.requireNonNull(alias);
            buf.add("AS " + alias);
            return this;
        }
    }

    /**
     * Builder step for the {@code ORDER BY} clause.
     * <p>Follow with {@link #asc()} / {@link #desc()}, then optionally
     * {@link #offset(int)} and/or {@link Limitable#limit(int)}.
     */
    public class OrderBy extends Limitable {
        OrderBy(String ...columns) {
            Objects.requireNonNull(columns);
            buf.add("ORDER BY " + String.join(", ", columns));
        }

        /**
         * Appends {@code ASC} (ascending order).
         */
        public OrderBy asc() {
            buf.add("ASC");
            return this;
        }

        /**
         * Appends {@code DESC} (descending order).
         */
        public OrderBy desc() {
            buf.add("DESC");
            return this;
        }

        /**
         * Adds an {@code OFFSET ? ROWS} clause.
         * @param offset the number of rows to skip
         */
        public Offset offset(int offset) {
            return new Offset(offset);
        }
    }

    /**
     * Builder step for the {@code OFFSET} clause.
     * <p>Must be preceded by {@code ORDER BY}. Follow with
     * {@link Limitable#limit(int)}.
     */
    public class Offset extends Limitable {
        Offset(int offset) {
            values.add(offset);
            buf.add("OFFSET ? ROWS");
        }
    }

    /**
     * Builder step for the {@code FETCH NEXT ? ROWS ONLY} clause (SQL:2008
     * standard LIMIT equivalent).
     * <p>Some databases (e.g. SQLite 3.x) do not support this syntax.
     */
    public class Limit extends TerminalClause {
        Limit(int limit) {
            values.add(limit);
            buf.add("FETCH NEXT ? ROWS ONLY");
        }
    }

    /**
     * Builder step for the {@code WHERE} clause.
     * <p>Accepts a {@link IExpression} and consumes it (single-use).
     * After WHERE, optional {@code GROUP BY} / {@code ORDER BY} / {@code LIMIT}
     * clauses may follow.
     */
    public class Where extends Groupable {
        Where(IExpression condition) {
            var filter = condition.build(strat);
            values.addAll(filter.params());
            buf.add("WHERE").add(filter.sql());
        }
    }

    /**
     * Builder step for the {@code JOIN} clause.
     * <p>Supply the join condition via {@link #on(IExpression)} or
     * {@link #using(String...)}. A plain {@code JOIN} (without qualifier)
     * produces {@code JOIN}. Use the methods on {@link Joinable} (e.g.
     * {@link Joinable#leftJoin(String) leftJoin()}) to add a qualifier.
     *
     * <p><b>Quirk:</b> {@code Join extends TerminalClause}, so
     * {@code crossJoin("t").build()} compiles without an {@code .on()} call.
     */
    public class Join extends TerminalClause {
        Join(String table) {
            buf.add("JOIN " + table);
        }

        Join(TerminalClause terminalClause) {
            var query = terminalClause.build();
            values.addAll(query.values());
            buf.add("JOIN (" + query.query() + ")");
        }

        /**
         * Assigns a subquery alias with the {@code AS} keyword.
         * @param alias the alias name
         */
        public Join as(String alias) {
            Objects.requireNonNull(alias);
            buf.add("AS " + alias);
            return this;
        }

        /**
         * Adds an {@code ON} clause with the given condition.
         * @param condition the join condition (consumed, single-use)
         */
        public On on(IExpression condition) {
            return new On(condition);
        }

        /**
         * Adds a {@code USING} clause with the given column names.
         * @param columns the shared column names
         */
        public On using(String ...columns) {
            return new On(columns);
        }
    }

    /**
     * Builder step for the {@code ON} or {@code USING} clause of a JOIN.
     * <p>After ON/USING, further JOIN or WHERE / GROUP BY / ORDER BY / LIMIT
     * clauses may follow.
     */
    public class On extends Joinable {
        On(IExpression condition) {
            var filter = condition.build(strat);
            values.addAll(filter.params());
            buf.add("ON").add(filter.sql());
        }

        On(String ...columns) {
            buf.add("USING (" + String.join(", ", columns) + ")");
        }
    }

    /**
     * Abstract step that exposes all JOIN variants after FROM or ON.
     *
     * <p>Available joins:
     * <ul>
     *   <li>{@link #join(String)} / {@link #join(TerminalClause)}</li>
     *   <li>{@link #innerJoin(String)}</li>
     *   <li>{@link #leftJoin(String)}</li>
     *   <li>{@link #rightJoin(String)}</li>
     *   <li>{@link #fullJoin(String)}</li>
     *   <li>{@link #crossJoin(String)}</li>
     *   <li>{@link #outerJoin(String)}</li>
     *   <li>{@link #naturalJoin(String)}</li>
     * </ul>
     * Each method also accepts a {@link TerminalClause} for subquery joins.
     */
    public abstract class Joinable extends Filterable {
        /**
         * Adds an {@code INNER JOIN} (or plain {@code JOIN}).
         * @param table the table name
         */
        public Join join(String table) {
            return new Join(table);
        }

        /**
         * Adds a {@code JOIN} to a subquery.
         * <p>Use {@link Join#as(String)} to alias the subquery.
         * @param subquery the subquery
         */
        public Join join(TerminalClause subquery) {
            return new Join(subquery);
        }

        /**
         * Adds an {@code INNER JOIN}.
         * @param table the table name
         */
        public Join innerJoin(String table) {
            buf.add("INNER");
            return new Join(table);
        }

        /**
         * Adds an {@code INNER JOIN} to a subquery.
         */
        public Join innerJoin(TerminalClause subquery) {
            buf.add("INNER");
            return new Join(subquery);
        }

        /**
         * Adds a {@code LEFT JOIN}.
         * @param table the table name
         */
        public Join leftJoin(String table) {
            buf.add("LEFT");
            return new Join(table);
        }

        /**
         * Adds a {@code LEFT JOIN} to a subquery.
         */
        public Join leftJoin(TerminalClause subquery) {
            buf.add("LEFT");
            return new Join(subquery);
        }

        /**
         * Adds a {@code RIGHT JOIN}.
         * @param table the table name
         */
        public Join rightJoin(String table) {
            buf.add("RIGHT");
            return new Join(table);
        }

        /**
         * Adds a {@code RIGHT JOIN} to a subquery.
         */
        public Join rightJoin(TerminalClause subquery) {
            buf.add("RIGHT");
            return new Join(subquery);
        }

        /**
         * Adds a {@code FULL JOIN}.
         * @param table the table name
         */
        public Join fullJoin(String table) {
            buf.add("FULL");
            return new Join(table);
        }

        /**
         * Adds a {@code FULL JOIN} to a subquery.
         */
        public Join fullJoin(TerminalClause subquery) {
            buf.add("FULL");
            return new Join(subquery);
        }

        /**
         * Adds a {@code CROSS JOIN}.
         * @param table the table name
         */
        public Join crossJoin(String table) {
            buf.add("CROSS");
            return new Join(table);
        }

        /**
         * Adds a {@code CROSS JOIN} to a subquery.
         */
        public Join crossJoin(TerminalClause subquery) {
            buf.add("CROSS");
            return new Join(subquery);
        }

        /**
         * Adds an {@code OUTER JOIN}.
         * @param table the table name
         */
        public Join outerJoin(String table) {
            buf.add("OUTER");
            return new Join(table);
        }

        /**
         * Adds an {@code OUTER JOIN} to a subquery.
         */
        public Join outerJoin(TerminalClause subquery) {
            buf.add("OUTER");
            return new Join(subquery);
        }

        /**
         * Adds a {@code NATURAL JOIN}.
         * @param table the table name
         */
        public Join naturalJoin(String table) {
            buf.add("NATURAL");
            return new Join(table);
        }

        /**
         * Adds a {@code NATURAL JOIN} to a subquery.
         */
        public Join naturalJoin(TerminalClause subquery) {
            buf.add("NATURAL");
            return new Join(subquery);
        }
    }

    /**
     * Builder step for the {@code GROUP BY} clause.
     * <p>Follow with {@link #having(IExpression)} or proceed to
     * {@code ORDER BY} / {@code LIMIT}.
     */
    public class GroupBy extends Orderable {
        GroupBy(String ...columns) {
            Objects.requireNonNull(columns);
            buf.add("GROUP BY").add(String.join(", ", columns));
        }

        /**
         * Adds a {@code HAVING} clause (filter after aggregation).
         * @param condition the having condition (consumed, single-use)
         */
        public Having having(IExpression condition) {
            return new Having(condition);
        }
    }

    /**
     * Builder step for the {@code HAVING} clause.
     * <p>After HAVING, proceed to {@code ORDER BY} / {@code LIMIT}.
     */
    public class Having extends Orderable {
        Having(IExpression condition) {
            var filter = condition.build(strat);
            values.addAll(filter.params());
            buf.add("HAVING").add(filter.sql());
        }
    }

    /**
     * Terminal clause that also exposes set operations.
     *
     * <p>Call {@link #build()} to finalize the query, or chain
     * {@link #union(TerminalClause)}, {@link #intersect(TerminalClause)},
     * or {@link #except(TerminalClause)} for set operations.
     */
    public abstract class TerminalClause {
        /**
         * Builds the query into a {@link Query} record.
         * <p>The returned object contains the SQL string and the parameter
         * values. The builder may be discarded after this call.
         */
        public Query build() {
            return new Query(buf.toString(), values);
        }

        /**
         * Combines this query with another using {@code UNION}.
         * <p>Example: {@code select("*").from("a").union(select("*").from("b"))}
         * produces {@code SELECT * FROM a UNION SELECT * FROM b}.
         * @param other the second query
         */
        public Union union(TerminalClause other) {
            return new Union("UNION", other);
        }

        /**
         * Combines this query with another using {@code INTERSECT}.
         * @param other the second query
         */
        public Union intersect(TerminalClause other) {
            return new Union("INTERSECT", other);
        }

        /**
         * Combines this query with another using {@code EXCEPT}.
         * @param other the second query
         */
        public Union except(TerminalClause other) {
            return new Union("EXCEPT", other);
        }
    }

    /**
     * Builder step for set operations (UNION / INTERSECT / EXCEPT).
     *
     * <p>After a set operation, optional {@code ORDER BY} / {@code LIMIT}
     * clauses may follow.
     *
     * <p><b>Quirk:</b> The {@link #build()} method is inherited from
     * {@link TerminalClause}. Calling {@code build()} on the second union
     * argument (the {@code other}) is done eagerly in the constructor,
     * so the other query must be fully built before being passed to
     * {@code union()}.
     */
    public class Union extends Orderable {
        Union(String operator, TerminalClause other) {
            var current = build();
            var otherQuery = other.build();
            values = new ArrayList<>(current.values());
            values.addAll(otherQuery.values());
            buf = new StringJoiner(" ");
            buf.add(current.query()).add(operator).add(otherQuery.query());
        }
    }

    /**
     * Abstract step that exposes {@link #limit(int)} after ORDER BY or OFFSET.
     */
    public abstract class Limitable extends TerminalClause {
        /**
         * Adds a {@code FETCH NEXT ? ROWS ONLY} clause (LIMIT).
         * @param limit the maximum number of rows to return
         */
        public Limit limit(int limit) {
            return new Limit(limit);
        }
    }

    /**
     * Abstract step that exposes {@link #orderBy(String...)}.
     */
    public abstract class Orderable extends Limitable {
        /**
         * Adds an {@code ORDER BY} clause.
         * @param columns the column(s) to order by
         */
        public OrderBy orderBy(String ...columns) {
            return new OrderBy(columns);
        }
    }

    /**
     * Abstract step that exposes {@link #groupBy(String...)}.
     */
    public abstract class Groupable extends Orderable {
        /**
         * Adds a {@code GROUP BY} clause.
         * @param columns the column(s) to group by
         */
        public GroupBy groupBy(String ...columns) {
            return new GroupBy(columns);
        }
    }

    /**
     * Abstract step that exposes {@link #where(IExpression)}.
     */
    public abstract class Filterable extends Groupable {
        /**
         * Adds a {@code WHERE} clause.
         * @param condition the filter condition (consumed, single-use)
         */
        public Where where(IExpression condition) {
            return new Where(condition);
        }
    }

}
