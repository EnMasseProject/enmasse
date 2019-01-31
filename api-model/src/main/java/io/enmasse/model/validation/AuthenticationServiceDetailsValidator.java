/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.model.validation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import io.enmasse.address.model.AuthenticationService;
import io.enmasse.address.model.AuthenticationServiceType;

public class AuthenticationServiceDetailsValidator implements ConstraintValidator<AuthenticationServiceDetails, AuthenticationService> {

    @Override
    public boolean isValid(final AuthenticationService service, final ConstraintValidatorContext context) {

        if (service == null) {
            // nothing to validate
            return true;
        }

        if (service.getType() == null) {
            // nothing to validate
            return true;
        }

        Map<String, Object> details = service.getDetails();
        if (details == null) {
            // null map means: no values -> empty map
            // which may be fine for some services
            details = Collections.emptyMap();
        }

        final Set<String> missing = findMissing(service.getType(), service.getDetails());
        final Set<String> unknown = findUnknown(service.getType(), service.getDetails());

        handleFields(context, "Missing", missing);
        handleFields(context, "Unknown", unknown);

        return missing.isEmpty() && unknown.isEmpty();

    }

    private void handleFields(final ConstraintValidatorContext context, final String kind, final Set<String> fields) {
        if (fields.isEmpty()) {
            return;
        }

        context.disableDefaultConstraintViolation();

        for (final String field : fields) {
            context
                    .buildConstraintViolationWithTemplate(kind + " field: " + field)
                    .addConstraintViolation();
        }
    }

    private Set<String> findUnknown(final AuthenticationServiceType type, final Map<String, Object> details) {
        // we are using a TreeSet here to get a stable field order
        final Set<String> fields = new TreeSet<>(details.keySet());
        fields.removeAll(type.getDetailsFields().keySet());
        return fields;
    }

    private Set<String> findMissing(final AuthenticationServiceType type, final Map<String, Object> details) {
        // we are using a TreeSet here to get a stable field order
        final Set<String> fields = new TreeSet<>(type.getMandatoryFields());
        fields.removeAll(details.keySet());
        return fields;
    }
}

