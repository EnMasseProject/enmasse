package io.enmasse.systemtest.executor.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class represents Map of arguments (allow duplicate argument)
 */
public class ArgumentMap {
    private final Map<Argument,Set<String>> mappings = new HashMap<>();

    /**
     * Returns set of values for argument
     * @param arg argument
     * @return Set of values
     */
    public Set<String> getValues(Argument arg)
    {
        return mappings.get(arg);
    }

    /**
     * Returns set of arguments
     * @return set of arguments
     */
    public Set<Argument> getArguments(){
        return mappings.keySet();
    }

    /**
     * Add argument and his values
     * @param key arguments
     * @param value value
     * @return true if operation is completed
     */
    public Boolean put(Argument key, String value)
    {
        Set<String> target = mappings.get(key);

        if(target == null)
        {
            target = new HashSet<>();
            mappings.put(key,target);
        }

        return target.add(value);
    }
}
