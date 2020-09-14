/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React from "react";
import {
  Form,
  FormGroup,
  TextInput,
  Card,
  Page,
  Flex,
  Button,
  ExpandableSection,
  Divider,
  FlexItem
} from "@patternfly/react-core";
import {
  ExtensionView,
  SecretsCredentialsView
} from "modules/iot-device/components/CredentialView";
import { StyleSheet, css } from "aphrodite";
import { DropdownWithToggle, SwitchWithToggle } from "components";
import { credentialTypeOptions } from "modules/iot-device/utils";
import { TrashIcon } from "@patternfly/react-icons";

const styles = StyleSheet.create({
  card_margin: {
    margin: 40,
    padding: 40
  },
  flex_view: {
    position: "absolute",
    right: 350,
    top: 150
  },
  dropdown_toggle_align: {
    minWidth: "100%"
  }
});

export const CredentialsAdvancedView = () => {
  return (
    <Card>
      <ExpandableSection toggleText={"Credentials 1"}>
        <Divider component="div" />
        <Page>
          <Card
            id="credentials-advanced-card"
            className={css(styles.card_margin)}
          >
            <Form>
              <FormGroup
                fieldId="credentials-advanced-type-dropdown"
                isRequired
                label="Credential Type"
              >
                <DropdownWithToggle
                  dropdownItems={credentialTypeOptions}
                  className={css(styles.dropdown_toggle_align)}
                  toggleClass={css(styles.dropdown_toggle_align)}
                  id="credentials-advanced-type-dropdown"
                  toggleId="credentials-advanced-type-dropdowntoggle"
                />
              </FormGroup>
              <FormGroup
                fieldId="credentials-advanced-id-input"
                isRequired
                label="Auth ID"
              >
                <TextInput type="text" id="credentials-advanced-id-input" />
              </FormGroup>
            </Form>
          </Card>
          <br />
          <SecretsCredentialsView />
          <br />
          <br />

          <ExtensionView />
        </Page>
      </ExpandableSection>
      <Flex>
        <FlexItem span={6} align={{ default: "alignRight" }}>
          <Button variant="link" id="credentials-advanced-delete-button">
            Delete
            <TrashIcon />
          </Button>
          &nbsp; &nbsp; &emsp;
          <SwitchWithToggle
            id="enable-credentials-switch"
            labelOff={"Disabled"}
            label={"Enabled"}
          />
        </FlexItem>
      </Flex>
    </Card>
  );
};
