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
import { IInfoTypePlan } from "modules/project-detail/components/GeneralInfo";
import { getCombinedString } from "utils";

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

const AccessCredentials: React.FunctionComponent<IAccessCredentialsProps> = ({
  tenantId,
  messaging,
  adapters
}) => {
  const [isHidden, setIsHidden] = useState<boolean>(false);
  const onToggle = () => {
    setIsHidden(!isHidden);
  };
  const EditIcon = () => (
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

  const Messaging = () => {
    const {
      url,
      username,
      password,
      addressSpace,
      commandAddressName,
      eventsAddressName,
      telemetryAddressName
    } = messaging || {};
    return (
      <>
        <Text component={TextVariants.h2}>Messaging</Text>
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
        {eventsAddressName && (
          <InputText
            label={"Events address name"}
            type={"text"}
            value={getCombinedString(
              eventsAddressName.type,
              eventsAddressName.plan
            )}
            isReadOnly={true}
            enableCopy={true}
            isExpandable={eventsAddressName.plan ? true : false}
            id={"messaging-event-address-name-input"}
            ariaLabel={"messaging event Address Name"}
          />
        )}
        <br />
        {telemetryAddressName && (
          <InputText
            label={"Telemetry address name"}
            type={"text"}
            value={getCombinedString(
              telemetryAddressName.type,
              telemetryAddressName.plan
            )}
            isReadOnly={true}
            enableCopy={true}
            isExpandable={telemetryAddressName.plan ? true : false}
            id={"messaging-telemetry-address-name-input"}
            ariaLabel={"messaging telemetry Address Name"}
          />
        )}
        <br />
        {commandAddressName && (
          <InputText
            label={"Command address name"}
            type={"text"}
            value={getCombinedString(
              commandAddressName.type,
              commandAddressName.plan
            )}
            isReadOnly={true}
            enableCopy={true}
            isExpandable={commandAddressName.plan ? true : false}
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
        <Text component={TextVariants.h2}>
          {type.toUpperCase() + " Adapter"}
        </Text>
        {value.tlsEnabled && (
          <>
            <br />
            <Grid>
              <GridItem span={4}>
                <b>tls</b>
              </GridItem>
              <GridItem span={8}>{isEnabled(value.tlsEnabled)}</GridItem>
            </Grid>
          </>
        )}
        {value.url && (
          <>
            <br />
            <InputText
              label={"Url"}
              type={"text"}
              value={value.url}
              isReadOnly={true}
              enableCopy={true}
              id={`adapter-${type}-api-url-input`}
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
            <h1>
              <b>Access Credentials</b>
            </h1>
          </SplitItem>
          <SplitItem isFilled />
          <SplitItem>
            <EditIcon />
          </SplitItem>
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
              id={"tenant-id-input"}
              ariaLabel={"tenant id"}
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
