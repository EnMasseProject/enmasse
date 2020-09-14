/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  PageSection,
  PageSectionVariants,
  Title
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { CredentialsAdvancedView } from "modules/iot-device/components/CredentialView/CredentialsAdvancedView";
import { SwitchWithToggle } from "/home/suyash/Project/enmasse/console/console-init/ui/src/components/SwitchWithToggle/SwitchWithToggle";
import {
  Form,
  FormGroup,
  TextInput,
  Card,
  CardBody,
  Button,
  CardTitle
} from "@patternfly/react-core";
import { PasswordInputFieldWithToggle } from "components";
import { PlusCircleIcon } from "@patternfly/react-icons";

const styles = StyleSheet.create({
  page_padding: {
    paddingLeft: 150,
    paddingRight: 150
  },

  form_width: {
    maxWidth: "50%"
  },
  margin_top: {
    marginTop: 20
  }
});

export const CredentialView = () => {
  const [advancedSettings, setAdvancedSettings] = useState<boolean>(false);

  const onToggleSwitch = (
    value: boolean,
    _: React.FormEvent<HTMLInputElement>
  ) => {
    setAdvancedSettings(value);
  };

  return (
    <PageSection
      variant={PageSectionVariants.light}
      className={css(styles.page_padding)}
    >
      <Title headingLevel="h1">Add Credentials to this Device</Title>
      <br />
      <br />

      <SwitchWithToggle
        id="credential-view-advancedSettings-switch"
        labelOff={"Show Advanced Settings"}
        onChange={onToggleSwitch}
        label={"Show Advanced Settings"}
      />
      <br />
      <br />
      {advancedSettings && (
        <>
          <CredentialsAdvancedView />
          <Card className={css(styles.margin_top)}>
            <CardBody>
              <Button
                id="credential-list-add-more-credential-button"
                variant="link"
                type="button"
                icon={<PlusCircleIcon />}
              >
                Add more credentials
              </Button>
            </CardBody>
          </Card>
        </>
      )}
      {!advancedSettings && (
        <Form className={css(styles.form_width)}>
          <FormGroup
            fieldId="credential-view-Id-input"
            label="Auth-id"
            isRequired
          >
            <TextInput type="text" id="credential-view-id-input" />
          </FormGroup>
          <FormGroup
            fieldId="credential-view-password-input"
            label="Password"
            isRequired
          >
            <PasswordInputFieldWithToggle id="credential-view-password-input" />
          </FormGroup>
        </Form>
      )}
    </PageSection>
  );
};
