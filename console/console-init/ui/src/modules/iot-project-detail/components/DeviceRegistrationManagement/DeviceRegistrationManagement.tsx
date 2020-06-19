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
import {
  InputText,
  JsonEditor,
  SwitchWithToggle,
  IAdapterConfig
} from "components";
import {} from "@patternfly/react-styles";

interface IDeviceRegistationManagementProps {
  token?: string;
  endpoiuntUrl?: string;
  username?: string;
  password?: string;
  registrationApi?: IAdapterConfig;
  credentialApi?: IAdapterConfig;
}

// const styles = StyleSheet.create({
//   fontSize: { fontSize: 20 }
// });

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
      {/* <CardTitle className={css(styles.fontSize)}> */}
      <CardTitle>
        <Split>
          <SplitItem>
            <Title size="xl" headingLevel="h2">
              Device Registration Management
            </Title>
          </SplitItem>
          <SplitItem isFilled />
          <SplitItem>
            <SwitchWithToggle
              id="device-registration-management-switch"
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
          <JsonEditor readOnly={true} value={JSON.stringify(data)} />
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
                id={"username-input"}
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
                id={"password-input"}
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
                id={"drm-input-token"}
                ariaLabel={"token"}
              />
            </>
          )}
          {endpoiuntUrl && (
            <>
              <br />
              <Title size="xl" headingLevel="h2">
                Endpoint
              </Title>
              <br />
              <InputText
                label={"URL"}
                type={"text"}
                value={endpoiuntUrl}
                isReadOnly={true}
                enableCopy={true}
                id={"drm-input-endpoiunt-url"}
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
