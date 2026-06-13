package querybuilder.api;

import static querybuilder.api.FilterBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class FilterBuilderTest {

    @Test
    void eqCondition() {
        var f = FilterBuilder.eq("name", "jon").build();
        assertEquals("name = ?", f.buf().toString());
        assertEquals(List.of("jon"), f.values());
    }

    @Test
    void notEqCondition() {
        var f = FilterBuilder.not_eq("name", "jon").build();
        assertEquals("NOT name = ?", f.buf().toString());
        assertEquals(List.of("jon"), f.values());
    }

    @Test
    void likeCondition() {
        var f = FilterBuilder.like("name", "j%").build();
        assertEquals("name LIKE ?", f.buf().toString());
        assertEquals(List.of("j%"), f.values());
    }

    @Test
    void notLikeCondition() {
        var f = FilterBuilder.not_like("name", "j%").build();
        assertEquals("name NOT LIKE ?", f.buf().toString());
        assertEquals(List.of("j%"), f.values());
    }

    @Test
    void inCondition() {
        var f = FilterBuilder.in("id", 1, 2, 3).build();
        assertEquals("id IN (?, ?, ?)", f.buf().toString());
        assertEquals(List.of(1, 2, 3), f.values());
    }

    @Test
    void notInCondition() {
        var f = FilterBuilder.not_in("id", 1, 2).build();
        assertEquals("id NOT IN (?, ?)", f.buf().toString());
        assertEquals(List.of(1, 2), f.values());
    }

    @Test
    void betweenCondition() {
        var f = FilterBuilder.between("id", 1, 100).build();
        assertEquals("id BETWEEN ? AND ?", f.buf().toString());
        assertEquals(List.of(1, 100), f.values());
    }

    @Test
    void notBetweenCondition() {
        var f = FilterBuilder.not_between("id", 1, 100).build();
        assertEquals("id NOT BETWEEN ? AND ?", f.buf().toString());
        assertEquals(List.of(1, 100), f.values());
    }

    @Test
    void andCondition() {
        var f = FilterBuilder.and(eq("a", 1), eq("b", 2)).build();
        assertEquals("( a = ? ) AND ( b = ? )", f.buf().toString());
        assertEquals(List.of(1, 2), f.values());
    }

    @Test
    void orCondition() {
        var f = FilterBuilder.or(eq("a", 1), eq("b", 2)).build();
        assertEquals("( a = ? ) OR ( b = ? )", f.buf().toString());
        assertEquals(List.of(1, 2), f.values());
    }

    @Test
    void notCondition() {
        var f = FilterBuilder.not(eq("name", "jon")).build();
        assertEquals("NOT ( name = ? )", f.buf().toString());
        assertEquals(List.of("jon"), f.values());
    }

    @Test
    void chainedAnd() {
        var f = eq("a", 1).and().eq("b", 2).build();
        assertEquals("a = ? AND b = ?", f.buf().toString());
        assertEquals(List.of(1, 2), f.values());
    }

    @Test
    void chainedOr() {
        var f = eq("a", 1).or().eq("b", 2).build();
        assertEquals("a = ? OR b = ?", f.buf().toString());
        assertEquals(List.of(1, 2), f.values());
    }

    @Test
    void chainedAndCombiner() {
        var f = eq("a", 1).and().and(eq("b", 2), eq("c", 3)).build();
        assertEquals("a = ? AND (( b = ? ) AND ( c = ? ))", f.buf().toString());
        assertEquals(List.of(1, 2, 3), f.values());
    }

    @Test
    void chainedOrCombiner() {
        var f = eq("a", 1).or().or(eq("b", 2), eq("c", 3)).build();
        assertEquals("a = ? OR (( b = ? ) OR ( c = ? ))", f.buf().toString());
        assertEquals(List.of(1, 2, 3), f.values());
    }

    @Test
    void instanceEqCondition() {
        var f = new FilterBuilder().new CombinedCondition().eq("x", 10).build();
        assertEquals("x = ?", f.buf().toString());
        assertEquals(List.of(10), f.values());
    }

    @Test
    void instanceNotEqCondition() {
        var f = new FilterBuilder().new CombinedCondition().not_eq("x", 10).build();
        assertEquals("NOT x = ?", f.buf().toString());
        assertEquals(List.of(10), f.values());
    }

    @Test
    void instanceLikeCondition() {
        var f = new FilterBuilder().new CombinedCondition().like("x", "%v").build();
        assertEquals("x LIKE ?", f.buf().toString());
        assertEquals(List.of("%v"), f.values());
    }

    @Test
    void instanceNotLikeCondition() {
        var f = new FilterBuilder().new CombinedCondition().not_like("x", "%v").build();
        assertEquals("x NOT LIKE ?", f.buf().toString());
        assertEquals(List.of("%v"), f.values());
    }

    @Test
    void instanceInCondition() {
        var f = new FilterBuilder().new CombinedCondition().in("x", 1, 2).build();
        assertEquals("x IN (?, ?)", f.buf().toString());
        assertEquals(List.of(1, 2), f.values());
    }

    @Test
    void instanceNotInCondition() {
        var f = new FilterBuilder().new CombinedCondition().not_in("x", 1, 2).build();
        assertEquals("x NOT IN (?, ?)", f.buf().toString());
        assertEquals(List.of(1, 2), f.values());
    }

    @Test
    void instanceBetweenCondition() {
        var f = new FilterBuilder().new CombinedCondition().between("x", 1, 10).build();
        assertEquals("x BETWEEN ? AND ?", f.buf().toString());
        assertEquals(List.of(1, 10), f.values());
    }

    @Test
    void instanceNotBetweenCondition() {
        var f = new FilterBuilder().new CombinedCondition().not_between("x", 1, 10).build();
        assertEquals("x NOT BETWEEN ? AND ?", f.buf().toString());
        assertEquals(List.of(1, 10), f.values());
    }

    @Test
    void instanceLt() {
        var f = new FilterBuilder().new CombinedCondition().lt("x", 5).build();
        assertEquals("x < ?", f.buf().toString());
        assertEquals(List.of(5), f.values());
    }

    @Test
    void instanceNotLt() {
        var f = new FilterBuilder().new CombinedCondition().not_lt("x", 5).build();
        assertEquals("NOT x < ?", f.buf().toString());
        assertEquals(List.of(5), f.values());
    }

    @Test
    void instanceGt() {
        var f = new FilterBuilder().new CombinedCondition().gt("x", 5).build();
        assertEquals("x > ?", f.buf().toString());
        assertEquals(List.of(5), f.values());
    }

    @Test
    void instanceNotGt() {
        var f = new FilterBuilder().new CombinedCondition().not_gt("x", 5).build();
        assertEquals("NOT x > ?", f.buf().toString());
        assertEquals(List.of(5), f.values());
    }

    // ---------------------------------------------------------------------------
    // Complicated / nested filter combinations
    // ---------------------------------------------------------------------------

    @Test
    void tripleNestedAndOr() {
        var f = and(
                or(eq("a", 1), eq("b", 2)),
                and(eq("c", 3), eq("d", 4)))
                .build();
        assertEquals("( ( a = ? ) OR ( b = ? ) ) AND ( ( c = ? ) AND ( d = ? ) )", f.buf().toString());
        assertEquals(List.of(1, 2, 3, 4), f.values());
    }

    @Test
    void notWrappingComplexCondition() {
        var f = not(and(
                or(eq("x", 10), eq("y", 20)),
                not(eq("z", 30))))
                .build();
        assertEquals("NOT ( ( ( x = ? ) OR ( y = ? ) ) AND ( NOT ( z = ? ) ) )", f.buf().toString());
        assertEquals(List.of(10, 20, 30), f.values());
    }

    @Test
    void chainedCombinersOnlyNoLeadingOp() {
        var f = eq("x", 1).and().and(eq("y", 2), eq("z", 3)).and().and(eq("a", 4), eq("b", 5)).build();
        assertEquals("x = ? AND (( y = ? ) AND ( z = ? )) AND (( a = ? ) AND ( b = ? ))", f.buf().toString());
        assertEquals(List.of(1, 2, 3, 4, 5), f.values());
    }

    @Test
    void chainedAfterCombiner() {
        var f = eq("x", 1).and().and(eq("y", 2), eq("z", 3)).and().eq("w", 4).build();
        assertEquals("x = ? AND (( y = ? ) AND ( z = ? )) AND w = ?", f.buf().toString());
        assertEquals(List.of(1, 2, 3, 4), f.values());
    }

    @Test
    void chainedInstanceConditionsAndCombiners() {
        var f = eq("a", 1)
                .and().eq("b", 2)
                .and().and(eq("c", 3), eq("d", 4))
                .or().eq("e", 5)
                .or().or(eq("f", 6), eq("g", 7))
                .build();
        assertEquals("a = ? AND b = ? AND (( c = ? ) AND ( d = ? )) OR e = ? OR (( f = ? ) OR ( g = ? ))", f.buf().toString());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7), f.values());
    }

    // ---------------------------------------------------------------------------
    // Edge cases
    // ---------------------------------------------------------------------------

    @Test
    void emptyIn() {
        var f = FilterBuilder.in("x").build();
        assertEquals("x IN ()", f.buf().toString());
        assertTrue(f.values().isEmpty());
    }

    @Test
    void instanceEmptyIn() {
        var f = new FilterBuilder().new CombinedCondition().in("y").build();
        assertEquals("y IN ()", f.buf().toString());
        assertTrue(f.values().isEmpty());
    }

    @Test
    void betweenWithSameBound() {
        var f = FilterBuilder.between("x", 5, 5).build();
        assertEquals("x BETWEEN ? AND ?", f.buf().toString());
        assertEquals(List.of(5, 5), f.values());
    }

    @Test
    void staticLt() {
        var f = FilterBuilder.lt("price", 100).build();
        assertEquals("price < ?", f.buf().toString());
        assertEquals(List.of(100), f.values());
    }

    @Test
    void staticGt() {
        var f = FilterBuilder.gt("price", 50).build();
        assertEquals("price > ?", f.buf().toString());
        assertEquals(List.of(50), f.values());
    }

    @Test
    void staticNotLt() {
        var f = FilterBuilder.not_lt("price", 100).build();
        assertEquals("NOT price < ?", f.buf().toString());
        assertEquals(List.of(100), f.values());
    }

    @Test
    void staticNotGt() {
        var f = FilterBuilder.not_gt("price", 50).build();
        assertEquals("NOT price > ?", f.buf().toString());
        assertEquals(List.of(50), f.values());
    }

    // ---------------------------------------------------------------------------
    // Chained lt/gt patterns — combiners still callable after lt/gt (bug exposed)
    // ---------------------------------------------------------------------------

    @Test
    void chainedLtAndCombiner() {
        var f = lt("a", 1).and().and(lt("b", 2), lt("c", 3)).build();
        assertEquals("a < ? AND (( b < ? ) AND ( c < ? ))", f.buf().toString());
        assertEquals(List.of(1, 2, 3), f.values());
    }

    @Test
    void chainedGtAndCombiner() {
        var f = gt("a", 1).and().and(gt("b", 2), gt("c", 3)).build();
        assertEquals("a > ? AND (( b > ? ) AND ( c > ? ))", f.buf().toString());
        assertEquals(List.of(1, 2, 3), f.values());
    }

    @Test
    void chainedLtOrCombiner() {
        var f = lt("a", 1).and().or(lt("b", 2), lt("c", 3)).build();
        assertEquals("a < ? AND (( b < ? ) OR ( c < ? ))", f.buf().toString());
        assertEquals(List.of(1, 2, 3), f.values());
    }

    @Test
    void chainedMixedEqCombiner() {
        var f = eq("a", 1).and().and(lt("b", 2), gt("c", 3)).build();
        assertEquals("a = ? AND (( b < ? ) AND ( c > ? ))", f.buf().toString());
        assertEquals(List.of(1, 2, 3), f.values());
    }

    @Test
    void chainedMixedLikeCombiner() {
        var f = like("a", "%x").and().and(like("b", "%y"), like("c", "%z")).build();
        assertEquals("a LIKE ? AND (( b LIKE ? ) AND ( c LIKE ? ))", f.buf().toString());
        assertEquals(List.of("%x", "%y", "%z"), f.values());
    }

    // ---------------------------------------------------------------------------
    // Valid lt/gt chaining patterns (no combiner)
    // ---------------------------------------------------------------------------

    @Test
    void chainedLtAndGt() {
        var f = lt("a", 1).and().gt("b", 2).build();
        assertEquals("a < ? AND b > ?", f.buf().toString());
        assertEquals(List.of(1, 2), f.values());
    }

    @Test
    void chainedGtOrLt() {
        var f = gt("a", 1).or().lt("b", 2).build();
        assertEquals("a > ? OR b < ?", f.buf().toString());
        assertEquals(List.of(1, 2), f.values());
    }

    @Test
    void chainedNotLtAndNotGt() {
        var f = not_lt("a", 1).and().not_gt("b", 2).build();
        assertEquals("NOT a < ? AND NOT b > ?", f.buf().toString());
        assertEquals(List.of(1, 2), f.values());
    }

    @Test
    void chainedEqAndLt() {
        var f = eq("a", 1).and().lt("b", 2).build();
        assertEquals("a = ? AND b < ?", f.buf().toString());
        assertEquals(List.of(1, 2), f.values());
    }

    @Test
    void chainedLtAndEq() {
        var f = lt("a", 1).and().eq("b", 2).build();
        assertEquals("a < ? AND b = ?", f.buf().toString());
        assertEquals(List.of(1, 2), f.values());
    }

    @Test
    void chainedLtBetween() {
        var f = lt("a", 1).and().between("b", 2, 3).build();
        assertEquals("a < ? AND b BETWEEN ? AND ?", f.buf().toString());
        assertEquals(List.of(1, 2, 3), f.values());
    }

    // ---------------------------------------------------------------------------
    // Static combiners with lt/gt
    // ---------------------------------------------------------------------------

    @Test
    void andOfLtAndGt() {
        var f = and(lt("a", 1), gt("b", 2)).build();
        assertEquals("( a < ? ) AND ( b > ? )", f.buf().toString());
        assertEquals(List.of(1, 2), f.values());
    }

    @Test
    void orOfNotLtAndNotGt() {
        var f = or(not_lt("a", 1), not_gt("b", 2)).build();
        assertEquals("( NOT a < ? ) OR ( NOT b > ? )", f.buf().toString());
        assertEquals(List.of(1, 2), f.values());
    }

    @Test
    void notLt() {
        var f = not(lt("a", 1)).build();
        assertEquals("NOT ( a < ? )", f.buf().toString());
        assertEquals(List.of(1), f.values());
    }

    @Test
    void notGt() {
        var f = not(gt("a", 1)).build();
        assertEquals("NOT ( a > ? )", f.buf().toString());
        assertEquals(List.of(1), f.values());
    }

    // ---------------------------------------------------------------------------
    // Type-boundary: all static methods return AbstractCondition, so combiners
    // require .and().and(c1,c2) / .or().or(c1,c2) bridge pattern.
    // ---------------------------------------------------------------------------

    @Test
    void eqCombinersViaAndBridge() {
        var f = eq("a", 1).and().and(eq("b", 2), eq("c", 3)).build();
        assertEquals("a = ? AND (( b = ? ) AND ( c = ? ))", f.buf().toString());
        assertEquals(List.of(1, 2, 3), f.values());
    }

    @Test
    void inWithNullValue() {
        var f = FilterBuilder.in("x", (Object) null).build();
        assertEquals("x IN (?)", f.buf().toString());
        assertEquals(java.util.Collections.singletonList(null), f.values());
    }

    // ---------------------------------------------------------------------------
    // TODO: Missing filter features (not yet supported)
    // ---------------------------------------------------------------------------

    // TODO: IS NULL / IS NOT NULL
    // Example: isNull("deleted_at") → "deleted_at IS NULL"
    //          isNotNull("deleted_at") → "deleted_at IS NOT NULL"

    // TODO: EXISTS / NOT EXISTS
    // Example: exists(select("*").from("orders").where(eq("customer_id", 1))) → "EXISTS (SELECT * FROM orders WHERE customer_id = ?)"


}
