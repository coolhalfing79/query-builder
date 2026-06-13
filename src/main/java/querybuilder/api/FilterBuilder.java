package querybuilder.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import querybuilder.api.QueryBuilder.TerminalClause;

public class FilterBuilder {
    StringJoiner buf = new StringJoiner(" ");
    List<Object> values = new ArrayList<Object>();

    FilterBuilder() {
    }

    record Filter(StringJoiner buf, List<Object> values) {}

    public static Condition not(Condition condition) {
        var builder = new FilterBuilder();
        var filter = condition.build();
        builder.values.addAll(filter.values());
        builder.buf.add("NOT").add("(").merge(filter.buf()).add(")");
        return builder. new Condition();
    }

    public static Condition eq(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add(name).add("= ?");
        return builder. new Condition();
    }

    public static Condition not_eq(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add("NOT").add(name).add("= ?");
        return builder. new Condition();
    }

    public static Condition like(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add(name).add("LIKE ?");
        return builder. new Condition();
    }

    public static Condition not_like(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add(name).add("NOT LIKE ?");
        return builder. new Condition();
    }

    public static Condition between(String name, Object first, Object second) {
        var builder = new FilterBuilder();
        builder.values.add(first);
        builder.values.add(second);
        builder.buf.add(name).add("BETWEEN ? AND ?");
        return builder. new Condition();
    }

    public static Condition not_between(String name, Object first, Object second) {
        var builder = new FilterBuilder();
        builder.values.add(first);
        builder.values.add(second);
        builder.buf.add(name).add("NOT BETWEEN ? AND ?");
        return builder. new Condition();
    }

    public static Condition in(String name, Object ...values) {
        var builder = new FilterBuilder();
        builder.values.addAll(Arrays.asList(values));
        builder.buf.add(name).add("IN (" + String.join(", ", Collections.nCopies(values.length, "?")) + ")");
        return builder. new Condition();
    }

    public static Condition in(String name, TerminalClause terminalClause) {
        var query = terminalClause.build();
        var builder = new FilterBuilder();
        builder.values.addAll(query.values());
        builder.buf.add(name).add("IN (" + query.query() + ")");
        return builder. new Condition();
    }

    public static Condition not_in(String name, Object ...values) {
        var builder = new FilterBuilder();
        builder.values.addAll(Arrays.asList(values));
        builder.buf.add(name).add("NOT IN (" + String.join(", ", Collections.nCopies(values.length, "?")) + ")");
        return builder. new Condition();
    }

    public static Condition not_in(String name, TerminalClause terminalClause) {
        var query = terminalClause.build();
        var builder = new FilterBuilder();
        builder.values.addAll(query.values());
        builder.buf.add(name).add("NOT IN (" + query.query() + ")");
        return builder. new Condition();
    }

    public static Condition and(Condition first, Condition second) {
        var builder = new FilterBuilder();
        var firstFilter = first.build();
        var secondFilter = second.build();
        builder.values.addAll(firstFilter.values);
        builder.values.addAll(secondFilter.values);
        builder.buf.add("(").merge(firstFilter.buf()).add(") AND (").merge(secondFilter.buf()).add(")");
        return builder. new Condition();
    }

    public static Condition lt(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add(name).add("< ?");
        return builder.new Condition();
    }

    public static Condition not_lt(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add("NOT").add(name).add("< ?");
        return builder.new Condition();
    }

    public static Condition gt(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add(name).add("> ?");
        return builder.new Condition();
    }

    public static Condition not_gt(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add("NOT").add(name).add("> ?");
        return builder.new Condition();
    }

    public static Condition or(Condition first, Condition second) {
        var builder = new FilterBuilder();
        var firstFilter = first.build();
        var secondFilter = second.build();
        builder.values.addAll(firstFilter.values());
        builder.values.addAll(secondFilter.values());
        builder.buf.add("(").merge(firstFilter.buf()).add(") OR (").merge(secondFilter.buf()).add(")");
        return builder. new Condition();
    }

    public class Condition {
        public CombinedCondition and() {
            buf.add("AND");
            return new CombinedCondition();
        }
        public CombinedCondition or() {
            buf.add("OR");
            return new CombinedCondition();
        }

        public Condition eq(String name, Object value) {
            values.add(value);
            buf.add(name).add("= ?");
            return new CombinedCondition();
        }

        public Condition not_eq(String name, Object value) {
            values.add(value);
            buf.add("NOT").add(name).add("= ?");
            return this;
        }

        public Condition like(String name, Object value) {
            values.add(value);
            buf.add(name).add("LIKE ?");
            return this;
        }

        public Condition not_like(String name, Object value) {
            values.add(value);
            buf.add(name).add("NOT LIKE ?");
            return this;
        }

        public Condition in(String name, Object ...vals) {
            values.addAll(Arrays.asList(vals));
            buf.add(name).add("IN (" + String.join(", ", Collections.nCopies(vals.length, "?")) + ")");
            return this;
        }
        public Condition in(String name, TerminalClause terminalClause) {
            var query = terminalClause.build();
            values.addAll(query.values());
            buf.add(name).add("IN (" + query.query() + ")");
            return this;
        }

        public Condition not_in(String name, Object ...vals) {
            values.addAll(Arrays.asList(vals));
            buf.add(name).add("NOT IN (" + String.join(", ", Collections.nCopies(vals.length, "?")) + ")");
            return this;
        }
        public Condition not_in(String name, TerminalClause terminalClause) {
            var query = terminalClause.build();
            values.addAll(query.values());
            buf.add(name).add("NOT IN (" + query.query() + ")");
            return this;
        }

        public Condition between(String name, Object first, Object second) {
            values.add(first);
            values.add(second);
            buf.add(name).add("BETWEEN ? AND ?");
            return this;
        }

        public Condition not_between(String name, Object first, Object second) {
            values.add(first);
            values.add(second);
            buf.add(name).add("NOT BETWEEN ? AND ?");
            return this;
        }

        public Condition lt(String name, Object value) {
            values.add(value);
            buf.add(name).add("< ?");
            return this;
        }

        public Condition not_lt(String name, Object value) {
            values.add(value);
            buf.add("NOT").add(name).add("< ?");
            return this;
        }

        public Condition gt(String name, Object value) {
            values.add(value);
            buf.add(name).add("> ?");
            return this;
        }

        public Condition not_gt(String name, Object value) {
            values.add(value);
            buf.add("NOT").add(name).add("> ?");
            return this;
        }

        Filter build() {
            return new Filter(buf, values);
        }
    }

    public class CombinedCondition extends Condition {
        public Condition and(Condition first, Condition second) {
            var firstFilter = first.build();
            var secondFilter = second.build();
            values.addAll(firstFilter.values());
            values.addAll(secondFilter.values());
            buf.add("((").merge(firstFilter.buf()).add(") AND (").merge(secondFilter.buf()).add("))");
            return this;
        }

        public Condition or(Condition first, Condition second) {
            var firstFilter = first.build();
            var secondFilter = second.build();
            values.addAll(firstFilter.values());
            values.addAll(secondFilter.values());
            buf.add("((").merge(firstFilter.buf()).add(") OR (").merge(secondFilter.buf()).add("))");
            return this;
        }
    }
}
