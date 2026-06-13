package querybuilder.api;

import java.util.StringJoiner;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QueryBuilder {
    StringJoiner buf = new StringJoiner(" ");
    List<Object> values = new ArrayList<>();
    private QueryBuilder() { }

    record Query(String query, List<Object> values) {}

    public static Select select(String ...columns) {
        var builder = new QueryBuilder();
        return builder.new Select(columns);
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

    public class From extends Filterable {
        From(String table) {
            Objects.requireNonNull(table);
            buf.add("FROM " + table);
        }
        public Join join(String otherTable) {
            return new Join(otherTable);
        }
    }

    class OrderBy extends Limitable {
        OrderBy(String ...columns) {
            Objects.requireNonNull(columns);
            buf.add("ORDER BY " + String.join(", ", columns));
        }
        public Offset offset(int offset) {
            return new Offset(offset);
        }

        public Limit limit(int limit) {
            return new Limit(limit);
        }
    }

    public class Offset extends Limitable {
        Offset(int offset) {
            buf.add("OFFSET " + String.valueOf(offset) + " ROWS");
        }
    }

    public class Limit extends TerminalClause {
        Limit(int limit) {
            buf.add("FETCH NEXT " + String.valueOf(limit) + " ROWS ONLY");
        }
    }

    public class Where extends Orderable {
        Where(FilterBuilder.Filter filter) {
            values.addAll(filter.values());
            buf.add("WHERE ").merge(filter.buf());
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
            return new Query(buf.add(";").toString(), values);
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
        public Where where(FilterBuilder.Filter filter) {
            return new Where(filter);
        }
    }

}
