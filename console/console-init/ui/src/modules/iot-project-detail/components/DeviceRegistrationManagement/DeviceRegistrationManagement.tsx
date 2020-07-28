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
  Text,
  TextVariants,
  Title,
  CardTitle
} from "@patternfly/react-core";
import { InputText, JsonEditor, SwitchWithToggle, IAdapter } from "components";
import { StyleSheet, css } from "aphrodite";

interface IDeviceRegistationManagementProps {
  token?: string;
  endpoiuntUrl?: string;
  username?: string;
  password?: string;
  registrationApi?: IAdapter;
  credentialApi?: IAdapter;
}

const styles = StyleSheet.create({
  fontSize: { fontSize: 20 }
});

const DeviceRegistationManagement: React.FunctionComponent<IDeviceRegistationManagementProps> = ({
  token,
  endpoiuntUrl,
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
            id={"device-reg-api-url-input"}
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
            id={"device-reg-api-host-input"}
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
            id={"device-reg-api-port-input"}
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
            id={"device-reg-credential-api-url-input"}
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
            id={"device-reg-credential-api-host-input"}
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
            id={"device-reg-credential-api-port-input"}
            ariaLabel={"credential-api-port"}
            isExpandable={false}
          />
        )}
      </>
    );
  };

  const data = {
    device_registry: {
      // TODO: modify after integration
      token: token,
      endpoint: {
        url: endpoiuntUrl
      }
    }
  };

  const CardToDisplay = () => (
    <Card>
      <CardTitle className={css(styles.fontSize)}>
        <Split>
          <SplitItem>
            <Title size="xl" headingLevel="h2" id="device-reg-management-title">
              Device Registration Management
            </Title>
          </SplitItem>
          <SplitItem isFilled />
          <SplitItem>
            <SwitchWithToggle
              id="device-reg-view-json-switch"
              aria-label="Switch to view Json format"
              label="View JSON format"
              labelOff="View JSON format"
              isChecked={isHidden}
              onChange={onToggle}
            />
          </SplitItem>
        </Split>
      </CardTitle>
      {isHidden ? (
        <CardBody>
          <JsonEditor
            readOnly={true}
            value={JSON.stringify(data, undefined, 2)}
          />
        </CardBody>
      ) : (
        <CardBody>
          {username && (
            <>
              <br />
              <InputText
                label={"Username"}
                type={"text"}
                value={username}
                isReadOnly={true}
                enableCopy={true}
                id={"device-reg-username-input"}
                ariaLabel={"username"}
                isExpandable={false}
              />
            </>
          )}
          {password && (
            <>
              <br />
              <InputText
                label={"Password"}
                type={"password"}
                value={password}
                isReadOnly={true}
                id={"device-reg-password-input"}
                ariaLabel={"password"}
              />
            </>
          )}
          {token && (
            <>
              <br />
              <InputText
                label={"Token"}
                type={"password"}
                value={token}
                isReadOnly={true}
                id={"device-reg-input-token"}
                ariaLabel={"token"}
              />
            </>
          )}
          {endpoiuntUrl && (
            <>
              <br />
              <Title size="xl" headingLevel="h2" id="device-reg-endpoint-title">
                Endpoint
              </Title>
              <br />
              <InputText
                label={"URL"}
                type={"text"}
                value={endpoiuntUrl}
                isReadOnly={true}
                enableCopy={true}
                id={"device-reg-endpoint-url-input"}
                ariaLabel={"endpoiunt url"}
              />
            </>
          )}
          {registrationApi && (
            <>
              <br />
              <RegistrationApi />
            </>
          )}
          {credentialApi && (
            <>
              <br />
              <CredentialApi />
            </>
          )}
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
