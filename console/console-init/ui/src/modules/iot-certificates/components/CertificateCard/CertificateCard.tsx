/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Card,
  CardActions,
  GridItem,
  CardBody,
  Grid,
  Title,
  DropdownItem,
  CardTitle
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { DropdownWithKebabToggle, SwitchWithToggle } from "components";
import { getLabelByKey } from "utils";
import { IIoTCertificate } from "modules/iot-certificates";

export interface ICertificateCardProps {
  certificate: IIoTCertificate;
  setOnEditMode: React.Dispatch<React.SetStateAction<boolean>>;
  onChangeStatus: (certificate: IIoTCertificate, isEnabled: boolean) => void;
  onDelete: (certifiacte: IIoTCertificate) => void;
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
  onDelete,
  onChangeStatus,
  id
}) => {
  const onEditCertificate = () => {
    setOnEditMode(true);
  };

  const onDeleteCertificate = () => {
    onDelete(certificate);
  };
  const onEnableChange = (
    value: boolean,
    _: React.FormEvent<HTMLInputElement>
  ) => {
    onChangeStatus(certificate, value);
  };

  const dropdownItems = [
    <DropdownItem
      id={`cert-card-edit-dropdown-${id}`}
      key="edit"
      aria-label="Edit certificate"
      onClick={onEditCertificate}
    >
      Edit
    </DropdownItem>,
    <DropdownItem
      id={`cert-card-delete-dropdown-${id}`}
      key="delete"
      aria-label="delete certificate"
      onClick={onDeleteCertificate}
    >
      Delete
    </DropdownItem>
  ];

  const rowMargin: string = css(styles.row_margin);

  return (
    <Card id={`cert-card-${id}`}>
      <CardTitle id={`cert-card-title-${id}`}>
        <CardActions className={css(styles.float_right)}>
          <DropdownWithKebabToggle
            isPlain={true}
            dropdownItems={dropdownItems}
            id={`cert-card-kebab-dropdown-${id}`}
          />
        </CardActions>
      </CardTitle>
      <CardBody>
        <Grid>
          <GridItem span={2}>
            <Title
              headingLevel="h1"
              size="md"
              id={`cert-card-subject-title-${id}`}
            >
              <b>{getLabelByKey("subject-dn")}</b>
            </Title>
          </GridItem>
          <GridItem span={10} className={rowMargin}>
            {certificate["subject-dn"]}
          </GridItem>
          <GridItem span={2}>
            <Title
              headingLevel="h1"
              size="md"
              id={`cert-card-public-key-title-${id}`}
            >
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
              id={`cert-card-auto-provision-title-${id}`}
            >
              <b>{getLabelByKey("auto-provisioning-enabled")}</b>
            </Title>
          </GridItem>
          <GridItem span={10} className={rowMargin}>
            <SwitchWithToggle
              id={`cert-card-auto-provision-switch-${id}`}
              aria-label="Toggle to enable auto provisioning"
              label="Enabled"
              labelOff="Disabled"
              isChecked={certificate["auto-provisioning-enabled"] || false}
              onChange={onEnableChange}
            />
          </GridItem>
          <GridItem span={2}>
            <Title
              headingLevel="h1"
              size="md"
              id={`cert-card-algorithm-title-${id}`}
              className={css(styles.capitalize)}
            >
              <b>{getLabelByKey("algorithm")}</b>
            </Title>
          </GridItem>
          <GridItem span={10} className={rowMargin}>
            {certificate["algorithm"]}
          </GridItem>
          <GridItem span={2}>
            <Title
              headingLevel="h1"
              size="md"
              id={`cert-card-not-before-title-${id}`}
            >
              <b>{getLabelByKey("not-before")}</b>
            </Title>
          </GridItem>
          <GridItem span={10} className={rowMargin}>
            {certificate["not-before"]}
          </GridItem>
          <GridItem span={2}>
            <Title
              headingLevel="h1"
              size="md"
              id={`cert-card-not-after-title-${id}`}
            >
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
