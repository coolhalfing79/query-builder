package querybuilder.api;

import java.util.StringJoiner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import querybuilder.api.FilterBuilder.Condition;

public class QueryBuilder {
    StringJoiner buf = new StringJoiner(" ");
    List<Object> values = new ArrayList<>();
    private QueryBuilder() { }

    public record Query(String query, List<Object> values) {}

    public static Select select(String ...columns) {
        var builder = new QueryBuilder();
        return builder.new Select(columns);
    }

    public static Select selectDistinct(String ...columns) {
        var builder = new QueryBuilder();
        return builder.new Select(true, columns);
    }

    public static Delete delete() {
        var builder = new QueryBuilder();
        return builder.new Delete();
    }

    public static Insert insertInto(String table, String ...columns) {
        var builder = new QueryBuilder();
        return builder.new Insert(table, columns);
    }

    public static Update update(String table) {
        var builder = new QueryBuilder();
        return builder.new Update(table);
    }

    public class Select {
        Select(boolean distinct, String ...columns) {
            Objects.requireNonNull(columns);
            buf.add("SELECT" + (distinct ? " DISTINCT " : " ") + String.join(", ", columns));
        }
        Select(String ...columns) {
            this(false, columns);
        }
        public From from(String table) {
            return new From(table);
        }

        public From from(TerminalClause terminalClause) {
            return new From(terminalClause);
        }
    }

    public class Delete {
        public From from(String table) {
            buf.add("DELETE");
            return new From(table);
        }
    }

    public class Insert {
        Insert(String table, String ...columns) {
            Objects.requireNonNull(table);
            Objects.requireNonNull(columns);
            buf.add("INSERT INTO " + table + " (" + String.join(", ", columns) + ")");
        }
        public Values values(Object ...values) {
            buf.add("VALUES");
            return new Values(values);
        }

        public Select select(String ...columns) {
            return new Select(columns);
        }

        public Select selectDistinct(String ...columns) {
            return new Select(true, columns);
        }
    }

    public class Update {
        Update(String table) {
            Objects.requireNonNull(table);
            buf.add("UPDATE").add(table);
        }
        public Set set(String col, Object val) {
            return new Set(col, val);
        }
    }

    public class Set extends Filterable {
        Set(String col, Object val) {
            Objects.requireNonNull(col);
            Objects.requireNonNull(val);
            values.add(val);
            buf.add("SET").add(col).add("= ?");
        }
        public Set set(String col, Object val) {
            Objects.requireNonNull(col);
            Objects.requireNonNull(val);
            values.add(val);
            buf.add(",").add(col).add("= ?");
            return this;
        }
    }

    public class Values extends TerminalClause {
        Values(Object ...vals) {
            Objects.requireNonNull(vals);
            values.addAll(Arrays.asList(vals));
            buf.add("(" + String.join(", ", Collections.nCopies(vals.length, "?")) + ")");
        }

        public Values values(Object ...vals) {
            buf.add(",");
            return new Values(vals);
        }
    }

    public class From extends Joinable {
        From(String table) {
            Objects.requireNonNull(table);
            buf.add("FROM " + table);
        }

        From(TerminalClause terminalClause) {
            var query = terminalClause.build();
            values.addAll(query.values());
            buf.add("FROM (" + query.query() + ")");
        }

        public From as(String alias) {
            Objects.requireNonNull(alias);
            buf.add("AS " + alias);
            return this;
        }
    }

    public class OrderBy extends Limitable {
        OrderBy(String ...columns) {
            Objects.requireNonNull(columns);
            buf.add("ORDER BY " + String.join(", ", columns));
        }
        public OrderBy asc() {
            buf.add("ASC");
            return this;
        }
        public OrderBy desc() {
            buf.add("DESC");
            return this;
        }
        public Offset offset(int offset) {
            return new Offset(offset);
        }
    }

    public class Offset extends Limitable {
        Offset(int offset) {
            values.add(offset);
            buf.add("OFFSET ? ROWS");
        }
    }

    public class Limit extends TerminalClause {
        Limit(int limit) {
            values.add(limit);
            buf.add("FETCH NEXT ? ROWS ONLY");
        }
    }

