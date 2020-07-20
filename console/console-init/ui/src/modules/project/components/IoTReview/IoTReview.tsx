/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import AceEditor from "react-ace";
import {
  PageSection,
  Title,
  Grid,
  GridItem,
  Tooltip,
  TooltipPosition,
  Button,
  ButtonVariant,
  PageSectionVariants,
  TitleSizes,
  Text,
  TextVariants
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { OutlinedCopyIcon } from "@patternfly/react-icons";

export interface IIoTReviewProps {
  name?: string;
  namespace: string;
  isEnabled: boolean;
}

const styles = StyleSheet.create({
  left_padding_with_border: {
    paddingLeft: 32,
    borderLeft: "0.1em solid",
    borderLeftColor: "lightgrey"
  },
  bottom_padding: {
    paddingBottom: 16
  },
  item_grid_margin: { marginBottom: 16, marginRight: 5 },
  editor: {
    width: 400,
    border: "1px solid",
    borderColor: "lightgrey"
  },
  expandable: {
    color: "rgb(0, 102, 204)"
  }
});

export const IoTReview: React.FunctionComponent<IIoTReviewProps> = ({
  name,
  namespace,
  isEnabled
}) => {
  const [isCopied, setIsCopied] = useState<boolean>(false);
  const [isExpanded, setIsExpanded] = useState<boolean>(false);
  return (
    <PageSection variant={PageSectionVariants.light}>
      <Title
        headingLevel="h2"
        size={TitleSizes["2xl"]}
        style={{ marginBottom: 32 }}
      >
        Review your configuration
      </Title>
      <Text component={TextVariants.h2}>
        Review the information below and click Finish to create the new iot
        project, use the Back button to make changes.
      </Text>
      <br />
      <Grid>
        <GridItem span={5}>
          {name && name.trim() !== "" && (
            <Grid>
              <GridItem span={5} className={css(styles.item_grid_margin)}>
                Project name
              </GridItem>
              <GridItem id="preview-iot-name" span={7}>
                {name}
              </GridItem>
            </Grid>
          )}
          {namespace && namespace.trim() !== "" && (
            <Grid>
              <GridItem span={5} className={css(styles.item_grid_margin)}>
                Namespace
              </GridItem>
              <GridItem id="preview-iot-namespace" span={7}>
                {namespace}
              </GridItem>
            </Grid>
          )}
          {isEnabled !== undefined && (
            <Grid>
              <GridItem span={5} className={css(styles.item_grid_margin)}>
                Enable
              </GridItem>
              <GridItem id="preview-iot-enabled" span={7}>
                {isEnabled ? "true" : "false"}
              </GridItem>
            </Grid>
          )}
          <br />
          <span>
            Click here to{" "}
            <Button
              variant={ButtonVariant.link}
              isInline
              onClick={() => setIsExpanded(!isExpanded)}
              id="iot-review-expand-button"
            >
              {" "}
              {!isExpanded
                ? "show equivalent command"
                : "hide the command"}{" "}
            </Button>
          </span>
        </GridItem>
        {isExpanded && (
          <GridItem span={7} className={css(styles.left_padding_with_border)}>
            <Title
              headingLevel="h2"
              size="lg"
              className={css(styles.bottom_padding)}
            >
              {`Configuration details  `}
              <Tooltip
                id="iot-review-successfully-copied-tooltip"
                position={TooltipPosition.top}
                enableFlip={false}
                trigger={"manual"}
                content={<div>Successfully copied to the clipboard</div>}
                isVisible={isCopied}
              >
                <span>
                  <Tooltip
                    id="iot-review-copy-configuration-tooltip"
                    position={TooltipPosition.top}
                    enableFlip={false}
                    content={
                      <div>Copy the configuration details to the clipboard</div>
                    }
                  >
                    <Button
                      id="iot-review-copy-button"
                      variant={ButtonVariant.link}
                      aria-label="copy iot configuration"
                      onClick={() => {
                        //   navigator.clipboard.writeText(data.addressSpaceCommand);
                        setIsCopied(true);
                      }}
                      onMouseLeave={() => setIsCopied(false)}
                    >
                      <OutlinedCopyIcon id="iot-review-copy-icon" size="md" />
                    </Button>
                  </Tooltip>
                </span>
              </Tooltip>
            </Title>
            <AceEditor
              mode="xml"
              theme="github"
              fontSize={14}
              width={"auto"}
              // onChange={() => {}}
              value={"data"}
              name="UNIQUE_ID_OF_DIV"
              editorProps={{ $blockScrolling: true }}
              className={css(styles.editor)}
            />
          </GridItem>
        )}
      </Grid>
    </PageSection>
  );
};
