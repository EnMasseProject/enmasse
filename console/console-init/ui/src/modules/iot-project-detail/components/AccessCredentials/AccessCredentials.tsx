/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Card,
  CardBody,
  CardHeader,
  PageSection,
  Split,
  SplitItem,
  Grid,
  GridItem,
  Title,
  Button,
  ButtonVariant
} from "@patternfly/react-core";
import { DownloadIcon } from "@patternfly/react-icons";
import {
  InputText,
  JsonEditor,
  SwitchWithToggle,
  AdapterList,
  IAdapter
} from "components";

export interface IIoTMessagingObject {
  url?: string;
  username?: string;
  password?: string;
  addressSpace?: string;
  eventsAddresses?: Array<string>;
  telemetryAddresses?: Array<string>;
  commandAddresses?: Array<string>;
}
export interface IAccessCredentialsProps {
  tenantId: string;
  messaging: IIoTMessagingObject;
  adapters: IAdapter[];
  onDownloadCertificate: (certificateType: ".pem" | ".jks") => void;
}

const AccessCredentials: React.FunctionComponent<IAccessCredentialsProps> = ({
  tenantId,
  messaging,
  adapters,
  onDownloadCertificate
}) => {
  const [isHidden, setIsHidden] = useState<boolean>(false);
  const onToggle = () => {
    setIsHidden(!isHidden);
  };
  const EditIcon = () => (
    <SwitchWithToggle
      id="access-credential-switch"
      label="View JSON format"
      labelOff="View JSON format"
      isChecked={isHidden}
      onChange={onToggle}
    />
  );

  const accessCredentailJsonData = {
    access_credentials: {
      tenantId: tenantId,
      messaging: messaging,
      adapters: adapters
    }
  };

  const onPemCertificateClick = () => {
    onDownloadCertificate(".pem");
  };
  const onJksCertificateClick = () => {
    onDownloadCertificate(".jks");
  };
  const Messaging = () => {
    const {
      url,
      username,
      password,
      addressSpace,
      commandAddresses,
      eventsAddresses,
      telemetryAddresses
    } = messaging || {};
    return (
      <>
        <Title size="xl" headingLevel="h2">
          Messaging
        </Title>
        <br />
        {url && (
          <InputText
            label={"Tenant Id"}
            type={"text"}
            value={url}
            isReadOnly={true}
            enableCopy={true}
            id={"messaging-tenant-id-input"}
            ariaLabel={"messaging tenant id"}
          />
        )}
        <br />
        {username && (
          <InputText
            label={"User name"}
            type={"text"}
            value={username}
            isReadOnly={true}
            enableCopy={true}
            id={"messaging-username-input"}
            ariaLabel={"messaging username"}
          />
        )}
        <br />
        {password && (
          <InputText
            label={"Password"}
            type={"password"}
            value={password}
            isReadOnly={true}
            id={"messaging-password-input"}
            ariaLabel={"messaging password"}
          />
        )}
        <br />
        {addressSpace && (
          <InputText
            label={"Address space"}
            type={"text"}
            value={addressSpace}
            isReadOnly={true}
            enableCopy={true}
            id={"messaging-addressspace-input"}
            ariaLabel={"messaging addressSpace"}
          />
        )}
        <br />
        {eventsAddresses && eventsAddresses.length > 0 && (
          <InputText
            label={"Events address name"}
            type={"text"}
            value={eventsAddresses.join(", ")}
            isReadOnly={true}
            enableCopy={true}
            isExpandable={eventsAddresses.length > 1}
            id={"messaging-event-address-name-input"}
            ariaLabel={"messaging event Address Name"}
          />
        )}
        <br />
        {telemetryAddresses && telemetryAddresses.length > 0 && (
          <InputText
            label={"Telemetry address name"}
            type={"text"}
            value={telemetryAddresses.join(", ")}
            isReadOnly={true}
            enableCopy={true}
            isExpandable={telemetryAddresses.length > 1}
            id={"messaging-telemetry-address-name-input"}
            ariaLabel={"messaging telemetry Address Name"}
          />
        )}
        <br />
        {commandAddresses && commandAddresses.length > 0 && (
          <InputText
            label={"Command address name"}
            type={"text"}
            value={commandAddresses.join(", ")}
            isReadOnly={true}
            enableCopy={true}
            isExpandable={commandAddresses.length > 1}
            id={"messaging-command-address-name-input"}
            ariaLabel={"messaging command Address Name"}
          />
        )}
        <br />
      </>
    );
  };

  const CardToDisplay = () => (
    <Card>
      <CardHeader style={{ fontSize: 20 }}>
        <Split>
          <SplitItem>
            <Title size="xl" headingLevel="h2">
              Access Credentials
            </Title>
          </SplitItem>
          <SplitItem isFilled />
          <SplitItem>
            <EditIcon />
          </SplitItem>
        </Split>
      </CardHeader>
      {isHidden ? (
        <CardBody>
          <JsonEditor
            readOnly={true}
            value={JSON.stringify(accessCredentailJsonData, undefined, 2)}
          />
        </CardBody>
      ) : (
        <CardBody>
          {tenantId && (
            <InputText
              label={"Tenant Id"}
              type={"text"}
              value={tenantId}
              isReadOnly={true}
              enableCopy={true}
              id={"tenant-id-input"}
              ariaLabel={"tenant id"}
            />
          )}
          <br />
          <Grid>
            <GridItem span={4}>
              <b>Certificate</b>
            </GridItem>
            <GridItem span={8}>
              <Button
                variant={ButtonVariant.tertiary}
                onClick={onPemCertificateClick}
              >
                <DownloadIcon /> Download PEM
              </Button>
              {"  "}
              <Button
                variant={ButtonVariant.tertiary}
                onClick={onJksCertificateClick}
              >
                <DownloadIcon /> Download JKS
              </Button>
            </GridItem>
          </Grid>
          <br />
          <Messaging />
          <br />
          <AdapterList id="ac-adapter-list" adapters={adapters} />
        </CardBody>
      )}
    </Card>
  );

  return (
    <PageSection>
      <CardToDisplay />
    </PageSection>
  );
};

export { AccessCredentials };
