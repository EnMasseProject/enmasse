/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { IMessagingProject } from "./CreateMessagingProject";
import {
  Form,
  FormGroup,
  TextInput,
  GridItem,
  Grid,
  Title,
  Radio
} from "@patternfly/react-core";
import { IAddressSpaceSchema } from "schema/ResponseTypes";
import { IOptionForKeyValueLabel } from "modules/address-space";

interface IConfiguringRoutes {
  projectDetail: IMessagingProject;
  addressSpaceSchema?: IAddressSpaceSchema;
  setProjectDetail: (projectDetail: IMessagingProject) => void;
}

interface ITlsTerrmination {
  portName?: string;
  tlsOptions?: string[];
}
const ConfiguringRoutes: React.FunctionComponent<IConfiguringRoutes> = ({
  projectDetail,
  addressSpaceSchema,
  setProjectDetail
}) => {
  const { hostname, tlsTermination, type, protocols } = projectDetail;

  const getTlsCertificateLabel = (tlsValue: string) => {
    if (tlsValue === "passthrough") {
      return "Pass through";
    } else if (tlsValue === "reencrypt") {
      return "Re-encrypt";
    }
    return tlsValue.toUpperCase();
  };

  const getTlsTerminations = () => {
    let tlsTerminations: ITlsTerrmination[] = [];
    if (addressSpaceSchema?.addressSpaceSchema) {
      addressSpaceSchema.addressSpaceSchema.forEach(as => {
        if (as.metadata.name === type && as.spec.routeServicePorts) {
          as.spec.routeServicePorts.forEach(port => {
            if (
              port.routeTlsTerminations &&
              port.routeTlsTerminations.length > 0
            ) {
              tlsTerminations.push({
                portName: port.name,
                tlsOptions: port.routeTlsTerminations
              });
            }
          });
        }
      });
    }
    let tlsTerminationOptions: IOptionForKeyValueLabel[] = [];
    if (
      tlsTerminations &&
      protocols &&
      tlsTerminations.length > 0 &&
      protocols.length > 0
    ) {
      for (let tlsTermination of tlsTerminations) {
        for (let protocol of protocols) {
          if (
            protocol === tlsTermination.portName &&
            tlsTermination.tlsOptions &&
            tlsTermination.tlsOptions.length > 0
          ) {
            for (let termination of tlsTermination.tlsOptions) {
              if (
                tlsTerminationOptions.findIndex(
                  term => term.value === termination
                ) < 0
              ) {
                tlsTerminationOptions.push({
                  key: `key-${termination}`,
                  label: getTlsCertificateLabel(termination),
                  value: termination
                });
              }
            }
          }
        }
      }
    }
    return tlsTerminationOptions;
  };
  const onChangeHostname = (value: string) => {
    setProjectDetail({ ...projectDetail, hostname: value });
  };

  const onChangeTlsTermination = (_: boolean, event: any) => {
    const value = event.target.value;
    if (value) {
      setProjectDetail({ ...projectDetail, tlsTermination: value });
    }
  };
  return (
    <>
      <Grid>
        <GridItem span={6}>
          <Title headingLevel="h2" size="lg">
            Configure OpenShift Route
          </Title>
          <br />
          <Form>
            <FormGroup
              fieldId="form-group-route-hostname"
              label="Hostname"
              helperText="Public hostname for the route.If not specified, a hostname is generated"
            >
              <TextInput
                type="text"
                id="simple-form-name"
                name="simple-form-name"
                aria-describedby="simple-form-name-helper"
                value={hostname}
                onChange={onChangeHostname}
              />
            </FormGroup>
            <br />
            {getTlsTerminations().length > 0 && (
              <FormGroup
                fieldId="form-group-endpoint-tls-certs"
                label="TLS Certificates"
                isRequired={true}
              >
                {getTlsTerminations().map((termination, index) => (
                  <div key={`key-termination-${index}`}>
                    <br />
                    <Radio
                      isChecked={tlsTermination === termination.value}
                      onChange={onChangeTlsTermination}
                      name={`radio-${termination.key}`}
                      key={termination.key}
                      label={termination.label}
                      id={`radio-${termination.key}`}
                      value={termination.value}
                      style={{ marginLeft: 20 }}
                    />
                  </div>
                ))}
              </FormGroup>
            )}
          </Form>
        </GridItem>
      </Grid>
    </>
  );
};

export { ConfiguringRoutes };
