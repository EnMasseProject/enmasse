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
  TextVariants
} from "@patternfly/react-core";
import { ToggleOffIcon, ToggleOnIcon } from "@patternfly/react-icons";
import { InputText, JsonViewEditor } from "components";

export interface IConfig {
  url?: string;
  host?: string;
  port?: number;
}
interface IDeviceRegistationManagementProps {
  username?: string;
  password?: string;
  registrationApi?: IConfig;
  credentialApi?: IConfig;
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
  const EditIcon = isHidden ? (
    <ToggleOffIcon onClick={onToggle} size="lg" />
  ) : (
    <ToggleOnIcon onClick={onToggle} size="lg" />
  );
  const RegistrationApi = registrationApi && (
    <>
      <Text component={TextVariants.h2}>Registration API</Text>
      <br />
      {registrationApi.url && (
        <InputText
          label={"Url"}
          type={"text"}
          value={registrationApi.url}
          isReadOnly={true}
          enableCopy={true}
          ariaLabel={"input-registration-api-url"}
          isExpandable={false}
        />
      )}
      <br />
      {registrationApi.host && (
        <InputText
          label={"Host"}
          type={"number"}
          value={registrationApi.host}
          isReadOnly={true}
          enableCopy={true}
          ariaLabel={"input-registration-api-host"}
          isExpandable={false}
        />
      )}
      <br />
      {registrationApi.port && (
        <InputText
          label={"Port"}
          type={"number"}
          value={registrationApi.port}
          isReadOnly={true}
          enableCopy={true}
          ariaLabel={"input-registration-api-port"}
          isExpandable={false}
        />
      )}
    </>
  );
  const CredentialApi = credentialApi && (
    <>
      <Text component={TextVariants.h2}>Credential API</Text>
      <br />
      {credentialApi.url && (
        <InputText
          label={"Url"}
          type={"text"}
          value={credentialApi.url}
          isReadOnly={true}
          enableCopy={true}
          ariaLabel={"input-credential-api-url"}
          isExpandable={false}
        />
      )}
      <br />
      {credentialApi.host && (
        <InputText
          label={"Host"}
          type={"number"}
          value={credentialApi.host}
          isReadOnly={true}
          enableCopy={true}
          ariaLabel={"input-credential-api-host"}
          isExpandable={false}
        />
      )}
      <br />
      {credentialApi.port && (
        <InputText
          label={"Port"}
          type={"number"}
          value={credentialApi.port}
          isReadOnly={true}
          enableCopy={true}
          ariaLabel={"input-credential-api-port"}
          isExpandable={false}
        />
      )}
    </>
  );
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
            {/* <Text component={TextVariants.h1}> */}
            <h1>
              <b>Device Registration Management</b>
            </h1>
            {/* </Text> */}
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
          {RegistrationApi}
          <br />
          {CredentialApi}
        </CardBody>
      )}
    </Card>
  );
  return <PageSection>{CardToDisplay}</PageSection>;
};

export { DeviceRegistationManagement };
