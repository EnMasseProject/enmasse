/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.clients;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class represents Map of arguments (allow duplicate argument)
 */
public class ArgumentMap {
    private final Map<Argument, ArrayList<String>> mappings = new HashMap<>();

    /**
     * Returns set of values for argument
     *
     * @param arg argument
     * @return Set of values
     */
    public ArrayList<String> getValues(Argument arg) {
        return mappings.get(arg);
    }

    /**
     * Returns set of arguments
     *
     * @return set of arguments
     */
    public Set<Argument> getArguments() {
        return mappings.keySet();
    }

    /**
     * Removes argument from map
     *
     * @param key name of argument
     */
    public void remove(Argument key) {
        mappings.remove(key);
    }

    /**
     * Clear all arguments
     */
    public void clear() {
        mappings.clear();
    }

    /**
     * Add argument and his values
     *
     * @param key   arguments
     * @param value value
     * @return true if operation is completed
     */
    public Boolean put(Argument key, String value) {
        ArrayList<String> target = mappings.get(key);

        if (target == null ||
                (key != Argument.MSG_PROPERTY &&
                        key != Argument.MSG_CONTENT_LIST_ITEM &&
                        key != Argument.MSG_CONTENT_MAP_ITEM &&
                        key != Argument.MSG_ANNOTATION) &&
                        key != Argument.CONN_PROPERTY) {

            target = new ArrayList<>();
            mappings.put(key, target);
        }
        return target.add(value);
    }
}
