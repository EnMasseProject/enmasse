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
  PageSection,
  Title
} from "@patternfly/react-core";
import { kFormatter } from "utils";
import { StyleSheet, css } from "@patternfly/react-styles";
import { NavLink } from "react-router-dom";

const styles = StyleSheet.create({
  style_margin: {
    marginRight: 20
  },
  font_size: {
    fontSize: 20
  }
});
interface IGeneralInfoProps {
  addressSpace: string;
  eventAddresses: Array<string>;
  telemetryAddresses: Array<string>;
  commandAddresses: Array<string>;
  maxConnection: number;
  dataVolume: number;
  startDate: string | Date;
  endDate: Date | string;
  namespace: string;
}

const GeneralInfo: React.FunctionComponent<IGeneralInfoProps> = ({
  addressSpace,
  eventAddresses,
  telemetryAddresses,
  commandAddresses,
  maxConnection,
  dataVolume,
  startDate,
  endDate,
  namespace
}) => {
  return (
    <PageSection>
      <Card>
        <CardHeader className={css(styles.font_size)}>
          <Title size="xl" headingLevel="h2">
            General Info
          </Title>
        </CardHeader>
        <CardBody>
          <b className={css(styles.style_margin)}>Address space</b>
          <NavLink
            //TODO:=modify route
            to={`/address-spaces/${namespace}/${addressSpace}/standard/addresses`}
            className="pf-c-nav__link"
            id={`navlink-as-${addressSpace}`}
          >
            {addressSpace}
          </NavLink>{" "}
          <br />
          <b className={css(styles.style_margin)}>Events address name</b>
          {eventAddresses &&
            eventAddresses.length > 0 &&
            eventAddresses.map((address: string) => (
              <>
                <NavLink
                  //TODO:=modify route
                  to={""}
                  className="pf-c-nav__link"
                  id={`navlink-gi-event-${address}`}
                  key={`navlink-gi-event-${address}`}
                >
                  {address}
                </NavLink>{" "}
              </>
            ))}
          <br />
          <b className={css(styles.style_margin)}>Telemetry address name</b>
          {telemetryAddresses &&
            telemetryAddresses.length > 0 &&
            telemetryAddresses.map((address: string) => (
              <>
                <NavLink
                  //TODO:=modify route
                  to={""}
                  className="pf-c-nav__link"
                  id={`navlink-gi-telemetry-${address}`}
                  key={`navlink-gi-telemetry-${address}`}
                >
                  {address}
                </NavLink>{" "}
              </>
            ))}
          <br />
          <b className={css(styles.style_margin)}>Command address name</b>
          {commandAddresses &&
            commandAddresses.length > 0 &&
            commandAddresses.map((address: string) => (
              <>
                <NavLink
                  //TODO:=modify route
                  to={""}
                  className="pf-c-nav__link"
                  id={`navlink-gi-command-${address}`}
                  key={`navlink-gi-command-${address}`}
                >
                  {address}
                </NavLink>{" "}
              </>
            ))}
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
