/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Title,
  Flex,
  FlexItem,
  Card,
  CardBody,
  CardTitle
} from "@patternfly/react-core";
import { AngleDownIcon, AngleUpIcon } from "@patternfly/react-icons";
import { FormatDistance } from "use-patternfly";
import {
  ConnectionDetailHeaderAttributes,
  MessagesDetailHeaderAttributes
} from "modules/connection-detail/components";
import { StyleSheet, css } from "aphrodite";
import { ConnectionProtocolFormat } from "utils";
import { useWindowDimensions } from "components";

const styles = StyleSheet.create({
  expandable: {
    color: "rgb(0, 102, 204)"
  },
  flex_right_border: {
    paddingRight: "1em",
    borderRight: "0.05em solid",
    borderRightColor: "lightgrey"
  }
});
export interface IConnectionHeaderDetailProps {
  hostname: string;
  containerId: string;
  protocol: string;
  encrypted: boolean;
  creationTimestamp: string;
  product?: string;
  version?: string;
  platform?: string;
  os?: string;
  messageIn?: number | string;
  messageOut?: number | string;
  addressSpaceType?: string;
}
export const ConnectionDetailHeader: React.FunctionComponent<IConnectionHeaderDetailProps> = ({
  hostname,
  containerId,
  protocol,
  encrypted,
  creationTimestamp,
  product,
  version,
  platform,
  os,
  messageIn,
  messageOut,
  addressSpaceType
}) => {
  const [isHidden, setIsHidden] = useState(true);
  const { width } = useWindowDimensions();

  return (
    <Card>
      <CardTitle>
        <Title id="connection-detail-header-title" headingLevel="h1" size="4xl">
          {hostname}
        </Title>
      </CardTitle>
      <CardBody>
        <Flex>
          <FlexItem
            id="connection-detail-header-container-id-flexitem"
            className={css(styles.flex_right_border)}
          >
            in container <b>{containerId}</b>
          </FlexItem>
          <FlexItem
            id="connection-detail-header-protocol-flexitem"
            className={css(styles.flex_right_border)}
          >
            <ConnectionProtocolFormat
              protocol={protocol}
              encrypted={encrypted}
            />
          </FlexItem>
          <FlexItem id="connection-detail-header-protocol-created-flexitem">
            Created &nbsp;
            <b>
              {creationTimestamp && (
                <>
                  <FormatDistance date={creationTimestamp} /> ago
                </>
              )}
            </b>
          </FlexItem>
          {width > 992 && (
            <FlexItem
              id="connection-detail-header-see-hide-details-flexitem"
              onClick={() => {
                setIsHidden(!isHidden);
              }}
              className={css(styles.expandable)}
            >
              {isHidden ? (
                <>
                  See more details <AngleDownIcon color="black" />
                </>
              ) : (
                <>
                  Hide details
                  <AngleUpIcon color="black" />
                </>
              )}
            </FlexItem>
          )}
        </Flex>
        <Flex
          id="connection-detail-header-connection-messages-flex"
          direction={{ sm: "column", lg: "row" }}
        >
          {(width < 992 || !isHidden) && (
            <>
              <ConnectionDetailHeaderAttributes
                product={product}
                version={version}
                jvm={platform}
                os={os}
                isMobileView={width < 992 ? true : false}
              />
              <MessagesDetailHeaderAttributes
                messageIn={messageIn}
                messageOut={messageOut}
                isMobileView={width < 992 ? true : false}
                addressSpaceType={addressSpaceType}
              />
            </>
          )}
        </Flex>
      </CardBody>
    </Card>
  );
};
