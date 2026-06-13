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
        Select(String ...columns) {
            Objects.requireNonNull(columns);
            buf.add("SELECT " + String.join(", ", columns));
        }
        public From from(String table) {
            return new From(table);
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

    public class From extends Filterable {
        From(String table) {
            Objects.requireNonNull(table);
            buf.add("FROM " + table);
        }
        public Join join(String otherTable) {
            return new Join(otherTable);
        }
    }

    public class OrderBy extends Limitable {
        OrderBy(String ...columns) {
            Objects.requireNonNull(columns);
            buf.add("ORDER BY " + String.join(", ", columns));
        }
        public Asc asc() {
            return new Asc();
        }
        public Desc desc() {
            return new Desc();
        }
        public Offset offset(int offset) {
            return new Offset(offset);
        }
    }

    public class Asc extends Limitable {
        Asc() {
            buf.add("ASC");
        }
        public Offset offset(int offset) {
            return new Offset(offset);
        }
    }

    public class Desc extends Limitable {
        Desc() {
            buf.add("DESC");
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

    public class Where extends Orderable {
        Where(Condition condition) {
            var filter = condition.build();
            values.addAll(filter.values());
            buf.add("WHERE").merge(filter.buf());
        }
    }

    public class Join {
        Join(String table) {
            buf.add("JOIN " + table);
        }

        public On on(String on) {
            return new On(on);
        }
    }

    public class On {
        On(String column) {
            buf.add("ON " + column);
        }

        public Eq eq(String otherColumn) {
            return new Eq(otherColumn);
        }
    }

    public class Eq extends Filterable  {
        Eq(String otherColumn) {
            buf.add("= " + otherColumn);
        }
    }

    public abstract class TerminalClause {
        public Query build() {
            return new Query(buf.toString(), values);
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
    public abstract class Filterable extends Orderable {
        public Where where(Condition condition) {
            return new Where(condition);
        }
    }

}
