/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Link } from "react-router-dom";
import { Flex, FlexItem, Text } from "@patternfly/react-core";

export interface IGatewayItemsProps {
  gateways: string[];
  title: string;
  projectname: string;
  namespace: string;
}

export const GatewayItems: React.FC<IGatewayItemsProps> = ({
  title,
  namespace,
  projectname,
  gateways
}) => (
  <>
    <Text>{title}</Text>
    <Flex>
      {gateways.map((gateway: string, index: number) => {
        return (
          <FlexItem span={2} key={`${gateway}-${index}`}>
            <Link
              id="device-info-id-link"
              to={`/iot-projects/${namespace}/${projectname}/devices/${gateway}/device-info`}
            >
              {gateway}
            </Link>
          </FlexItem>
        );
      })}
    </Flex>
  </>
);
