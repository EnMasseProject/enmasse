/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { IEndPoint } from "modules/endpoints/EndpointPage";
import { PageSection, PageSectionVariants } from "@patternfly/react-core";
import { EndPointList } from "modules/endpoints/components";

export default {
  title: "Endpoints Details"
};

const endpoints: IEndPoint[] = [
  {
    name: "messaging-service",
    type: "cluster",
    host: "message-queue.space.enmasse-infra",
    ports: [
      { port: 5671, protocol: "amqps", name: "" },
      { port: 5672, protocol: "amqp_ws", name: "" },
      { port: 443, protocol: "amqp_wss", name: "" }
    ]
  },
  {
    name: "messaging",
    type: "route",
    host: "message-queue.space.enmasse-infra",
    ports: [
      { port: 5671, protocol: "amqps", name: "" },
      { port: 5672, protocol: "amqp_ws", name: "" },
      { port: 443, protocol: "amqp_wss", name: "" }
    ]
  },
  {
    name: "messaging-wss",
    type: "route",
    host: "message-queue.space.enmasse-infra",
    ports: [
      { port: 5671, protocol: "amqps", name: "" },
      { port: 5672, protocol: "amqp_ws", name: "" },
      { port: 443, protocol: "amqp_wss", name: "" }
    ]
  }
];

export const endpointsList = () => (
  <PageSection variant={PageSectionVariants.light}>
    <EndPointList endpoints={endpoints} />
  </PageSection>
);
