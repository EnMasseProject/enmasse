/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Page,
  PageSection,
  Grid,
  GridItem,
  Card,
  CardBody,
  Title,
  Alert
} from "@patternfly/react-core";
import { AdapterList, IAdapterListProps } from "components";
import {
  CredentialsView,
  ICredentialsViewProps
} from "modules/iot-device-detail/components";

export interface IConfigurationInfoProps
  extends Pick<IAdapterListProps, "adapters">,
    Pick<ICredentialsViewProps, "credentials"> {
  id: string;
}

export const ConfigurationInfo: React.FC<IConfigurationInfoProps> = ({
  id,
  adapters,
  credentials
}) => {
  return (
    <Page id={id}>
      <PageSection>
        <Alert
          variant="info"
          isInline
          title="Device connection configuration info"
        >
          This info section provides a quick view of the information needed to
          configure a device connection on the device side.
        </Alert>
        <br />
        <Grid gutter="sm">
          <GridItem span={6}>
            <Card>
              <CardBody>
                <Title size="xl" headingLevel="h1">
                  <b>Adapters</b>
                </Title>
                <br />
                <AdapterList id="ci-adapter" adapters={adapters} />
              </CardBody>
            </Card>
          </GridItem>
          <GridItem span={6}>
            <Grid gutter="sm">
              <GridItem>
                <Card>
                  <CardBody>dropdown</CardBody>
                </Card>
              </GridItem>
            </Grid>
            <CredentialsView
              id="ci-credentials-view"
              credentials={credentials}
            />
          </GridItem>
        </Grid>
      </PageSection>
    </Page>
  );
};
