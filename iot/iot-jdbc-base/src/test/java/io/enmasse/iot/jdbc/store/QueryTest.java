/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.enmasse.iot.jdbc.store.Statement.ExpandedStatement;

public class QueryTest {

    @Test
    public void testNoParams() {
        final ExpandedStatement expanded = Statement.statement("select 1").expand(Collections.emptyMap());
        assertEquals("select 1", expanded.getSql());
        assertArrayEquals(new Object[] {}, expanded.getParameters());
    }

    @Test
    public void testSimpleParams() {
        final Map<String, Object> params = new HashMap<>();
        params.put("foo", 1);
        params.put("bar", "baz");

        final ExpandedStatement expanded = Statement.statement("select * from table where foo=:foo and bar=:bar").expand(params);
        assertEquals("select * from table where foo=? and bar=?", expanded.getSql());
        assertArrayEquals(new Object[] {1, "baz"}, expanded.getParameters());
    }

    @Test
    public void testDoubleParams() {
        final Map<String, Object> params = new HashMap<>();
        params.put("foo", 1);
        params.put("bar", "baz");

        final ExpandedStatement expanded = Statement.statement("select * from table where foo=:foo and foo2=:foo and bar=:bar and barfoo=:foo and 1=1").expand(params);
        assertEquals("select * from table where foo=? and foo2=? and bar=? and barfoo=? and 1=1", expanded.getSql());
        assertArrayEquals(new Object[] {1, 1, "baz", 1}, expanded.getParameters());
    }

    @Test
    public void testExtraParams() {
        final ExpandedStatement expanded = Statement.statement("select * from table where foo=:foo and bar=:bar")
                .expand(params -> {
                    params.put("foobarbaz", true); // not used
                    params.put("foo", 1);
                    params.put("bar", "baz");
                });
        assertEquals("select * from table where foo=? and bar=?", expanded.getSql());
        assertArrayEquals(new Object[] {1, "baz"}, expanded.getParameters());
    }

    @Test
    public void testMissingParam() {
        assertThrows(IllegalArgumentException.class, () -> {
            Statement.statement("select * from table where foo=:foo and bar=:bar")
                    .expand(params -> {
                        params.put("bar", "baz");
                    });
        });
    }

    @Test
    public void testPostgresJson1() {
        final ExpandedStatement expanded = Statement
                .statement("SELECT device_id, version, credentials FROM table WHERE tenant_id=:tenant_id AND credentials @> jsonb_build_object('type', :type, 'auth-id', :auth_id)")
                .expand(map -> {
                    map.put("tenant_id", "tenant");
                    map.put("type", "hashed-password");
                    map.put("auth_id", "auth");
                });

        assertEquals("SELECT device_id, version, credentials FROM table WHERE tenant_id=? AND credentials @> jsonb_build_object('type', ?, 'auth-id', ?)", expanded.getSql());
        assertArrayEquals(new Object[] {"tenant", "hashed-password", "auth"}, expanded.getParameters());
    }

    @Test
    public void testPostgresJson2() {
        final ExpandedStatement expanded =
                Statement.statement("INSERT INTO %s (tenant_id, device_id, version, data) VALUES (:tenant_id, :device_id, :version, to_jsonb(:data::jsonb))", "table")
                        .expand(map -> {
                            map.put("tenant_id", "tenant");
                            map.put("device_id", "device");
                            map.put("version", "version");
                            map.put("data", "{}");
                        });

        assertEquals("INSERT INTO table (tenant_id, device_id, version, data) VALUES (?, ?, ?, to_jsonb(?::jsonb))", expanded.getSql());
        assertArrayEquals(new Object[] {"tenant", "device", "version", "{}"}, expanded.getParameters());
    }


}
