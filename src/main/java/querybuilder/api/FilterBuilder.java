package querybuilder.api;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Factory for building SQL filter conditions.
 *
 * <p>
 * All static methods return a {@link Operation} that can be passed to
 * {@link QueryBuilder.Joinable#where(Operation) where()},
 * {@link QueryBuilder.Join#on(Operation) on()}, or
 * {@link QueryBuilder.GroupBy#having(Operation) having()}.
 *
 * <p>
 * Conditions are <b>single-use</b>. Once {@code build()} is called (by the
 * clause that receives the condition), the internal buffer and value list are
 * consumed and must not be reused.
 */
public class FilterBuilder {

    public interface BuilderStrategy<T> {
        T build(IExpression expr);
    }

    public sealed interface IExpression permits And, Or, Equals, Not, Like, In, Between, GreaterThan, LessThan, IsNull,
            IsNotNull, Exists, NotExists, ColEq, InSubquery {
        default <T> T build(BuilderStrategy<T> strat) {
            return strat.build(this);
        }
    }

    public record And(List<IExpression> expressions) implements IExpression {
        And(IExpression... expressions) {
            this(List.of(expressions));
        }
    }

    public record Or(List<IExpression> expressions) implements IExpression {
        Or(IExpression... expressions) {
            this(List.of(expressions));
        }
    }

    public record In(String key, List<Object> values) implements IExpression {
        In(String key, Object... values) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(values);
            this(key, Arrays.asList(values));
        }
    }

    public record Between(String key, Object first, Object second) implements IExpression {
        public Between {
            Objects.requireNonNull(key);
            Objects.requireNonNull(first);
            Objects.requireNonNull(second);
        }
    }

    public record Equals(String key, Object value) implements IExpression {
        public Equals {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
        }
    }

    public record Like(String key, Object value) implements IExpression {
        public Like {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
        }
    }

    public record GreaterThan(String key, Object value) implements IExpression {
        public GreaterThan {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
        }
    }

    public record LessThan(String key, Object value) implements IExpression {
        public LessThan {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
        }
    }

    public record Not(IExpression expression) implements IExpression {
    }

    public record IsNull(String key) implements IExpression {
        public IsNull {
            Objects.requireNonNull(key);
        }
    }

    public record IsNotNull(String key) implements IExpression {
        public IsNotNull {
            Objects.requireNonNull(key);
        }
    }

    public record Exists(QueryBuilder.TerminalClause subquery) implements IExpression {
    }

    public record NotExists(QueryBuilder.TerminalClause subquery) implements IExpression {
    }

    public record ColEq(String left, String right) implements IExpression {
        public ColEq {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
        }
    }

    public record InSubquery(String key, QueryBuilder.TerminalClause subquery) implements IExpression {
    }

    public static And and(IExpression... expressions) {
        return new And(Arrays.asList(expressions));
    }

    public static Or or(IExpression... expressions) {
        return new Or(Arrays.asList(expressions));
    }

    public static Not not(IExpression expression) {
        return new Not(expression);
    }

    public static Equals eq(String key, Object value) {
        return new Equals(key, value);
    }

    public static Like like(String key, Object value) {
        return new Like(key, value);
    }

    public static GreaterThan gt(String key, Object value) {
        return new GreaterThan(key, value);
    }

    public static LessThan lt(String key, Object value) {
        return new LessThan(key, value);
    }

    public static In in(String key, Object... values) {
        return new In(key, values);
    }

    public static Between between(String key, Object first, Object second) {
        return new Between(key, first, second);
    }

    public static InSubquery in(String key, QueryBuilder.TerminalClause subquery) {
        return new InSubquery(key, subquery);
    }

    public static IsNull isNull(String key) {
        return new IsNull(key);
    }

    public static IsNotNull isNotNull(String key) {
        return new IsNotNull(key);
    }

    public static Exists exists(QueryBuilder.TerminalClause subquery) {
        return new Exists(subquery);
    }

    public static NotExists notExists(QueryBuilder.TerminalClause subquery) {
        return new NotExists(subquery);
    }

    public static ColEq colEq(String left, String right) {
        return new ColEq(left, right);
    }
}
