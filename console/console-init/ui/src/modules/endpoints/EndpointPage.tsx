/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useDocumentTitle, useA11yRouteChange } from "use-patternfly";
import { PageSectionVariants, PageSection } from "@patternfly/react-core";
import { EndPointList } from "./components";

export interface IEndPoint {
  name: string;
  type: string;
  host: string;
  ports: Array<{ protocol: string; portNumber: number }>;
}

export default function AddressPage() {
  useDocumentTitle("Endpoint List");
  useA11yRouteChange();

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
    }
  ];
  return (
    <PageSection variant={PageSectionVariants.light}>
      {endpoints && endpoints.length > 0 ? (
        <EndPointList endpoints={endpoints} />
      ) : (
        ""
      )}
    </PageSection>
  );
}
