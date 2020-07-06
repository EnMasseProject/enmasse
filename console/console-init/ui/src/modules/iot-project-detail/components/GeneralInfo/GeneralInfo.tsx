/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Card,
  CardBody,
  Divider,
  PageSection,
  Title,
  CardTitle,
  Button
} from "@patternfly/react-core";
import { kFormatter } from "utils";
import { StyleSheet, css } from "aphrodite";
import { useHistory } from "react-router-dom";

const styles = StyleSheet.create({
  style_margin: {
    marginRight: 20
  },
  font_size: {
    fontSize: 20
  }
});
interface IGeneralInfoProps {
  addressSpace?: string;
  eventAddress?: string;
  telemetryAddress?: string;
  commandAddresses?: Array<string>;
  maxConnection: number;
  dataVolume: number;
  startDate: string | Date;
  endDate: Date | string;
  namespace?: string;
}

const GeneralInfo: React.FunctionComponent<IGeneralInfoProps> = ({
  addressSpace,
  eventAddress,
  telemetryAddress,
  commandAddresses,
  maxConnection,
  dataVolume,
  startDate,
  endDate,
  namespace
}) => {
  const history = useHistory();

  const navigateToAdressSpace = () => {
    history.push(
      `/messaging-projects/${namespace}/${addressSpace}/standard/addresses`
    );
  };

  const navigateToAddress = (address: string) => {
    history.push(
      `/messaging-projects/${namespace}/${addressSpace}/standard/addresses/${addressSpace}.${address}`
    );
  };

  return (
    <PageSection>
      <Card>
        <CardTitle className={css(styles.font_size)}>
          <Title size="xl" headingLevel="h2">
            General Info
          </Title>
        </CardTitle>
        <CardBody>
          <b className={css(styles.style_margin)}>Address space</b>
          <Button variant="link" isInline onClick={navigateToAdressSpace}>
            {addressSpace}
          </Button>
          <br />
          <b className={css(styles.style_margin)}>Events address name</b>
          {eventAddress && (
            <Button
              variant="link"
              onClick={() => navigateToAddress(eventAddress)}
              isInline
            >
              {eventAddress}
            </Button>
          )}
          <br />
          <b className={css(styles.style_margin)}>Telemetry address name</b>
          {telemetryAddress && (
            <Button
              variant="link"
              onClick={() => navigateToAddress(telemetryAddress)}
              isInline
            >
              {telemetryAddress}
            </Button>
          )}
          <br />
          <b className={css(styles.style_margin)}>Command address name</b>
          {commandAddresses &&
            commandAddresses.length > 0 &&
            commandAddresses.map((address: string, index: number) => (
              <React.Fragment key={`navlink-gi-command-${address}-${index}`}>
                <Button
                  variant="link"
                  isInline
                  onClick={() => navigateToAddress(address)}
                >
                  {address}
                </Button>
                {index < commandAddresses.length - 1 && ", "}
              </React.Fragment>
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
