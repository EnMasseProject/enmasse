/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Card,
  CardBody,
  PageSection,
  Split,
  SplitItem,
  Grid,
  GridItem,
  Title,
  Button,
  ButtonVariant,
  CardTitle
} from "@patternfly/react-core";
import { DownloadIcon } from "@patternfly/react-icons";
import {
  InputText,
  JsonEditor,
  SwitchWithToggle,
  IAdapter,
  AdapterList
} from "components";

export interface IIoTMessagingObject {
  url?: string;
  username?: string;
  password?: string;
  addressSpace?: string;
  eventAddress?: string;
  telemetryAddress: string;
  commandAddresses?: Array<string | undefined>;
}
export interface IAccessCredentialsProps {
  tenantId?: string;
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
      id="access-credentials-view-json-switchtoggle"
      aria-label="Switch with toggle for view in json"
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
      eventAddress,
      telemetryAddress
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
            id={"access-credentials-messaging-tenant-id-input"}
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
            id={"access-credentials-messaging-username-input"}
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
            id={"access-credentials-messaging-password-input"}
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
            id={"access-credentials-messaging-addressspace-input"}
            ariaLabel={"messaging addressSpace"}
          />
        )}
        <br />
        {eventAddress && (
          <InputText
            label={"Events address name"}
            type={"text"}
            value={eventAddress}
            isReadOnly={true}
            enableCopy={true}
            id={"access-credentials-messaging-event-address-name-input"}
            ariaLabel={"messaging event Address Name"}
          />
        )}
        <br />
        {telemetryAddress && (
          <InputText
            label={"Telemetry address name"}
            type={"text"}
            value={telemetryAddress}
            isReadOnly={true}
            enableCopy={true}
            id={"access-credentials-messaging-telemetry-address-name-input"}
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
            id={"access-credentials-messaging-command-address-name-input"}
            ariaLabel={"messaging command Address Name"}
          />
        )}
        <br />
      </>
    );
  };

  const CardToDisplay = () => (
    <Card>
      <CardTitle style={{ fontSize: 20 }}>
        <Split>
          <SplitItem>
            <Title size="xl" headingLevel="h2" id="access-credentials-title">
              Access Credentials
            </Title>
          </SplitItem>
          <SplitItem isFilled />
          <SplitItem>
            <EditIcon />
          </SplitItem>
        </Split>
      </CardTitle>
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
              id={"access-credentials-tenant-id-input"}
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
                id="access-credentials-download-pem-cert-button"
                aria-label="Download PEM certificate"
                variant={ButtonVariant.tertiary}
                onClick={onPemCertificateClick}
              >
                <DownloadIcon /> Download PEM
              </Button>
              {"  "}
              <Button
                id="access-credentials-download-jks-cert-button"
                aria-label="Download JKS certificate"
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
          <AdapterList
            id="access-credentials-adapter-list-container"
            adapters={adapters}
          />
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
