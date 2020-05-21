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
import {
  CheckCircleIcon,
  ErrorCircleOIcon,
  DownloadIcon
} from "@patternfly/react-icons";
import { InputText, JsonEditor, SwitchWithToggle } from "components";
import { getCombinedString } from "utils";

export interface IMessagingObject {
  url?: string;
  username?: string;
  password?: string;
  addressSpace?: string;
  eventsAddresses?: Array<string>;
  telemetryAddresses?: Array<string>;
  commandAddresses?: Array<string>;
}
export interface IAdapterConfig {
  url?: string;
  host?: string;
  port?: number;
  tlsEnabled?: boolean;
}
export interface IAdapter {
  type: "http" | "https" | "mqtt" | "amqp" | "amqps" | "coap";
  value: IAdapterConfig;
}

export interface IAccessCredentialsProps {
  tenantId: string;
  messaging: IMessagingObject;
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

  const isEnabled = (value: boolean) => {
    return value ? (
      <>
        <CheckCircleIcon color={"green"} /> &nbsp;Enabled
      </>
    ) : (
      <>
        <ErrorCircleOIcon color={"red"} /> &nbsp;Disabled
      </>
    );
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
            label={"Address Space"}
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
            value={getCombinedString(eventsAddresses)}
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
            value={getCombinedString(telemetryAddresses)}
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
            value={getCombinedString(commandAddresses)}
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

  const getAdapter = (adapter: IAdapter) => {
    const { value, type } = adapter;
    return (
      <>
        <Title size="xl" headingLevel="h2">
          {type.toUpperCase() + " Adapter"}
        </Title>
        {value.tlsEnabled && (
          <>
            <br />
            <Grid>
              <GridItem span={4}>
                <b>TLS</b>
              </GridItem>
              <GridItem span={8}>{isEnabled(value.tlsEnabled)}</GridItem>
            </Grid>
          </>
        )}
        {value.url && (
          <>
            <br />
            <InputText
              label={"URL"}
              type={"text"}
              value={value.url}
              isReadOnly={true}
              enableCopy={true}
              id={`adapter-${type}-api-url-input`}
              key={`adapter-${type}-api-url-input`}
              ariaLabel={`adapter-${type}-api-url`}
              isExpandable={false}
            />
          </>
        )}
        {value.host && (
          <>
            <br />
            <InputText
              label={"Host"}
              type={"number"}
              value={value.host}
              isReadOnly={true}
              enableCopy={true}
              id={`adapter-${type}-api-host-input`}
              ariaLabel={`adapter ${type} api host`}
              isExpandable={false}
            />
          </>
        )}
        {value.port && (
          <>
            <br />
            <InputText
              label={"Port"}
              type={"number"}
              value={value.port}
              isReadOnly={true}
              enableCopy={true}
              id={`adapter-${type}-api-port-input`}
              ariaLabel={`adapter ${type} api port`}
              isExpandable={false}
            />
          </>
        )}
      </>
    );
  };

  const Adapters = () => (
    <>
      {adapters &&
        adapters.map((adapter: IAdapter) => (
          <>
            {" "}
            {getAdapter(adapter)}
            <br />
          </>
        ))}
    </>
  );

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
          <Adapters />
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
