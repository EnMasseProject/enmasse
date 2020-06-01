/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Grid,
  GridItem,
  Form,
  Checkbox,
  FormGroup,
  Radio,
  Switch
} from "@patternfly/react-core";
import { IMessagingProject } from "modules/address-space/dialogs";
import { TlsCertificateType } from "modules/address-space/utils";
import { StyleSheet, css } from "@patternfly/react-styles";
import { IAddressSpaceSchema } from "schema/ResponseTypes";
import { IDropdownOption } from "components";

const style = StyleSheet.create({
  margin_left: {
    marginLeft: 20
  }
});
interface IEndpointConfigurationProps {
  projectDetail: IMessagingProject;
  addressSpaceSchema?: IAddressSpaceSchema;
  setProjectDetail: (project: IMessagingProject) => void;
}
const EndpointConfiguration: React.FunctionComponent<IEndpointConfigurationProps> = ({
  projectDetail,
  addressSpaceSchema,
  setProjectDetail
}) => {
  const { tlsCertificate, protocols, type } = projectDetail;
  const getProtocolOptions = () => {
    let protocols: IDropdownOption[] = [];
    if (addressSpaceSchema?.addressSpaceSchema) {
      addressSpaceSchema.addressSpaceSchema.forEach(as => {
        if (as.metadata.name === type && as.spec.routeServicePorts) {
          protocols = as.spec.routeServicePorts.map(port => ({
            value: port.name || "",
            label: port.displayName || "",
            key: `key-${port.name}`
          }));
        }
      });
    }
    return protocols;
  };
  const getCertificateOptions = () => {
    let certificateOptions: IDropdownOption[] = [];
    if (addressSpaceSchema?.addressSpaceSchema) {
      addressSpaceSchema.addressSpaceSchema.forEach(as => {
        if (as.metadata.name === type && as.spec.certificateProviderTypes) {
          certificateOptions = as.spec.certificateProviderTypes.map(cert => ({
            value: cert.name || "",
            label: cert.description || "",
            key: `key-${cert.name}`
          }));
        }
      });
    }
    return certificateOptions;
  };

  const onRouteChange = (value: boolean) => {
    if (!value) {
      setProjectDetail({
        ...projectDetail,
        tlsTermination: undefined,
        hostname: undefined,
        addRoutes: value
      });
    } else {
      setProjectDetail({ ...projectDetail, addRoutes: value });
    }
  };
  const onCertificateChange = (_: boolean, event: any) => {
    const value = event.target.value;
    if (value) {
      setProjectDetail({
        ...projectDetail,
        tlsCertificate: value,
        addCertificate: value === TlsCertificateType.UPLOAD_CERT ? true : false
      });
    }
  };
  const findProtocol = (protocol: string) => {
    if (protocols) {
      const filteredProtocols = protocols.filter(pr => pr === protocol);
      return filteredProtocols[0];
    }
  };
  const isProtocolPresent = (protocol: string) => {
    if (protocols && findProtocol(protocol)) {
      return true;
    }
    return false;
  };
  const onProtocolChange = (checked: boolean, event: any) => {
    const protocolValue = event.target.value;
    let protocolList: string[] = [];
    if (protocols) {
      protocolList = JSON.parse(JSON.stringify(protocols));
    }
    if (protocolValue) {
      if (checked) {
        protocolList.push(protocolValue);
      } else {
        const index = protocolList.findIndex(pr => pr === protocolValue);
        if (index >= 0) {
          protocolList.splice(index, 1);
        }
      }
    }
    setProjectDetail({ ...projectDetail, protocols: protocolList });
  };
  return (
    <Grid>
      <GridItem span={6}>
        <Form>
          {getProtocolOptions() && getProtocolOptions().length > 0 && (
            <FormGroup
              fieldId="form-group-endpoint-protocol"
              label="Protocols"
              isRequired={true}
            >
              {getProtocolOptions().map(protocol => (
                <>
                  <br />
                  <Checkbox
                    label={protocol.label}
                    isChecked={isProtocolPresent(protocol.value)}
                    onChange={onProtocolChange}
                    value={protocol.value}
                    aria-label={`Protocol checkbox to select ${protocol.value}`}
                    id={`checkbox-${protocol.key}`}
                    key={protocol.key}
                    name={protocol.key}
                    className={css(style.margin_left)}
                  />
                </>
              ))}
            </FormGroup>
          )}
          {getCertificateOptions() && getCertificateOptions().length > 0 && (
            <FormGroup
              fieldId="form-group-endpoint-tls-certs"
              label="TLS Certificates"
              isRequired={true}
            >
              {getCertificateOptions().map(certificate => (
                <>
                  <br />
                  <Radio
                    isChecked={tlsCertificate === certificate.value}
                    key={certificate.key}
                    onChange={onCertificateChange}
                    name={`radio-${certificate.key}`}
                    label={certificate.label}
                    id={`radio-${certificate.key}`}
                    value={certificate.value}
                    className={css(style.margin_left)}
                  />
                </>
              ))}
            </FormGroup>
          )}

          <FormGroup fieldId="form-group-create-routes" label="Create Routes">
            <br />
            <Switch
              id="switch-configure-route-btn"
              label={
                "Enable routing to make the address space publicly visible"
              }
              isChecked={projectDetail.addRoutes}
              onChange={onRouteChange}
            />
          </FormGroup>
        </Form>
      </GridItem>
    </Grid>
  );
};

export { EndpointConfiguration };
