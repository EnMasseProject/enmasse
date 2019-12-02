import * as React from "react";
import {
  Title,
  Flex,
  FlexItem,
  Card,
  CardHeader,
  CardBody
} from "@patternfly/react-core";
import {
  LockIcon,
  LockOpenIcon,
  AngleDownIcon,
  AngleUpIcon
} from "@patternfly/react-icons";
import { ConnectionDetail } from "./ConnectionDetail";
import { MessagesDetail } from "./MessagesDetail";
import { css, StyleSheet } from "@patternfly/react-styles";
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
  product,
  version,
  platform,
  os,
  messagesIn,
  messagesOut
}) => {
  const generateIcons = () => {
    switch (protocol) {
      case "AMQP":
        return <LockIcon />;
      default:
        return <LockOpenIcon />;
    }
  };
  const [isHidden, setIsHidden] = React.useState(true);
  const [isMobileView, setIsMobileView] = React.useState(false);
  window.addEventListener("resize", () => {
    if (window.innerWidth < 992) {
      setIsMobileView(true);
    } else {
      setIsMobileView(false);
    }
  });
  return (
    <Card>
      <CardHeader>
        <Title headingLevel="h1" size="4xl">
          {hostname}
        </Title>
      </CardHeader>
      <CardBody>
        <Flex>
          <FlexItem className={css(styles.flex_right_border)}>
            in container <b>{containerId}</b>
          </FlexItem>
          <FlexItem>
            {protocol} {generateIcons()}
          </FlexItem>
          {!isMobileView && (
            <FlexItem
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
          breakpointMods={[
            { modifier: "column", breakpoint: "sm" },
            { modifier: "row", breakpoint: "lg" }
          ]}
        >
          {isMobileView || !isHidden ? (
            <>
              <ConnectionDetail
                product={product}
                version={version}
                jvm={platform}
                os={os}
                isMobileView={isMobileView}
              />
              <MessagesDetail
                messagesIn={messagesIn}
                messagesOut={messagesOut}
                isMobileView={isMobileView}
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
