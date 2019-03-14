/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class IoTProjectTest {

	@Test
	void testCreate() {
		IoTProject project = new IoTProjectBuilder()
				.withNewMetadata()
				.withName("proj")
				.endMetadata()
				.withNewSpec()
				.withNewDownstreamStrategy()
				.withNewExternalStrategy()
				.withCertificate("a".getBytes())
				.withHost("host")
				.withPassword("pwd")
				.withUsername("username")
				.withPort(1234)
				.withTls(true)
				.endExternalStrategy()
				.endDownstreamStrategy()
				.endSpec()
				.build();

		assertEquals("proj", project.getMetadata().getName());
		assertEquals("a".getBytes(), project.getSpec().getDownstreamStrategy().getExternalStrategy().getCertificate());
		assertEquals("host", project.getSpec().getDownstreamStrategy().getExternalStrategy().getHost());
		assertEquals("pwd", project.getSpec().getDownstreamStrategy().getExternalStrategy().getPassword());
		assertEquals("username", project.getSpec().getDownstreamStrategy().getExternalStrategy().getUsername());
		assertEquals(1234, project.getSpec().getDownstreamStrategy().getExternalStrategy().getPort());
		assertEquals(true, project.getSpec().getDownstreamStrategy().getExternalStrategy().isTls());
	}

}
