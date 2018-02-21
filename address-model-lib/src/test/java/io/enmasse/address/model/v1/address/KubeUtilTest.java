/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1.address;

import io.enmasse.address.model.KubeUtil;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class KubeUtilTest {
    @Test
    public void testLeaveSpaceForPodIdentifier() {
        String uuid = UUID.randomUUID().toString();
        String id = KubeUtil.sanitizeWithUuid("myloooooooooooooooooooooooooooooooooooongid", uuid);
        assertThat(id.length(), is(60));
    }
}
