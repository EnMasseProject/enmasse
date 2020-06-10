/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { EndPointList } from "./EndPointList";
import { IEndPoint } from "modules/endpoints/EndpointPage";

describe("Endpoint List", () => {
  test("it renders list of endpoints", () => {
    const endpoints: IEndPoint[] = [
      {
        name: "messaging-service",
        type: "Cluster",
        host: "message-queue.space.enmasse-infra",
        ports: [
          { port: 5671, protocol: "AMQPS", name: "AMQPS" },
          { port: 5672, protocol: "AMQP", name: "AMQP" },
          { port: 443, protocol: "AMQP_WSS", name: "AMQP-WSS" }
        ]
      }
    ];
    const { getByText } = render(
      <MemoryRouter>
        <EndPointList endpoints={endpoints} />
      </MemoryRouter>
    );
    getByText(endpoints[0].name);
    getByText(endpoints[0].type);
    getByText(endpoints[0].host);
  });
});
