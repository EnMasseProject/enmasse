/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Card,
  CardActions,
  CardHeader,
  GridItem,
  CardBody,
  Grid,
  Title,
  Switch,
  DropdownItem
} from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";
import { DropdownWithKebabToggle } from "components";
import { getLabelByKey } from "utils";
import { IProjectCertificate } from "modules/iot-certificates";

export interface ICertificateCardProps {
  certificate: IProjectCertificate;
  setOnEditMode: React.Dispatch<React.SetStateAction<boolean>>;
  id: string;
}

const styles = StyleSheet.create({
  row_margin: {
    marginBottom: 5
  },
  float_right: {
    float: "right"
  },
  capitalize: {
    textTransform: "capitalize"
  }
});

export const CertificateCard: React.FunctionComponent<ICertificateCardProps> = ({
  certificate,
  setOnEditMode,
  id
}) => {
  const handleEditCertificate = () => {
    setOnEditMode(true);
  };

  const handleDeleteCertificate = () => {
    // TODO: Mutation to delete the certificate
  };

  const dropdownItems = [
    <DropdownItem
      id={`cc-dropdown-edit-${id}`}
      key="edit"
      aria-label="Edit certificate"
      onClick={handleEditCertificate}
    >
      Edit
    </DropdownItem>,
    <DropdownItem
      id={`cc-dropdown-delete-${id}`}
      key="delete"
      aria-label="delete"
      onClick={handleDeleteCertificate}
    >
      Delete
    </DropdownItem>
  ];

  const rowMargin: string = css(styles.row_margin);

  return (
    <Card id={`cc-card-${id}`}>
      <CardHeader id={`cc-dard-header-${id}`}>
        <CardActions className={css(styles.float_right)}>
          <DropdownWithKebabToggle
            isPlain={true}
            dropdownItems={dropdownItems}
            id={`cc-dropdown-with-kebab-${id}`}
          />
        </CardActions>
      </CardHeader>
      <CardBody>
        <Grid>
          <GridItem span={2}>
            <Title headingLevel="h1" size="md" id={`cc-subject-dn-title-${id}`}>
              <b>{getLabelByKey("subject-dn")}</b>
            </Title>
          </GridItem>
          <GridItem span={10} className={rowMargin}>
            {certificate["subject-dn"]}
          </GridItem>
          <GridItem span={2}>
            <Title headingLevel="h1" size="md" id={`cc-public-key-title-${id}`}>
              <b>{getLabelByKey("public-key")}</b>
            </Title>
          </GridItem>
          <GridItem span={10} className={rowMargin}>
            {certificate["public-key"]}
          </GridItem>
          <GridItem span={2}>
            <Title
              headingLevel="h1"
              size="md"
              id={`cc-auto-provision-title-${id}`}
            >
              <b>{getLabelByKey("auto-provisioning-enabled")}</b>
            </Title>
          </GridItem>
          <GridItem span={10} className={rowMargin}>
            <Switch
              id={`cc-auto-provision-switch-${id}`}
              label="Enabled"
              labelOff="Disabled"
              readOnly
              isChecked={certificate["auto-provisioning-enabled"]}
            />
          </GridItem>
          <GridItem span={2}>
            <Title
              headingLevel="h1"
              size="md"
              id={`cc-algorithm-title-${id}`}
              className={css(styles.capitalize)}
            >
              <b>{getLabelByKey("algorithm")}</b>
            </Title>
          </GridItem>
          <GridItem span={10} className={rowMargin}>
            {certificate["algorithm"]}
          </GridItem>
          <GridItem span={2}>
            <Title headingLevel="h1" size="md" id={`cc-not-before-title-${id}`}>
              <b>{getLabelByKey("not-before")}</b>
            </Title>
          </GridItem>
          <GridItem span={10} className={rowMargin}>
            {certificate["not-before"]}
          </GridItem>
          <GridItem span={2}>
            <Title headingLevel="h1" size="md" id={`cc-not-after-title-${id}`}>
              <b>{getLabelByKey("not-after")}</b>
            </Title>
          </GridItem>
          <GridItem span={10} className={rowMargin}>
            {certificate["not-after"]}
          </GridItem>
        </Grid>
      </CardBody>
    </Card>
  );
};
