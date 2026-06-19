package querybuilder.api;

import static querybuilder.api.FilterBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class FilterBuilderTest {

    @Test
    void eqCondition() {
        var expr = FilterBuilder.eq("name", "jon");
        assertEquals(new Equals("name", "jon"), expr);
    }

    @Test
    void notEqCondition() {
        var expr = FilterBuilder.not(FilterBuilder.eq("name", "jon"));
        assertEquals(new Not(new Equals("name", "jon")), expr);
    }

    @Test
    void likeCondition() {
        var f = FilterBuilder.like("name", "j%");
        assertEquals(new Like("name", "j%"), f);
    }

    @Test
    void notLikeCondition() {
        var f = FilterBuilder.not(like("name", "j%"));
        assertEquals(new Not(new Like("name", "j%")), f);
    }

    @Test
    void inCondition() {
        var f = FilterBuilder.in("id", 1, 2, 3);
        assertEquals(new In("id", 1, 2, 3), f);
    }

    @Test
    void notInCondition() {
        var f = FilterBuilder.not(in("id", 1, 2));
        assertEquals(new Not(new In("id", 1, 2)), f);
    }

    @Test
    void betweenCondition() {
        var f = FilterBuilder.between("id", 1, 100);
        assertEquals(new Between("id", 1, 100), f);
    }

    @Test
    void notBetweenCondition() {
        var f = FilterBuilder.not(between("id", 1, 100));
        assertEquals(new Not(new Between("id", 1, 100)), f);
    }

    @Test
    void andCondition() {
        var f = FilterBuilder.and(eq("a", 1), eq("b", 2));
        assertEquals(new And(new Equals("a", 1), new Equals("b", 2)), f);
    }

    @Test
    void orCondition() {
        var f = FilterBuilder.or(eq("a", 1), eq("b", 2));
        assertEquals(new Or(new Equals("a", 1), new Equals("b", 2)), f);
    }

    @Test
    void notCondition() {
        var f = FilterBuilder.not(eq("name", "jon"));
        assertEquals(new Not(new Equals("name", "jon")), f);
    }

    @Test
    void tripleNestedAndOr() {
        var f = and(
                or(eq("a", 1), eq("b", 2)),
                and(eq("c", 3), eq("d", 4)));
        assertEquals(new And(new Or(new Equals("a", 1), new Equals("b", 2)),
                new And(new Equals("c", 3), new Equals("d", 4))), f);
    }

    @Test
    void notWrappingComplexCondition() {
        var f = not(and(
                or(eq("x", 10), eq("y", 20)),
                not(eq("z", 30))));
        assertEquals(
                new Not(
                        new And(
                                new Or(new Equals("x", 10), new Equals("y", 20)),
                                new Not(new Equals("z", 30)))),
                f);
    }

    @Test
    void lessThanCondition() {
        var f = FilterBuilder.lt("price", 100);
        assertEquals(new LessThan("price", 100), f);
    }

    @Test
    void greaterThanCondition() {
        var f = FilterBuilder.gt("price", 50);
        assertEquals(new GreaterThan("price", 50), f);
    }

    @Test
    void notLessThanCondition() {
        var f = FilterBuilder.not(lt("price", 100));
        assertEquals(new Not(new LessThan("price", 100)), f);
    }

    @Test
    void notGreaterThanCondition() {
        var f = FilterBuilder.not(gt("price", 50));
        assertEquals(new Not(new GreaterThan("price", 50)), f);
    }

    @Test
    void emptyIn() {
        var f = FilterBuilder.in("x");
        assertEquals(new In("x"), f);
    }

    @Test
    void betweenWithSameBound() {
        var f = FilterBuilder.between("x", 5, 5);
        assertEquals(new Between("x", 5, 5), f);
    }

    @Test
    void inWithNullValue() {
        var f = FilterBuilder.in("x", (Object) null);
        assertEquals(new In("x", java.util.Collections.singletonList(null)), f);
    }

    @Test
    void isNullCondition() {
        var f = FilterBuilder.isNull("deleted_at");
        assertEquals(new IsNull("deleted_at"), f);
    }

    @Test
    void isNotNullCondition() {
        var f = FilterBuilder.isNotNull("deleted_at");
        assertEquals(new IsNotNull("deleted_at"), f);
    }

    @Test
    void existsCondition() {
        var sub = QueryBuilder.select("*").from("orders").where(FilterBuilder.eq("customer_id", 1));
        var f = FilterBuilder.exists(sub);
        assertEquals(Exists.class, f.getClass());
        assertSame(sub, ((Exists) f).subquery());
    }

    @Test
    void notExistsCondition() {
        var sub = QueryBuilder.select("*").from("orders").where(FilterBuilder.eq("customer_id", 1));
        var f = FilterBuilder.notExists(sub);
        assertEquals(NotExists.class, f.getClass());
        assertSame(sub, ((NotExists) f).subquery());
    }

    @Test
    void andOfIsNullAndNotNull() {
        var f = FilterBuilder.and(isNull("deleted_at"), isNotNull("created_at"));
        assertEquals(new And(new IsNull("deleted_at"), new IsNotNull("created_at")), f);
    }
}
