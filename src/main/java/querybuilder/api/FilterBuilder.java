package querybuilder.api;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class FilterBuilder {
    StringJoiner buf = new StringJoiner(" ");
    List<Object> values = new ArrayList<Object>();
    FilterBuilder() {
    }

    record Filter(StringJoiner buf, List<Object> values) {}

    public static Condition eq(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add(name).add("= ?");
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
        public Condition and() {
            buf.add("AND");
            return this;
        }

        public Condition and(Condition first, Condition second) {
            var firstFilter = first.build();
            var secondFilter = second.build();
            values.addAll(firstFilter.values());
            values.addAll(secondFilter.values());
            buf.add("(").merge(firstFilter.buf()).add(") AND (").merge(secondFilter.buf()).add(")");
            return this;
        }

        public Condition or() {
            buf.add("OR");
            return this;
        }

        public Condition or(Condition first, Condition second) {
            var firstFilter = first.build();
            var secondFilter = second.build();
            values.addAll(firstFilter.values());
            values.addAll(secondFilter.values());
            buf.add("(").merge(firstFilter.buf()).add(") OR (").merge(secondFilter.buf()).add(")");
            return this;
        }

        public Condition eq(String name, Object value) {
            values.add(value);
            buf.add(name).add("= ?");
            return this;
        }

        public Filter build() {
            return new Filter(buf, values);
        }
    }
}
