/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  Card,
  CardBody,
  Form,
  FormGroup,
  TextInput,
  Grid,
  GridItem,
  Split,
  SplitItem,
  ActionGroup,
  Button,
  DropdownPosition
} from "@patternfly/react-core";
import { DropdownWithToggle, SwitchWithToggle } from "components";
import { StyleSheet, css } from "aphrodite";
import { getLabelByKey } from "utils";
import {
  algorithmTypeOptions,
  IIoTCertificate
} from "modules/iot-certificates";

const styles = StyleSheet.create({
  dropdown_align: {
    display: "flex"
  },
  dropdown_toggle_align: {
    flex: "1"
  },
  capitalize: {
    textTransform: "capitalize"
  }
});

export interface ICertificateFormProps {
  certificate?: IIoTCertificate;
  setOnEditMode: React.Dispatch<React.SetStateAction<boolean>>;
  onSave?: (certificate: IIoTCertificate) => void;
  id: string;
}

export const CertificateForm: React.FunctionComponent<ICertificateFormProps> = ({
  setOnEditMode,
  certificate,
  onSave,
  id
}) => {
  const initialFormState: IIoTCertificate = {
    "subject-dn": "",
    "public-key": "",
    "auto-provisioning-enabled": false,
    algorithm: "RSA",
    "not-before": "",
    "not-after": ""
  };

  const [certificateFormData, setCertificateFormData] = useState<
    IIoTCertificate
  >(initialFormState);

  useEffect(() => {
    // code to initialize the form if it is being used for edit
    certificate && setCertificateFormData(certificate);
  }, [certificate]);

  const onChangeCertificateFormData = (
    value: string | boolean,
    event: React.FormEvent<HTMLInputElement>
  ) => {
    const { name: certificateField } = event.currentTarget;
    const newCertificateData = { ...certificateFormData };
    (newCertificateData as any)[certificateField] = value;
    setCertificateFormData(newCertificateData);
  };

  const onSelectAlgorithm = (value: string) => {
    setCertificateFormData({ ...certificateFormData, algorithm: value });
  };

  const onCancelEdit = () => {
    setOnEditMode(false);
  };

  const onSaveCertificate = () => {
    onSave && onSave(certificateFormData);
  };

  const onChangeStatus = (
    value: boolean,
    _: React.FormEvent<HTMLInputElement>
  ) => {
    setCertificateFormData({
      ...certificateFormData,
      "auto-provisioning-enabled": value
    });
  };

  const {
    "subject-dn": subjectDn,
    "public-key": publicKey,
    "auto-provisioning-enabled": autoProvision,
    algorithm,
    "not-before": notBefore,
    "not-after": notAfter
  } = certificateFormData;

  return (
    <Card id={`cf-card-${id}`}>
      <CardBody>
        <Form>
          <FormGroup
            fieldId={`cert-form-subjectdn-${id}`}
            isRequired
            label={getLabelByKey("subject-dn")}
          >
            <TextInput
              id={`cert-form-subjectdn-${id}`}
              type="text"
              name="subject-dn"
              isRequired
              value={subjectDn || ""}
              onChange={onChangeCertificateFormData}
            />
          </FormGroup>
          <FormGroup
            fieldId={`cert-form-public-key-text-${id}`}
            isRequired
            label={getLabelByKey("public-key")}
          >
            <TextInput
              id={`cert-form-public-key-text-${id}`}
              aria-label="Text input for public key"
              type="text"
              name="public-key"
              isRequired
              value={publicKey || ""}
              onChange={onChangeCertificateFormData}
            />
          </FormGroup>
          <FormGroup
            fieldId={`cert-form-algo-dropdown-${id}`}
            isRequired
            label={getLabelByKey("algorithm")}
            className={css(styles.capitalize)}
          >
            <DropdownWithToggle
              id={`cert-form-algo-dropdown-${id}`}
              aria-label="Dropdown for selecting algorithm"
              name="algorithm"
              className={css(styles.dropdown_align)}
              toggleClass={css(styles.dropdown_toggle_align)}
              position={DropdownPosition.left}
              onSelectItem={onSelectAlgorithm}
              dropdownItems={algorithmTypeOptions}
              value={algorithm || ""}
            />
          </FormGroup>
          <Grid>
            <GridItem span={6}>
              <FormGroup
                fieldId={`cert-form-not-before-text-${id}`}
                isRequired
                label={getLabelByKey("not-before")}
              >
                <TextInput
                  id={`cert-form-not-before-text-${id}`}
                  name="not-before"
                  isRequired
                  value={notBefore || ""}
                  onChange={onChangeCertificateFormData}
                />
              </FormGroup>
            </GridItem>
            <GridItem span={6}>
              <FormGroup
                fieldId={`cert-form-not-after-text-${id}`}
                isRequired
                label={getLabelByKey("not-after")}
              >
                <TextInput
                  id={`cert-form-not-after-text-${id}`}
                  name="not-after"
                  isRequired
                  value={notAfter || ""}
                  onChange={onChangeCertificateFormData}
                />
              </FormGroup>
            </GridItem>
          </Grid>
          <Split>
            <SplitItem>
              <h1>
                <b>{getLabelByKey("auto-provisioning-enabled")}</b>
              </h1>
            </SplitItem>
            <SplitItem isFilled />
            <SplitItem>
              <SwitchWithToggle
                id="cert-form-auto-provision-switch"
                aria-label="switch to enable auto provision"
                label="Enabled"
                labelOff="Disabled"
                isChecked={autoProvision || false}
                onChange={onChangeStatus}
              />
            </SplitItem>
          </Split>
          <ActionGroup>
            <Button
              variant="primary"
              onClick={onSaveCertificate}
              id="cert-form-save-button"
              aria-label="save button"
            >
              Save
            </Button>
            <Button
              variant="link"
              onClick={onCancelEdit}
              id="cert-form-cancel-button"
              aria-label="cancel button"
            >
              Cancel
            </Button>
          </ActionGroup>
        </Form>
      </CardBody>
    </Card>
  );
};
