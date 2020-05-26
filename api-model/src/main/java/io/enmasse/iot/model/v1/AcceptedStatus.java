/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
                )
        )
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AcceptedStatus {

    /*
     * This is the Hono prepared TenantObject response. It actually is a map,
     * but the sundrio builder cannot handle a map that is "null", it always
     * creates an instance of an empty map. However, if the controller does not
     * accept the configuration, the map itself will be null, and thus we should
     * not allow the tenant to be used. Having an object allows us to detect the
     * difference between a null and an empty map.
     */
    private Object configuration;

    public void setConfiguration(final Object configuration) {
        this.configuration = configuration;
    }

    public Object getConfiguration() {
        return this.configuration;
    }

}
