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
  Text,
  TextVariants,
  Grid,
  GridItem,
  Switch
} from "@patternfly/react-core";
import { CheckCircleIcon, ErrorCircleOIcon } from "@patternfly/react-icons";
import { InputText, JsonViewEditor } from "components";
import { IInfoTypePlan } from "../GeneralInfo";

export interface IMessagingObject {
  url?: string;
  username?: string;
  password?: string;
  addressSpace?: string;
  eventsAddressName?: IInfoTypePlan;
  telemetryAddressName?: IInfoTypePlan;
  commandAddressName?: IInfoTypePlan;
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
}

const getCombinedString = (a: string, b?: string) => {
  let s: string = "";
  s += a;
  if (b !== undefined) {
    s += ", ";
    s += b;
  }
  return s;
};
const AccessCredentials: React.FunctionComponent<IAccessCredentialsProps> = ({
  tenantId,
  messaging,
  adapters
}) => {
  const [isHidden, setIsHidden] = useState<boolean>(false);
  const onToggle = () => {
    setIsHidden(!isHidden);
  };
  const EditIcon = (
    <Switch
      id="access-credential-switch"
      label="View in Json"
      labelOff="View in Json"
      isChecked={isHidden}
      onChange={onToggle}
    />
  );
  const data = {
    access_credentials: {
      tenantId: tenantId,
      messaging: messaging,
      adapters: adapters
    }
  };

  const IsEnabled = (value: boolean) => {
    return value ? (
      <>
        <CheckCircleIcon color={"green"} /> &nbsp;Enabled
      </>
    ) : (
      <>
        <ErrorCircleOIcon color={"red"} /> Disabled
      </>
    );
  };
  const Messaging = messaging && (
    <>
      <Text component={TextVariants.h2}>Messaging</Text>
      <br />
      {messaging.url && (
        <InputText
          label={"Tenant Id"}
          type={"text"}
          value={messaging.url}
          isReadOnly={true}
          enableCopy={true}
          ariaLabel={"input-messaging-url"}
        />
      )}
      <br />
      {messaging.username && (
        <InputText
          label={"User name"}
          type={"text"}
          value={messaging.username}
          isReadOnly={true}
          enableCopy={true}
          ariaLabel={"input-messaging-username"}
        />
      )}
      <br />
      {messaging.password && (
        <InputText
          label={"Password"}
          type={"password"}
          value={messaging.password}
          isReadOnly={true}
          ariaLabel={"input-messaging-password"}
        />
      )}
      <br />
      {messaging.addressSpace && (
        <InputText
          label={"Address Space"}
          type={"text"}
          value={messaging.addressSpace}
          isReadOnly={true}
          enableCopy={true}
          ariaLabel={"input-messaging-addressSpace"}
        />
      )}
      <br />
      {messaging.eventsAddressName && (
        <InputText
          label={"Events address name"}
          type={"text"}
          value={getCombinedString(
            messaging.eventsAddressName.type,
            messaging.eventsAddressName.plan
          )}
          isReadOnly={true}
          enableCopy={true}
          isExpandable={messaging.eventsAddressName.plan ? true : false}
          ariaLabel={"input-messaging-eventsAddressName"}
        />
      )}
      <br />
      {messaging.telemetryAddressName && (
        <InputText
          label={"Telemetry address name"}
          type={"text"}
          value={getCombinedString(
            messaging.telemetryAddressName.type,
            messaging.telemetryAddressName.plan
          )}
          isReadOnly={true}
          enableCopy={true}
          isExpandable={messaging.telemetryAddressName.plan ? true : false}
          ariaLabel={"input-messaging-telemetryAddressName"}
        />
      )}
      <br />
      {messaging.commandAddressName && (
        <InputText
          label={"Command address name"}
          type={"text"}
          value={getCombinedString(
            messaging.commandAddressName.type,
            messaging.commandAddressName.plan
          )}
          isReadOnly={true}
          enableCopy={true}
          isExpandable={messaging.commandAddressName.plan ? true : false}
          ariaLabel={"input-messaging-commandAddressName"}
        />
      )}
      <br />
    </>
  );
  const getAdapter = (adapter: IAdapter) => {
    let title = adapter.type.toUpperCase() + " Adapter";
    return (
      <>
        <Text component={TextVariants.h2}>{title}</Text>
        {adapter.value.tlsEnabled && (
          <>
            <br />
            <Grid>
              <GridItem span={4}>
                <b>tls</b>
              </GridItem>
              <GridItem span={8}>
                {IsEnabled(adapter.value.tlsEnabled)}
              </GridItem>
            </Grid>
          </>
        )}
        {adapter.value.url && (
          <>
            <br />
            <InputText
              label={"Url"}
              type={"text"}
              value={adapter.value.url}
              isReadOnly={true}
              enableCopy={true}
              ariaLabel={`input-${adapter.type}-api-url`}
              isExpandable={false}
            />
          </>
        )}
        {adapter.value.host && (
          <>
            <br />
            <InputText
              label={"Host"}
              type={"number"}
              value={adapter.value.host}
              isReadOnly={true}
              enableCopy={true}
              ariaLabel={"input-registration-api-host"}
              isExpandable={false}
            />
          </>
        )}
        {adapter.value.port && (
          <>
            <br />
            <InputText
              label={"Port"}
              type={"number"}
              value={adapter.value.port}
              isReadOnly={true}
              enableCopy={true}
              ariaLabel={"input-registration-api-port"}
              isExpandable={false}
            />
          </>
        )}
      </>
    );
  };

  const Adapters = adapters && (
    <>
      {adapters.map((adapter: IAdapter) => (
        <>
          {" "}
          {getAdapter(adapter)}
          <br />
        </>
      ))}
    </>
  );
  const CardToDisplay = (
    <Card>
      <CardHeader style={{ fontSize: 20 }}>
        <Split>
          <SplitItem>
            <h1>
              <b>Access Credentials</b>
            </h1>
          </SplitItem>
          <SplitItem isFilled />
          <SplitItem>{EditIcon}</SplitItem>
        </Split>
      </CardHeader>
      {isHidden ? (
        <CardBody>
          <JsonViewEditor readOnly={true} detailInJson={data} />
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
              ariaLabel={"input-username"}
            />
          )}
          <br />
          {/*TODO: Add certificate options*/}
          {/* {password && (
            <InputText
              label={"Password"}
              type={"password"}
              value={password}
              isReadOnly={true}
              ariaLabel={"input-password"}
            />
          )} 
          <br />
          */}
          {Messaging}
          <br />
          {Adapters}
        </CardBody>
      )}
    </Card>
  );
  return <PageSection>{CardToDisplay}</PageSection>;
};

export { AccessCredentials };
