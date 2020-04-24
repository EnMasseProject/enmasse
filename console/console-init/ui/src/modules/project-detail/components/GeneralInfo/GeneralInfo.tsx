/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Card,
  CardBody,
  Divider,
  CardHeader,
  PageSection
} from "@patternfly/react-core";
import { kFormatter } from "utils";
export interface IInfoTypePlan {
  type: string;
  plan?: string;
}

interface IGeneralInfoProps {
  addressSpace: string;
  eventAddressName: IInfoTypePlan;
  telemetryAddressName: IInfoTypePlan;
  commandAddressName: IInfoTypePlan;
  maxConnection: number;
  dataVolume: number;
  startDate: string | Date;
  endDate: Date | string;
}

const GeneralInfo: React.FunctionComponent<IGeneralInfoProps> = ({
  addressSpace,
  eventAddressName,
  telemetryAddressName,
  commandAddressName,
  maxConnection,
  dataVolume,
  startDate,
  endDate
}) => {
  return (
    <PageSection>
      <Card>
        <CardHeader style={{ fontSize: 20 }}>General Info</CardHeader>
        <CardBody>
          <b style={{ marginRight: 20 }}>Address space</b>
          {addressSpace} <br />
          <b style={{ marginRight: 20 }}>Event address name</b>
          {eventAddressName.type}{" "}
          {eventAddressName.plan ? ", " + eventAddressName.plan : ""}
          <br />
          <b style={{ marginRight: 20 }}>Telemetry address name</b>
          {telemetryAddressName.type}
          {telemetryAddressName.plan ? ", " + telemetryAddressName.plan : ""}
          <br />
          <b style={{ marginRight: 20 }}>Command address name</b>
          {commandAddressName.type}
          {commandAddressName.plan ? ", " + commandAddressName.plan : ""}
          <br />
          <br />
          <Divider />
          <br />
          <b style={{ marginRight: 20 }}>Max Connection</b>
          {kFormatter(maxConnection)} <br />
          <b style={{ marginRight: 20 }}>Data Volume</b>
          {kFormatter(dataVolume)} <br />
          <b style={{ marginRight: 20 }}>Start Date</b>
          {startDate} <br />
          <b style={{ marginRight: 20 }}>End Date</b>
          {endDate} <br />
        </CardBody>
      </Card>
    </PageSection>
  );
};

export { GeneralInfo };
