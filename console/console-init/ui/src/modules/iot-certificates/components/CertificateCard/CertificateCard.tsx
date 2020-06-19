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
      id={`cc-dropdown-edit-${id}`}
      key="edit"
      aria-label="Edit certificate"
      onClick={onEditCertificate}
    >
      Edit
    </DropdownItem>,
    <DropdownItem
      id={`cc-dropdown-delete-${id}`}
      key="delete"
      aria-label="delete"
      onClick={onDeleteCertificate}
    >
      Delete
    </DropdownItem>
  ];

  const rowMargin: string = css(styles.row_margin);

  return (
    <Card id={`cc-card-${id}`}>
      <CardTitle id={`cc-dard-header-${id}`}>
        <CardActions className={css(styles.float_right)}>
          <DropdownWithKebabToggle
            isPlain={true}
            dropdownItems={dropdownItems}
            id={`cc-dropdown-with-kebab-${id}`}
          />
        </CardActions>
      </CardTitle>
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
            <SwitchWithToggle
              id={`cc-auto-provision-switch-${id}`}
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
