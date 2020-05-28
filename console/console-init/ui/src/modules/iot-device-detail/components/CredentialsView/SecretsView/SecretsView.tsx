/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Title, Grid, GridItem, Button } from "@patternfly/react-core";
import { EditAltIcon } from "@patternfly/react-icons";
import classNames from "classnames";
import { getLabelByKey } from "utils";
import { ISecret } from "modules/iot-device/components";
import { StyleSheet } from "@patternfly/react-styles";
import { PasswordLabel } from "components";

const styles = StyleSheet.create({
  row_margin: {
    marginBottom: 5
  },
  section_margin: {
    marginTop: 20,
    marginBottom: 10
  },
  c_button_PaddingLeft: {
    paddingLeft: 0
  },
  c_button_PaddingBottom: {
    paddingTop: 0
  }
});

export interface ISecretsViewProps {
  id: string;
  secrets: Omit<ISecret, "id">[];
  heading: string;
}

const SecretRow: React.FC<{ secret: ISecret }> = ({ secret }) => {
  const renderGridItemValue = (value: string, key: string) => {
    if (key === "pwd-hash") {
      return (
        <Button
          variant="link"
          icon={<EditAltIcon />}
          className={classNames([
            styles.c_button_PaddingLeft,
            styles.c_button_PaddingBottom
          ])}
        >
          Change password
        </Button>
      );
    } else if (key === "key") {
      return <PasswordLabel id="sv-key-password-lebel" value={value} />;
    }
    return value;
  };

  const secretsKeys = Object.keys(secret);
  return (
    <>
      {secretsKeys.map((key: string) => {
        const value = secret && (secret as any)[key];
        return (
          <>
            <Grid key={"secrets-view-" + key}>
              <GridItem span={3}>
                <Title headingLevel="h1" size="md">
                  <b>{getLabelByKey(key)}</b>
                </Title>
              </GridItem>
              <GridItem span={9} className={styles.row_margin}>
                {renderGridItemValue(value, key)}
              </GridItem>
            </Grid>
          </>
        );
      })}
    </>
  );
};

export const SecretsView: React.FC<ISecretsViewProps> = ({
  id,
  secrets,
  heading
}) => {
  return (
    <>
      {secrets && secrets.length > 0 && (
        <Grid id={id}>
          <GridItem span={12} className={styles.section_margin}>
            {heading}
          </GridItem>
          {secrets.map((secret: ISecret, index: number) => (
            <>
              <SecretRow secret={secret} />
              {index < secrets.length - 1 && <br />}
            </>
          ))}
        </Grid>
      )}
    </>
  );
};
