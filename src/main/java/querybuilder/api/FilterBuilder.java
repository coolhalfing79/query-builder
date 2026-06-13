package querybuilder.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import querybuilder.api.QueryBuilder.TerminalClause;

/**
 * Factory for building SQL filter conditions.
 *
 * <p>All static methods return a {@link Condition} that can be passed to
 * {@link QueryBuilder.Joinable#where(Condition) where()},
 * {@link QueryBuilder.Join#on(Condition) on()}, or
 * {@link QueryBuilder.GroupBy#having(Condition) having()}.
 *
 * <p>Conditions are <b>single-use</b>. Once {@code build()} is called (by the
 * clause that receives the condition), the internal buffer and value list are
 * consumed and must not be reused.
 */
public class FilterBuilder {
    StringJoiner buf = new StringJoiner(" ");
    List<Object> values = new ArrayList<Object>();

    FilterBuilder() {
    }

    record Filter(StringJoiner buf, List<Object> values) {}

    /**
     * Negates a condition: {@code NOT (condition)}.
     */
    public static Condition not(Condition condition) {
        var builder = new FilterBuilder();
        var filter = condition.build();
        builder.values.addAll(filter.values());
        builder.buf.add("NOT").add("(").merge(filter.buf()).add(")");
        return builder. new Condition();
    }

    /**
     * Column equality: {@code name = ?}.
     * @param name the column name
     * @param value the parameterized value (replaced with {@code ?})
     */
    public static Condition eq(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add(name).add("= ?");
        return builder. new Condition();
    }

    /**
     * Column inequality: {@code NOT name = ?}.
     * @param name the column name
     * @param value the parameterized value (replaced with {@code ?})
     */
    public static Condition not_eq(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add("NOT").add(name).add("= ?");
        return builder. new Condition();
    }

    /**
     * LIKE pattern match: {@code name LIKE ?}.
     * @param name the column name
     * @param value the pattern (use {@code %} and {@code _} wildcards)
     */
    public static Condition like(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add(name).add("LIKE ?");
        return builder. new Condition();
    }

    /**
     * Negated LIKE: {@code name NOT LIKE ?}.
     * @param name the column name
     * @param value the pattern
     */
    public static Condition not_like(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add(name).add("NOT LIKE ?");
        return builder. new Condition();
    }

    /**
     * Range: {@code name BETWEEN ? AND ?}.
     * @param name the column name
     * @param first lower bound (inclusive)
     * @param second upper bound (inclusive)
     */
    public static Condition between(String name, Object first, Object second) {
        var builder = new FilterBuilder();
        builder.values.add(first);
        builder.values.add(second);
        builder.buf.add(name).add("BETWEEN ? AND ?");
        return builder. new Condition();
    }

    /**
     * Negated range: {@code name NOT BETWEEN ? AND ?}.
     * @param name the column name
     * @param first lower bound (inclusive)
     * @param second upper bound (inclusive)
     */
    public static Condition not_between(String name, Object first, Object second) {
        var builder = new FilterBuilder();
        builder.values.add(first);
        builder.values.add(second);
        builder.buf.add(name).add("NOT BETWEEN ? AND ?");
        return builder. new Condition();
    }

    /**
     * Set membership with inline values: {@code name IN (?, ?, ...)}.
     * @param name the column name
     * @param values the candidate values
     */
    public static Condition in(String name, Object ...values) {
        var builder = new FilterBuilder();
        builder.values.addAll(Arrays.asList(values));
        builder.buf.add(name).add("IN (" + String.join(", ", Collections.nCopies(values.length, "?")) + ")");
        return builder. new Condition();
    }

    /**
     * Set membership with a subquery: {@code name IN (subquery)}.
     * <p>The subquery is provided as a {@link TerminalClause} (e.g. the result
     * of {@code select(...).from(...)}). It is built immediately and its values
     * are absorbed into the condition.
     */
    public static Condition in(String name, TerminalClause terminalClause) {
        var query = terminalClause.build();
        var builder = new FilterBuilder();
        builder.values.addAll(query.values());
        builder.buf.add(name).add("IN (" + query.query() + ")");
        return builder. new Condition();
    }

