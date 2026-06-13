package querybuilder;

import querybuilder.api.*;
import static querybuilder.api.FilterBuilder.eq;
import static querybuilder.api.FilterBuilder.or;

public class Main {
    public static void main(String[] args) {
        var query = QueryBuilder
            .select("*").from("users")
            .where(
                    or(
                        eq("first_name", "jon").and().eq("last_name", "doe"),
                        eq("first_name", "jane").and().eq("last_name", "doe")
                      ).build())
            .build();
        System.out.println(query);
    }
}
