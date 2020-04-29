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
  Switch
} from "@patternfly/react-core";
import { InputText, JsonViewEditor } from "components";
import { IAdapter, IAdapterConfig } from "../AccessCredentials";

interface IDeviceRegistationManagementProps {
  username?: string;
  password?: string;
  registrationApi?: IAdapterConfig;
  credentialApi?: IAdapterConfig;
}

const DeviceRegistationManagement: React.FunctionComponent<IDeviceRegistationManagementProps> = ({
  username,
  password,
  registrationApi,
  credentialApi
}) => {
  const [isHidden, setIsHidden] = useState<boolean>(false);
  const onToggle = () => {
    setIsHidden(!isHidden);
  };
  const EditIcon = (
    <Switch
      id="device-registry-management-switch"
      label="View in Json"
      labelOff="View in Json"
      isChecked={isHidden}
      onChange={onToggle}
    />
  );
  const RegistrationApi = () => {
    if (registrationApi) {
      const { url, host, port } = registrationApi;
      return (
        <>
          <Text component={TextVariants.h2}>Registration API</Text>
          <br />
          {url && (
            <InputText
              label={"Url"}
              type={"text"}
              value={url}
              isReadOnly={true}
              enableCopy={true}
              ariaLabel={"input-registration-api-url"}
              isExpandable={false}
            />
          )}
          <br />
          {host && (
            <InputText
              label={"Host"}
              type={"number"}
              value={host}
              isReadOnly={true}
              enableCopy={true}
              ariaLabel={"input-registration-api-host"}
              isExpandable={false}
            />
          )}
          <br />
          {port && (
            <InputText
              label={"Port"}
              type={"number"}
              value={port}
              isReadOnly={true}
              enableCopy={true}
              ariaLabel={"input-registration-api-port"}
              isExpandable={false}
            />
          )}
        </>
      );
    } else {
      return <></>;
    }
  };

  const CredentialApi = () => {
    if (credentialApi) {
      const { url, host, port } = credentialApi;
      return (
        <>
          <Text component={TextVariants.h2}>Credential API</Text>
          <br />
          {url && (
            <InputText
              label={"Url"}
              type={"text"}
              value={url}
              isReadOnly={true}
              enableCopy={true}
              ariaLabel={"input-credential-api-url"}
              isExpandable={false}
            />
          )}
          <br />
          {host && (
            <InputText
              label={"Host"}
              type={"number"}
              value={host}
              isReadOnly={true}
              enableCopy={true}
              ariaLabel={"input-credential-api-host"}
              isExpandable={false}
            />
          )}
          <br />
          {port && (
            <InputText
              label={"Port"}
              type={"number"}
              value={port}
              isReadOnly={true}
              enableCopy={true}
              ariaLabel={"input-credential-api-port"}
              isExpandable={false}
            />
          )}
        </>
      );
    } else {
      return <></>;
    }
  };

  const data = {
    device_registry: {
      username: username,
      password: password,
      registrationApi: registrationApi,
      credentialApi: credentialApi
    }
  };

  const CardToDisplay = (
    <Card>
      <CardHeader style={{ fontSize: 20 }}>
        <Split>
          <SplitItem>
            <h1>
              <b>Device Registration Management</b>
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
          {username && (
            <InputText
              label={"Username"}
              type={"text"}
              value={username}
              isReadOnly={true}
              enableCopy={true}
              ariaLabel={"input-username"}
              isExpandable={false}
            />
          )}
          <br />
          {password && (
            <InputText
              label={"Password"}
              type={"password"}
              value={password}
              isReadOnly={true}
              ariaLabel={"input-password"}
            />
          )}
          <br />
          <RegistrationApi />
          <br />
          <CredentialApi />
        </CardBody>
      )}
    </Card>
  );
  return <PageSection>{CardToDisplay}</PageSection>;
};

export { DeviceRegistationManagement };