    public class Where extends Groupable {
        Where(Condition condition) {
            var filter = condition.build();
            values.addAll(filter.values());
            buf.add("WHERE").merge(filter.buf());
        }
    }

    public class Join extends TerminalClause {
        Join(String table) {
            buf.add("JOIN " + table);
        }

        Join(TerminalClause terminalClause) {
            var query = terminalClause.build();
            values.addAll(query.values());
            buf.add("JOIN (" + query.query() + ")");
        }

        public Join as(String alias) {
            Objects.requireNonNull(alias);
            buf.add("AS " + alias);
            return this;
        }

        public On on(Condition condition) {
            return new On(condition);
        }

        public On using(String... columns) {
            return new On(columns);
        }
    }

    public class On extends Joinable {
        On(Condition condition) {
            var filter = condition.build();
            values.addAll(filter.values());
            buf.add("ON").merge(filter.buf());
        }

        On(String... columns) {
            buf.add("USING (" + String.join(", ", columns) + ")");
        }
    }

    public abstract class Joinable extends Filterable {
        public Join join(String table) {
            return new Join(table);
        }

        public Join join(TerminalClause subquery) {
            return new Join(subquery);
        }

        public Join innerJoin(String table) {
            buf.add("INNER");
            return new Join(table);
        }

        public Join innerJoin(TerminalClause subquery) {
            buf.add("INNER");
            return new Join(subquery);
        }

        public Join leftJoin(String table) {
            buf.add("LEFT");
            return new Join(table);
        }

        public Join leftJoin(TerminalClause subquery) {
            buf.add("LEFT");
            return new Join(subquery);
        }

        public Join rightJoin(String table) {
            buf.add("RIGHT");
            return new Join(table);
        }

        public Join rightJoin(TerminalClause subquery) {
            buf.add("RIGHT");
            return new Join(subquery);
        }

        public Join fullJoin(String table) {
            buf.add("FULL");
            return new Join(table);
        }

        public Join fullJoin(TerminalClause subquery) {
            buf.add("FULL");
            return new Join(subquery);
        }

        public Join crossJoin(String table) {
            buf.add("CROSS");
            return new Join(table);
        }

        public Join crossJoin(TerminalClause subquery) {
            buf.add("CROSS");
            return new Join(subquery);
        }

        public Join outerJoin(String table) {
            buf.add("OUTER");
            return new Join(table);
        }

        public Join outerJoin(TerminalClause subquery) {
            buf.add("OUTER");
            return new Join(subquery);
        }

        public Join naturalJoin(String table) {
            buf.add("NATURAL");
            return new Join(table);
        }

        public Join naturalJoin(TerminalClause subquery) {
            buf.add("NATURAL");
            return new Join(subquery);
        }
    }

    public class GroupBy extends Orderable {
        GroupBy(String ...columns) {
            Objects.requireNonNull(columns);
            buf.add("GROUP BY").add(String.join(", ", columns));
        }

        public Having having(Condition condition) {
            return new Having(condition);
        }
    }

    public class Having extends Orderable {
        Having(Condition condition) {
            var filter = condition.build();
            values.addAll(filter.values());
            buf.add("HAVING").merge(filter.buf());
        }
    }

    public abstract class TerminalClause {
        public Query build() {
            return new Query(buf.toString(), values);
        }

        public Union union(TerminalClause other) {
            return new Union("UNION", other);
        }

        public Union intersect(TerminalClause other) {
            return new Union("INTERSECT", other);
        }

        public Union except(TerminalClause other) {
            return new Union("EXCEPT", other);
        }
    }

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
    public abstract class Limitable extends TerminalClause {
        public Limit limit(int limit) {
            return new Limit(limit);
        }
    }
    public abstract class Orderable extends Limitable {
        public OrderBy orderBy(String ...columns) {
            return new OrderBy(columns);
        }
    }

    public abstract class Groupable extends Orderable {
        public GroupBy groupBy(String ...columns) {
            return new GroupBy(columns);
        }
    }

    public abstract class Filterable extends Groupable {
        public Where where(Condition condition) {
            return new Where(condition);
        }
    }

}
