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
  }
});
export interface ConnectionHeaderDetailProps {
  hostname: string;
  containerId: string;
  protocol: string;
  product: string;
  version: string;
  platform: string;
  os: string;
  messagesIn: number;
  messagesOut: number;
}
export const ConnectionDetailHeader: React.FunctionComponent<
  ConnectionHeaderDetailProps
> = ({
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
          <FlexItem>
            in container <b>{containerId}</b>
          </FlexItem>
          <FlexItem>
            {protocol} {generateIcons()}
          </FlexItem>
          {!isMobileView ? (
            <FlexItem
              onClick={() => {
                setIsHidden(!isHidden);
              }}
              className={css(styles.expandable)}
              // style={{ color: 'rgb(0, 102, 204)' }}
            >
              {" "}
              {isHidden ? (
                <>
                  see more details <AngleDownIcon color="black" />
                </>
              ) : (
                <>
                  hide details
                  <AngleUpIcon color="black" />
                </>
              )}
            </FlexItem>
          ) : (
            ""
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
