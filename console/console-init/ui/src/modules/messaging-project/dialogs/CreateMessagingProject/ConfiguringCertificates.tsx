/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { IMessagingProject } from "./CreateMessagingProject";
import { EndpointCertificateConfiguration } from "modules/messaging-project/components";
import { Grid, GridItem } from "@patternfly/react-core";

interface IConfiguringCertificates {
  projectDetail: IMessagingProject;
  setProjectDetail: (projectDetail: IMessagingProject) => void;
}

const ConfiguringCertificates: React.FunctionComponent<IConfiguringCertificates> = ({
  projectDetail,
  setProjectDetail
}) => {
  const { certValue, privateKey } = projectDetail;
  const setCertValue = (certValue: string) => {
    setProjectDetail({ ...projectDetail, certValue: certValue.trim() });
  };
  const setPrivateKey = (key: string) => {
    setProjectDetail({ ...projectDetail, privateKey: key.trim() });
  };
  return (
    <Grid>
      <GridItem span={9}>
        <EndpointCertificateConfiguration
          certificate={certValue}
          privateKey={privateKey}
          setCertificate={setCertValue}
          setPrivateKey={setPrivateKey}
        />
      </GridItem>
    </Grid>
  );
};

export { ConfiguringCertificates };
