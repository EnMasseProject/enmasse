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
  ExpandableSection,
  TextArea,
  Grid,
  GridItem,
  Divider
} from "@patternfly/react-core";
import { PasswordInputFieldWithToggle } from "components/PasswordInputFieldWithToggle";
import { StyleSheet, css } from "aphrodite";

const styles = StyleSheet.create({
  card_margin: {
    marginLeft: 40,
    marginRight: 40
  },

  secrets_expandable_view: {
    paddingLeft: 25
  },

  form_width: {
    paddingLeft: 25,
    paddingRight: 75
  },
  form_position: {
    top: -32,
    position: "relative"
  }
});

export const SecretsCredentialsView = () => {
  return (
    <Card className={css(styles.card_margin)}>
      <ExpandableSection toggleText="Secrets">
        <Divider component="div" />
        <ExpandableSection
          className={css(styles.secrets_expandable_view)}
          toggleText="Secrets1"
        >
          <Form className={css(styles.form_width)} isHorizontal={false}>
            <FormGroup
              fieldId="secrets-credential-password-input"
              label="Password"
              isRequired
            >
              <PasswordInputFieldWithToggle id="secrets-credential-password-input" />
            </FormGroup>
            <Grid hasGutter>
              <FormGroup
                isInline={true}
                fieldId="secrets-credential-not-before-dateinput"
                label="Not before"
              >
                <GridItem span={2}>
                  <TextInput
                    id="secrets-credential-not-before-date-input"
                    type="date"
                  />
                </GridItem>

                <GridItem>
                  <TextInput
                    id="secrets-credential-not-before-time-input"
                    type="time"
                  />
                </GridItem>
                <GridItem className={css(styles.form_position)}>
                  <FormGroup
                    isInline={true}
                    fieldId="secrets-credential-not-after-date-input"
                    label="Not after"
                  >
                    <GridItem>
                      <TextInput
                        id="secrets-credential-not-after-date-input"
                        type="date"
                      />
                    </GridItem>
                    <GridItem>
                      <TextInput
                        id="secrets-credential-not-after-time-input"
                        type="time"
                      />
                    </GridItem>
                  </FormGroup>
                </GridItem>
              </FormGroup>
            </Grid>
            <FormGroup
              label="Comments"
              fieldId="secrets-credential-comments-text"
            >
              <TextArea
                label="Comments"
                className={css(styles.form_position)}
                name="Comments"
                id="secrets-credential-comments-text"
              />
            </FormGroup>
          </Form>
        </ExpandableSection>
      </ExpandableSection>
    </Card>
  );
};
