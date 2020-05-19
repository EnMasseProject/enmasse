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
import {
  tlsCertificateOptions,
  endpointProtocolOptions,
  TlsCertificateType
} from "modules/address-space/utils";
import { StyleSheet, css } from "@patternfly/react-styles";

const style = StyleSheet.create({
  margin_left: {
    marginLeft: 20
  }
});
interface IEndpointConfigurationProps {
  projectDetail: IMessagingProject;
  setProjectDetail: (project: IMessagingProject) => void;
}
const EndpointConfiguration: React.FunctionComponent<IEndpointConfigurationProps> = ({
  projectDetail,
  setProjectDetail
}) => {
  const { tlsCertificate, protocols } = projectDetail;
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
      if (value === TlsCertificateType.UPLOAD_CERT) {
        setProjectDetail({
          ...projectDetail,
          tlsCertificate: value,
          addCertificate: true
        });
      } else {
        setProjectDetail({
          ...projectDetail,
          tlsCertificate: value,
          addCertificate: false
        });
      }
    }
  };
  const findPrototcol = (protocol: string) => {
    if (protocols) {
      const filteredProtocols = protocols.filter(pr => pr === protocol);
      return filteredProtocols[0];
    }
  };
  const isProtocolPresent = (protocol: string) => {
    if (protocols) {
      if (findPrototcol(protocol)) {
        return true;
      }
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
          <FormGroup
            fieldId="form-group-endpoint-protocol"
            label="Protocol"
            isRequired={true}
          >
            {endpointProtocolOptions.map(protocol => (
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
          <FormGroup
            fieldId="form-group-endpoint-tls-certs"
            label="TLS Certificates"
            isRequired={true}
          >
            {tlsCertificateOptions.map(certificate => (
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
