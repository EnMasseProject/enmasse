/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Card, CardBody, CardHeader, Title } from "@patternfly/react-core";

export const DeviceInfoCredentials: React.FC<{}> = () => {
  return (
    <Card>
      <CardHeader>
        <Title id="divice-info-header-title" headingLevel="h1" size="2xl">
          Credentials
        </Title>
      </CardHeader>
      <CardBody></CardBody>
    </Card>
  );
};