    /**
     * Negated set membership with inline values: {@code name NOT IN (?, ?, ...)}.
     * @param name the column name
     * @param values the candidate values
     */
    public static Condition not_in(String name, Object ...values) {
        var builder = new FilterBuilder();
        builder.values.addAll(Arrays.asList(values));
        builder.buf.add(name).add("NOT IN (" + String.join(", ", Collections.nCopies(values.length, "?")) + ")");
        return builder. new Condition();
    }

    /**
     * Negated set membership with a subquery: {@code name NOT IN (subquery)}.
     * @param name the column name
     * @param terminalClause the subquery
     */
    public static Condition not_in(String name, TerminalClause terminalClause) {
        var query = terminalClause.build();
        var builder = new FilterBuilder();
        builder.values.addAll(query.values());
        builder.buf.add(name).add("NOT IN (" + query.query() + ")");
        return builder. new Condition();
    }

    /**
     * Column-to-column equality (no parameterization): {@code column1 = column2}.
     * <p>Use this in JOIN ON clauses to reference columns from different tables.
     * Unlike {@link #eq(String, Object)}, this does <b>not</b> add a {@code ?}
     * placeholder.
     */
    public static Condition colEq(String column1, String column2) {
        var builder = new FilterBuilder();
        builder.buf.add(column1).add("=").add(column2);
        return builder. new Condition();
    }

    /**
     * {@code name IS NULL}.
     * @param name the column name
     */
    public static Condition isNull(String name) {
        var builder = new FilterBuilder();
        builder.buf.add(name).add("IS NULL");
        return builder. new Condition();
    }

    /**
     * {@code name IS NOT NULL}.
     * @param name the column name
     */
    public static Condition isNotNull(String name) {
        var builder = new FilterBuilder();
        builder.buf.add(name).add("IS NOT NULL");
        return builder. new Condition();
    }

    /**
     * {@code EXISTS (subquery)}.
     * @param subquery the subquery to test for row existence
     */
    public static Condition exists(TerminalClause subquery) {
        var query = subquery.build();
        var builder = new FilterBuilder();
        builder.values.addAll(query.values());
        builder.buf.add("EXISTS (" + query.query() + ")");
        return builder. new Condition();
    }

    /**
     * {@code NOT EXISTS (subquery)}.
     * @param subquery the subquery to test for row absence
     */
    public static Condition notExists(TerminalClause subquery) {
        var query = subquery.build();
        var builder = new FilterBuilder();
        builder.values.addAll(query.values());
        builder.buf.add("NOT EXISTS (" + query.query() + ")");
        return builder. new Condition();
    }

    /**
     * Conjunction of two conditions: {@code (first) AND (second)}.
     */
    public static Condition and(Condition first, Condition second) {
        var builder = new FilterBuilder();
        var firstFilter = first.build();
        var secondFilter = second.build();
        builder.values.addAll(firstFilter.values);
        builder.values.addAll(secondFilter.values);
        builder.buf.add("(").merge(firstFilter.buf()).add(") AND (").merge(secondFilter.buf()).add(")");
        return builder. new Condition();
    }

    /**
     * Less-than: {@code name < ?}.
     * @param name the column name
     * @param value the upper bound (exclusive)
     */
    public static Condition lt(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add(name).add("< ?");
        return builder.new Condition();
    }

    /**
     * Negated less-than: {@code NOT name < ?}.
     * @param name the column name
     * @param value the upper bound
     */
    public static Condition not_lt(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add("NOT").add(name).add("< ?");
        return builder.new Condition();
    }

    /**
     * Greater-than: {@code name > ?}.
     * @param name the column name
     * @param value the lower bound (exclusive)
     */
    public static Condition gt(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add(name).add("> ?");
        return builder.new Condition();
    }

    /**
     * Negated greater-than: {@code NOT name > ?}.
     * @param name the column name
     * @param value the lower bound
     */
    public static Condition not_gt(String name, Object value) {
        var builder = new FilterBuilder();
        builder.values.add(value);
        builder.buf.add("NOT").add(name).add("> ?");
        return builder.new Condition();
    }

