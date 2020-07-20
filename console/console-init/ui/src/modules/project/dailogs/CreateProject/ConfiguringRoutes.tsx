/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { IMessagingProject } from "./CreateProject";
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
import { IDropdownOption } from "components";

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
  const { type, protocols, routesConf } = projectDetail;

  const getTlsCertificateLabel = (tlsValue: string) => {
    if (tlsValue === "passthrough") {
      return "Pass through";
    } else if (tlsValue === "reencrypt") {
      return "Re-encrypt";
    }
    return tlsValue.toUpperCase();
  };

  const getTlsTerminations = (protocol?: string) => {
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
    if (protocol) {
      tlsTerminations = tlsTerminations.filter(
        tls => tls.portName === protocol
      );
    }
    if (routesConf && routesConf.length > 0) {
      for (let tls of tlsTerminations) {
        for (let route of routesConf) {
          if (route.protocol === tls.portName && tls.tlsOptions?.length === 1) {
            if (!route.tlsTermination) {
              const routes = routesConf;
              const index = routesConf.findIndex(
                route => route.protocol === tls.portName
              );
              routes[index] = {
                protocol: route.protocol,
                tlsTermination: tls.tlsOptions[0]
              };
              setProjectDetail({ ...projectDetail, routesConf: routes });
            }
          }
        }
      }
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
  const onChangeHostname = (protocol: string, value?: string) => {
    const routes = projectDetail.routesConf;
    routes?.forEach(route => {
      if (route.protocol === protocol) {
        route.hostname = value;
      }
    });
    setProjectDetail({ ...projectDetail, routesConf: routes });
  };

  const onChangeTlsTermination = (protocol: string, event: any) => {
    const value = event.target.value;
    if (value) {
      const routes = projectDetail.routesConf || [];
      routes.forEach(route => {
        if (route.protocol === protocol) {
          route.tlsTermination = value;
        }
      });
      setProjectDetail({ ...projectDetail, routesConf: routes });
    }
  };

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

  const getLabelForPrototcol = (protocol: string) => {
    const protocolOptions = getProtocolOptions();
    if (protocolOptions && protocolOptions.length > 0) {
      return protocolOptions.filter(prtcl => prtcl.value === protocol)[0]
        ?.label;
    }
  };

  return (
    <>
      <Grid>
        <GridItem span={6}>
          <Title headingLevel="h1" size="xl">
            Configure OpenShift Route
          </Title>
          <br />
          <Form>
            {protocols &&
              protocols.map(protocol => {
                const routes = routesConf?.filter(
                  route => route.protocol === protocol
                );
                let hostname: string | undefined,
                  tlsTermination: string | undefined;
                if (routes && routes.length > 0) {
                  hostname = routes[0].hostname;
                  tlsTermination = routes[0].tlsTermination;
                }
                return (
                  <div key={`key-protocol-${protocol}`}>
                    <Title headingLevel="h4" size="lg">
                      Route for {getLabelForPrototcol(protocol)}
                    </Title>
                    <br />
                    <FormGroup
                      fieldId={`configuring-routes-hostname-input-${protocol}`}
                      label="Hostname"
                      helperText="Public hostname for the route.If not specified, a hostname is generated"
                    >
                      <TextInput
                        type="text"
                        id={`configuring-routes-hostname-input-${protocol}`}
                        name="text-input-hostname"
                        aria-describedby="text-input-hostname-helper"
                        value={hostname}
                        onChange={(value, _) => {
                          onChangeHostname(protocol, value);
                        }}
                      />
                    </FormGroup>
                    {getTlsTerminations(protocol).length > 0 && (
                      <>
                        <br />
                        <FormGroup
                          fieldId="configuring-routes-tls-terminations-radio"
                          label="TLS Terminations"
                          isRequired={true}
                        >
                          {getTlsTerminations(protocol).map(
                            (termination, index) => (
                              <div key={`key-termination-${index}`}>
                                <br />
                                <Radio
                                  isChecked={
                                    tlsTermination === termination.value
                                  }
                                  onChange={(_, value) =>
                                    onChangeTlsTermination(protocol, value)
                                  }
                                  name={`radio-${termination.key}-${protocol}`}
                                  key={`${protocol}-${termination.key}`}
                                  label={termination.label}
                                  id={`configuring-routes-tls-terminations-radio-${termination.key}-${protocol}`}
                                  value={termination.value}
                                  style={{ marginLeft: 20 }}
                                />
                              </div>
                            )
                          )}
                        </FormGroup>
                      </>
                    )}
                    <br />
                  </div>
                );
              })}
          </Form>
        </GridItem>
      </Grid>
    </>
  );
};

export { ConfiguringRoutes };
