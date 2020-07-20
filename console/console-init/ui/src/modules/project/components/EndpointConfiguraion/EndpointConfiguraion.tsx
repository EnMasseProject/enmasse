/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Form, Checkbox, FormGroup, Radio } from "@patternfly/react-core";
import { IMessagingProject, IRouteConf } from "modules/address-space/dialogs";
import { TlsCertificateType } from "modules/address-space/utils";
import { IAddressSpaceSchema } from "schema/ResponseTypes";
import { IDropdownOption, SwitchWithToggle } from "components";

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
        routesConf: [],
        addRoutes: value
      });
    } else {
      const routes: IRouteConf[] = [];
      projectDetail.protocols?.forEach(protocol => {
        routes.push({
          protocol: protocol,
          hostname: undefined,
          tlsTermination: undefined
        });
      });
      setProjectDetail({
        ...projectDetail,
        addRoutes: value,
        routesConf: routes
      });
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
    const routes: IRouteConf[] = projectDetail.routesConf || [];
    if (protocols) {
      protocolList = JSON.parse(JSON.stringify(protocols));
    }
    if (protocolValue) {
      if (checked) {
        protocolList.push(protocolValue);
        if (projectDetail.addRoutes) routes.push({ protocol: protocolValue });
      } else {
        const index = protocolList.findIndex(pr => pr === protocolValue);
        if (index >= 0) {
          protocolList.splice(index, 1);
        }
        const routeIndex = routes.findIndex(
          route => route.protocol === protocolValue
        );
        if (routeIndex > -1) {
          routes.splice(routeIndex, 1);
        }
      }
    }
    setProjectDetail({
      ...projectDetail,
      protocols: protocolList,
      routesConf: routes
    });
  };
  return (
    <Form>
      {getProtocolOptions() && getProtocolOptions().length > 0 && (
        <FormGroup
          fieldId="endpoint-config-protocol-checkbox"
          label="Protocols"
          isRequired={true}
        >
          {getProtocolOptions().map(protocol => (
            <div key={`key-protocol-${protocol.key}`}>
              <br />
              <Checkbox
                label={protocol.label}
                isChecked={isProtocolPresent(protocol.value)}
                onChange={onProtocolChange}
                value={protocol.value}
                aria-label={`Protocol checkbox to select ${protocol.value}`}
                id={`endpoint-config-protocol-checkbox-${protocol.key}`}
                key={protocol.key}
                name={protocol.key}
              />
            </div>
          ))}
        </FormGroup>
      )}
      {getCertificateOptions() && getCertificateOptions().length > 0 && (
        <FormGroup
          fieldId="endpoint-config-tls-certificate-radio"
          label="TLS Certificates"
          isRequired={true}
        >
          {getCertificateOptions().map(certificate => (
            <div key={`key-cert-${certificate.key}`}>
              <br />
              <Radio
                isChecked={tlsCertificate === certificate.value}
                key={certificate.key}
                onChange={onCertificateChange}
                name={`radio-${certificate.key}`}
                label={certificate.label}
                id={`endpoint-config-tls-certificate-radio-${certificate.key}`}
                value={certificate.value}
              />
            </div>
          ))}
        </FormGroup>
      )}

      <FormGroup
        fieldId="endpoint-config-enable-routing-switchtoggle"
        label="Create Routes"
      >
        <br />
        <SwitchWithToggle
          id="endpoint-config-enable-routing-switchtoggle"
          onChange={onRouteChange}
          label={"Enable routing to make the address space publicly visible"}
          isChecked={projectDetail.addRoutes}
        />
      </FormGroup>
    </Form>
  );
};

export { EndpointConfiguration };