    /**
     * Disjunction of two conditions: {@code (first) OR (second)}.
     */
    public static Condition or(Condition first, Condition second) {
        var builder = new FilterBuilder();
        var firstFilter = first.build();
        var secondFilter = second.build();
        builder.values.addAll(firstFilter.values());
        builder.values.addAll(secondFilter.values());
        builder.buf.add("(").merge(firstFilter.buf()).add(") OR (").merge(secondFilter.buf()).add(")");
        return builder. new Condition();
    }

    /**
     * A single filter condition or predicate.
     *
     * <p>Instances of this class hold an internal buffer and value list that
     * are shared with the enclosing {@link FilterBuilder}. Instance methods on
     * {@code Condition} allow inline chaining of additional predicates.
     *
     * <p><b>Quirk:</b> Some instance methods return a {@link CombinedCondition}
     * instead of a plain {@code Condition}. This allows the caller to chain
     * the combiners {@link CombinedCondition#and(Condition, Condition) and(c1,c2)}
     * and {@link CombinedCondition#or(Condition, Condition) or(c1,c2)} directly.
     * The methods that promote to {@code CombinedCondition} are:
     * {@link #eq(String, Object)}, {@link #exists(TerminalClause)},
     * {@link #notExists(TerminalClause)}, {@link #and()}, and {@link #or()}.
     *
     * <p><b>Single-use:</b> A {@code Condition} must not be reused after it has
     * been passed to a consuming method (e.g. {@code where()}, {@code on()},
     * {@code having()}, or a static combiner like {@link #and(Condition, Condition) and()}).
     */
    public class Condition {
        /**
         * Starts an AND chain: {@code ... AND }.
         * Returns a {@link CombinedCondition} that accepts the two-argument
         * {@link CombinedCondition#and(Condition, Condition) and(c1,c2)} combiner.
         */
        public CombinedCondition and() {
            buf.add("AND");
            return new CombinedCondition();
        }
        /**
         * Starts an OR chain: {@code ... OR }.
         * Returns a {@link CombinedCondition} that accepts the two-argument
         * {@link CombinedCondition#or(Condition, Condition) or(c1,c2)} combiner.
         */
        public CombinedCondition or() {
            buf.add("OR");
            return new CombinedCondition();
        }

        /**
         * Appends {@code name = ?} to the current predicate chain.
         * @return a {@link CombinedCondition} to allow further chaining via
         *         the two-argument combiners
         */
        public Condition eq(String name, Object value) {
            values.add(value);
            buf.add(name).add("= ?");
            return new CombinedCondition();
        }

        /**
         * Appends {@code NOT name = ?} to the current predicate chain.
         */
        public Condition not_eq(String name, Object value) {
            values.add(value);
            buf.add("NOT").add(name).add("= ?");
            return this;
        }

        /**
         * Appends column-to-column equality ({@code col1 = col2}) without
         * parameterization. Useful for JOIN ON clauses.
         */
        public Condition colEq(String column1, String column2) {
            buf.add(column1).add("=").add(column2);
            return this;
        }

        /**
         * Appends {@code name IS NULL} to the current predicate chain.
         */
        public Condition isNull(String name) {
            buf.add(name).add("IS NULL");
            return this;
        }

        /**
         * Appends {@code name IS NOT NULL} to the current predicate chain.
         */
        public Condition isNotNull(String name) {
            buf.add(name).add("IS NOT NULL");
            return this;
        }

        /**
         * Appends {@code EXISTS (subquery)} to the current predicate chain.
         * @return a {@link CombinedCondition} to allow further chaining via
         *         the two-argument combiners
         */
        public Condition exists(TerminalClause subquery) {
            var query = subquery.build();
            values.addAll(query.values());
            buf.add("EXISTS (" + query.query() + ")");
            return new CombinedCondition();
        }

        /**
         * Appends {@code NOT EXISTS (subquery)} to the current predicate chain.
         * @return a {@link CombinedCondition} to allow further chaining via
         *         the two-argument combiners
         */
        public Condition notExists(TerminalClause subquery) {
            var query = subquery.build();
            values.addAll(query.values());
            buf.add("NOT EXISTS (" + query.query() + ")");
            return new CombinedCondition();
        }

