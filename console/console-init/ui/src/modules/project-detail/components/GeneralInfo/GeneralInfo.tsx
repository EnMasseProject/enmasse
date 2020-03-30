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
import { StyleSheet, css } from "@patternfly/react-styles";

const styles = StyleSheet.create({
  style_margin: {
    marginRight: 20
  },
  font_size: {
    fontSize: 20
  }
});
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
        <CardHeader className={css(styles.font_size)}>General Info</CardHeader>
        <CardBody>
          <b className={css(styles.style_margin)}>Address space</b>
          {addressSpace} <br />
          <b className={css(styles.style_margin)}>Event address name</b>
          {eventAddressName.type}{" "}
          {eventAddressName.plan ? ", " + eventAddressName.plan : ""}
          <br />
          <b className={css(styles.style_margin)}>Telemetry address name</b>
          {telemetryAddressName.type}
          {telemetryAddressName.plan ? ", " + telemetryAddressName.plan : ""}
          <br />
          <b className={css(styles.style_margin)}>Command address name</b>
          {commandAddressName.type}
          {commandAddressName.plan ? ", " + commandAddressName.plan : ""}
          <br />
          <br />
          <Divider />
          <br />
          <b className={css(styles.style_margin)}>Max Connection</b>
          {kFormatter(maxConnection)} <br />
          <b className={css(styles.style_margin)}>Data Volume</b>
          {kFormatter(dataVolume)} <br />
          <b className={css(styles.style_margin)}>Start Date</b>
          {startDate} <br />
          <b className={css(styles.style_margin)}>End Date</b>
          {endDate} <br />
        </CardBody>
      </Card>
    </PageSection>
  );
};

export { GeneralInfo };
