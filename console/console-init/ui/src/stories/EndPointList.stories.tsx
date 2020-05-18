/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router-dom";
import { IEndPoint } from "modules/endpoints/EndpointPage";
import { PageSection, PageSectionVariants } from "@patternfly/react-core";
import { EndPointList } from "modules/endpoints/components";

export default {
  title: "Endpoints Details"
};

const endpoints: IEndPoint[] = [
  {
    name: "messaging-service",
    type: "Cluster",
    host: "message-queue.space.enmasse-infra",
    ports: [
      { portNumber: 5671, protocol: "AMQPS" },
      { portNumber: 5672, protocol: "AMQP" },
      { portNumber: 443, protocol: "AMQP-WSS" }
    ]
  },
  {
    name: "messaging",
    type: "Route",
    host: "message-queue.space.enmasse-infra",
    ports: [
      { portNumber: 5671, protocol: "AMQPS" },
      { portNumber: 5672, protocol: "AMQP" },
      { portNumber: 443, protocol: "AMQP-WSS" }
    ]
  },
  {
    name: "messaging-wss",
    type: "Route",
    host: "message-queue.space.enmasse-infra",
    ports: [
      { portNumber: 5671, protocol: "AMQPS" },
      { portNumber: 5672, protocol: "AMQP" },
      { portNumber: 443, protocol: "AMQP-WSS" }
    ]
  }
];

export const endpointsList = () => (
  <MemoryRouter>
    <PageSection variant={PageSectionVariants.light}>
      <EndPointList endpoints={endpoints} />
    </PageSection>
  </MemoryRouter>
);
