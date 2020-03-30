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
import { IAdapterConfig } from "modules/project-detail/components";
import { StyleSheet, css } from "@patternfly/react-styles";

interface IDeviceRegistationManagementProps {
  username?: string;
  password?: string;
  registrationApi?: IAdapterConfig;
  credentialApi?: IAdapterConfig;
}

const styles = StyleSheet.create({
  fontSize: { fontSize: 20 }
});

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

  const RegistrationApi = () => {
    const { url, host, port } = registrationApi || {};
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
            id={"registration-api-url-input"}
            ariaLabel={"registration-api-url"}
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
            id={"registration-api-host-input"}
            ariaLabel={"registration-api-host"}
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
            id={"registration-api-port-input"}
            ariaLabel={"registration-api-port"}
            isExpandable={false}
          />
        )}
      </>
    );
  };

  const CredentialApi = () => {
    const { url, host, port } = credentialApi || {};
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
            id={"credential-api-url-input"}
            ariaLabel={"credential-api-url"}
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
            id={"credential-api-host-input"}
            ariaLabel={"credential-api-host"}
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
            id={"credential-api-port-input"}
            ariaLabel={"credential-api-port"}
            isExpandable={false}
          />
        )}
      </>
    );
  };

  const data = {
    device_registry: {
      username: username,
      password: password,
      registrationApi: registrationApi,
      credentialApi: credentialApi
    }
  };

  const CardToDisplay = () => (
    <Card>
      <CardHeader className={css(styles.fontSize)}>
        <Split>
          <SplitItem>
            <h1>
              <b>Device Registration Management</b>
            </h1>
          </SplitItem>
          <SplitItem isFilled />
          <SplitItem>
            <Switch
              id="device-registration-management-switch"
              label="View in Json"
              labelOff="View in Json"
              isChecked={isHidden}
              onChange={onToggle}
            />
          </SplitItem>
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
              id={"username-input"}
              ariaLabel={"username"}
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
              id={"password-input"}
              ariaLabel={"password"}
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

  return (
    <PageSection>
      <CardToDisplay />
    </PageSection>
  );
};

export { DeviceRegistationManagement };