        /**
         * Appends {@code name LIKE ?} to the current predicate chain.
         */
        public Condition like(String name, Object value) {
            values.add(value);
            buf.add(name).add("LIKE ?");
            return this;
        }

        /**
         * Appends {@code name NOT LIKE ?} to the current predicate chain.
         */
        public Condition not_like(String name, Object value) {
            values.add(value);
            buf.add(name).add("NOT LIKE ?");
            return this;
        }

        /**
         * Appends {@code name IN (?, ?, ...)} with inline values.
         */
        public Condition in(String name, Object ...vals) {
            values.addAll(Arrays.asList(vals));
            buf.add(name).add("IN (" + String.join(", ", Collections.nCopies(vals.length, "?")) + ")");
            return this;
        }
        /**
         * Appends {@code name IN (subquery)}.
         */
        public Condition in(String name, TerminalClause terminalClause) {
            var query = terminalClause.build();
            values.addAll(query.values());
            buf.add(name).add("IN (" + query.query() + ")");
            return this;
        }

        /**
         * Appends {@code name NOT IN (?, ?, ...)} with inline values.
         */
        public Condition not_in(String name, Object ...vals) {
            values.addAll(Arrays.asList(vals));
            buf.add(name).add("NOT IN (" + String.join(", ", Collections.nCopies(vals.length, "?")) + ")");
            return this;
        }
        /**
         * Appends {@code name NOT IN (subquery)}.
         */
        public Condition not_in(String name, TerminalClause terminalClause) {
            var query = terminalClause.build();
            values.addAll(query.values());
            buf.add(name).add("NOT IN (" + query.query() + ")");
            return this;
        }

        /**
         * Appends {@code name BETWEEN ? AND ?} with inclusive bounds.
         */
        public Condition between(String name, Object first, Object second) {
            values.add(first);
            values.add(second);
            buf.add(name).add("BETWEEN ? AND ?");
            return this;
        }

        /**
         * Appends {@code name NOT BETWEEN ? AND ?} with inclusive bounds.
         */
        public Condition not_between(String name, Object first, Object second) {
            values.add(first);
            values.add(second);
            buf.add(name).add("NOT BETWEEN ? AND ?");
            return this;
        }

        /**
         * Appends {@code name < ?} to the current predicate chain.
         */
        public Condition lt(String name, Object value) {
            values.add(value);
            buf.add(name).add("< ?");
            return this;
        }

        /**
         * Appends {@code NOT name < ?} to the current predicate chain.
         */
        public Condition not_lt(String name, Object value) {
            values.add(value);
            buf.add("NOT").add(name).add("< ?");
            return this;
        }

        /**
         * Appends {@code name > ?} to the current predicate chain.
         */
        public Condition gt(String name, Object value) {
            values.add(value);
            buf.add(name).add("> ?");
            return this;
        }

        /**
         * Appends {@code NOT name > ?} to the current predicate chain.
         */
        public Condition not_gt(String name, Object value) {
            values.add(value);
            buf.add("NOT").add(name).add("> ?");
            return this;
        }

        Filter build() {
            return new Filter(buf, values);
        }
    }

    /**
     * A {@code Condition} that additionally exposes the two-argument
     * combiner methods {@link #and(Condition, Condition)} and
     * {@link #or(Condition, Condition)} for inline conjunction/disjunction.
     *
     * <p>These methods produce doubly-wrapped parenthesized groups:
     * {@code ((a) AND (b))}, which is correct when nesting inside a larger
     * chain but may produce unnecessary parentheses when used alone.
     */
    public class CombinedCondition extends Condition {
        /**
         * Inline conjunction with two sub-conditions:
         * {@code ((first) AND (second))}.
         */
        public Condition and(Condition first, Condition second) {
            var firstFilter = first.build();
            var secondFilter = second.build();
            values.addAll(firstFilter.values());
            values.addAll(secondFilter.values());
            buf.add("((").merge(firstFilter.buf()).add(") AND (").merge(secondFilter.buf()).add("))");
            return this;
        }

        /**
         * Inline disjunction with two sub-conditions:
         * {@code ((first) OR (second))}.
         */
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
