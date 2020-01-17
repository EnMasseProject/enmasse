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
import { ConnectionProtocolFormat } from "../Common/ConnectionListFormatter";
import useWindowDimensions from "../Common/WindowDimension";
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
  product?: string;
  version?: string;
  platform?: string;
  os?: string;
  messagesIn?: number;
  messagesOut?: number;
}
export const ConnectionDetailHeader: React.FunctionComponent<IConnectionHeaderDetailProps> = ({
  hostname,
  containerId,
  protocol,
  encrypted,
  product,
  version,
  platform,
  os,
  messagesIn,
  messagesOut
}) => {
  const [isHidden, setIsHidden] = React.useState(true);
  const { width } = useWindowDimensions();
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
          <FlexItem id="cd-header-protocol">
            <ConnectionProtocolFormat
              protocol={protocol}
              encrypted={encrypted}
            />
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
                messagesIn={messagesIn}
                messagesOut={messagesOut}
                isMobileView={width < 992 ? true : false}
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
