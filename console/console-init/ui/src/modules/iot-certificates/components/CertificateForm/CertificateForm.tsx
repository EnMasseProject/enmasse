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
import { css, StyleSheet } from "@patternfly/react-styles";
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
  id: string;
}

export const CertificateForm: React.FunctionComponent<ICertificateFormProps> = ({
  setOnEditMode,
  certificate,
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

  const onCertificateFormDataChange = (
    value: string | boolean,
    event: React.FormEvent<HTMLInputElement>
  ) => {
    const { name: certificateField } = event.currentTarget;
    const newCertificateData = { ...certificateFormData };
    (newCertificateData as any)[certificateField] = value;
    setCertificateFormData(newCertificateData);
  };

  const cancelEdit = () => {
    setOnEditMode(false);
  };

  const saveCertificate = () => {
    // TODO: A mutation to save the certificate and hide form on success
  };

  const {
    ["subject-dn"]: subjectDn,
    ["public-key"]: publicKey,
    ["auto-provisioning-enabled"]: autoProvision,
    algorithm,
    ["not-before"]: notBefore,
    ["not-after"]: notAfter
  } = certificateFormData;

  return (
    <Card id={`cf-card-${id}`}>
      <CardBody>
        <Form>
          <FormGroup
            fieldId={`cf-subjectdn-${id}`}
            isRequired
            label={getLabelByKey("subject-dn")}
          >
            <TextInput
              id={`cf-subjectdn-${id}`}
              type="text"
              name="subject-dn"
              isRequired
              value={subjectDn || ""}
              onChange={onCertificateFormDataChange}
            />
          </FormGroup>
          <FormGroup
            fieldId={`cf-public-key-${id}`}
            isRequired
            label={getLabelByKey("public-key")}
          >
            <TextInput
              id={`cf-public-key-${id}`}
              type="text"
              name="public-key"
              isRequired
              value={publicKey || ""}
              onChange={onCertificateFormDataChange}
            />
          </FormGroup>
          <FormGroup
            fieldId={`cf-algo-${id}`}
            isRequired
            label={getLabelByKey("algorithm")}
            className={css(styles.capitalize)}
          >
            <DropdownWithToggle
              id={`cf-algo-${id}`}
              name="algorithm"
              className={css(styles.dropdown_align)}
              toggleClass={css(styles.dropdown_toggle_align)}
              position={DropdownPosition.left}
              onSelectItem={onCertificateFormDataChange}
              dropdownItems={algorithmTypeOptions}
              value={algorithm || ""}
            />
          </FormGroup>
          <Grid>
            <GridItem span={6}>
              <FormGroup
                fieldId={`cf-not-before-${id}`}
                isRequired
                label={getLabelByKey("not-before")}
              >
                <TextInput
                  id={`cf-not-before-${id}`}
                  name="not-before"
                  isRequired
                  value={notBefore || ""}
                  onChange={onCertificateFormDataChange}
                />
              </FormGroup>
            </GridItem>
            <GridItem span={6}>
              <FormGroup
                fieldId={`cf-not-after-${id}`}
                isRequired
                label={getLabelByKey("not-after")}
              >
                <TextInput
                  id={`cf-not-after-${id}`}
                  name="not-after"
                  isRequired
                  value={notAfter || ""}
                  onChange={onCertificateFormDataChange}
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
                id="cf-auto-provision-switch"
                label="Enabled"
                labelOff="Disabled"
                isChecked={autoProvision || false}
                onChange={onCertificateFormDataChange}
              />
            </SplitItem>
          </Split>
          <ActionGroup>
            <Button
              variant="primary"
              onClick={saveCertificate}
              id="cf-save-button"
            >
              Save
            </Button>
            <Button variant="link" onClick={cancelEdit} id="cf-cancel-button">
              Cancel
            </Button>
          </ActionGroup>
        </Form>
      </CardBody>
    </Card>
  );
};
