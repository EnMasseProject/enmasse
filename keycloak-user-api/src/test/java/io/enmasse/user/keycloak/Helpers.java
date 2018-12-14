/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.user.keycloak;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.enmasse.user.model.v1.UserAuthorization;

public final class Helpers {
    private Helpers() {
    }

    public static <T extends Comparable<T>> int compareLists(List<T> o1, List<T> o2) {

        if (o1 == o2) {
            return 0;
        }

        // ensure lists are non-null and mutable

        if (o1 == null) {
            o1 = new ArrayList<>();
        } else {
            o1 = new ArrayList<>(o1);
        }

        if (o2 == null) {
            o2 = new ArrayList<>();
        } else {
            o2 = new ArrayList<>(o2);
        }

        // compare by size

        int rc = Integer.compare(o1.size(), o2.size());

        if (rc != 0) {
            return rc;
        }

        // compare by content

        for (int i = 0; i < o1.size(); i++) {
            rc = o1.get(i).compareTo(o2.get(i));
            if (rc != 0) {
                return rc;
            }
        }

        // seem equal

        return 0;
    }

    /**
     * Compare two user authorizations.
     */
    public static int compareUserAuthorization(final UserAuthorization o1, final UserAuthorization o2) {
        final int rc = compareLists(o1.getOperations(), o2.getOperations());
        if (rc != 0) {
            return rc;
        }
        return compareLists(o1.getAddresses(), o2.getAddresses());
    }

    public static void assertSorted(final Object[] expected, final Object[] actual) {
        Arrays.sort(expected);
        Arrays.sort(actual);
        assertArrayEquals(expected, actual);
    }

    public static <T extends Comparable<T>> void assertSorted(final Collection<T> expected,
            final Collection<T> actual) {
        assertSorted(expected.toArray(), actual.toArray());
    }
}
