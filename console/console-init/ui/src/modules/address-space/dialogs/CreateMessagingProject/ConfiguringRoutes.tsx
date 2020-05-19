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
import { tlsTerminationOptions } from "modules/address-space/utils";

interface IConfiguringRoutes {
  projectDetail: IMessagingProject;
  setProjectDetail: (projectDetail: IMessagingProject) => void;
}

const ConfiguringRoutes: React.FunctionComponent<IConfiguringRoutes> = ({
  projectDetail,
  setProjectDetail
}) => {
  const { hostname, tlsTermination } = projectDetail;
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
          <Title headingLevel="h2" size="2xl">
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
                isRequired
                type="text"
                id="simple-form-name"
                name="simple-form-name"
                aria-describedby="simple-form-name-helper"
                value={hostname}
                onChange={onChangeHostname}
              />
            </FormGroup>
            <br />
            <FormGroup
              fieldId="form-group-endpoint-tls-certs"
              label="TLS Certificates"
            >
              {tlsTerminationOptions.map(termination => (
                <>
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
                </>
              ))}
            </FormGroup>
          </Form>
        </GridItem>
      </Grid>
    </>
  );
};

export { ConfiguringRoutes };
