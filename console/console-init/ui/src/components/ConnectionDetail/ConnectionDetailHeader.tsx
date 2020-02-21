/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import {
  Title,
  Flex,
  FlexItem,
  Card,
  CardHeader,
  CardBody
} from "@patternfly/react-core";
import { AngleDownIcon, AngleUpIcon } from "@patternfly/react-icons";
import { ConnectionDetail } from "./ConnectionDetail";
import { MessagesDetail } from "./MessagesDetail";
import { css, StyleSheet } from "@patternfly/react-styles";
import { ConnectionProtocolFormat } from "components/common/ConnectionListFormatter";
import useWindowDimensions from "components/common/WindowDimension";
import { FormatDistance } from "use-patternfly";
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
  const [isHidden, setIsHidden] = React.useState(true);
  const { width } = useWindowDimensions();
  console.log("addressSpaceType", addressSpaceType);
  return (
    <Card>
      <CardHeader>
        <Title id="cd-header-title" headingLevel="h1" size="4xl">
          {hostname}
        </Title>
      </CardHeader>
      <CardBody>
        <Flex>
          <FlexItem
            id="cd-header-container-id"
            className={css(styles.flex_right_border)}
          >
            in container <b>{containerId}</b>
          </FlexItem>
          <FlexItem
            id="cd-header-protocol"
            className={css(styles.flex_right_border)}
          >
            <ConnectionProtocolFormat
              protocol={protocol}
              encrypted={encrypted}
            />
          </FlexItem>
          <FlexItem id="cd-header-protocol">
            Created about &nbsp;
            <b>
              <FormatDistance date={creationTimestamp} /> ago
            </b>
          </FlexItem>
          {width > 992 && (
            <FlexItem
              id="cd-header-see-hide-more"
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
          id="cd-header-connection-messages"
          breakpointMods={[
            { modifier: "column", breakpoint: "sm" },
            { modifier: "row", breakpoint: "lg" }
          ]}
        >
          {width < 992 || !isHidden ? (
            <>
              <ConnectionDetail
                product={product}
                version={version}
                jvm={platform}
                os={os}
                isMobileView={width < 992 ? true : false}
              />
              <MessagesDetail
                messageIn={messageIn}
                messageOut={messageOut}
                isMobileView={width < 992 ? true : false}
                addressSpaceType={addressSpaceType}
              />
            </>
          ) : (
            ""
          )}
        </Flex>
      </CardBody>
    </Card>
  );
};
