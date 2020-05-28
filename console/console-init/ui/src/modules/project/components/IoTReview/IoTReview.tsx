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
  PageSectionVariants
} from "@patternfly/react-core";
import { StyleSheet, css } from "@patternfly/react-styles";
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
    width: 700,
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
      <Title size="3xl" style={{ marginBottom: 32 }}>
        Review your configuration
      </Title>
      <Title size="xl" style={{ marginBottom: 32 }}>
        {" "}
        Review the information below and Click Finish to create the new iot
        project. Use the Back button to make changes.
      </Title>
      <Grid>
        <GridItem span={5}>
          <Grid>
            {name && name.trim() !== "" && (
              <>
                <GridItem span={5} className={css(styles.item_grid_margin)}>
                  Project name
                </GridItem>
                <GridItem id="preview-iot-name" span={7}>
                  {name}
                </GridItem>
              </>
            )}
            {namespace && namespace.trim() !== "" && (
              <>
                <GridItem span={5} className={css(styles.item_grid_margin)}>
                  Namespace
                </GridItem>
                <GridItem id="preview-iot-namespace" span={7}>
                  {namespace}
                </GridItem>
              </>
            )}
            {isEnabled !== undefined && (
              <>
                <GridItem span={5} className={css(styles.item_grid_margin)}>
                  Enabled
                </GridItem>
                <GridItem id="preview-iot-enabled" span={7}>
                  {isEnabled ? "true" : "false"}
                </GridItem>
              </>
            )}
            <br />
            <span>
              Click here to{" "}
              <a onClick={() => setIsExpanded(!isExpanded)}>
                {" "}
                {!isExpanded
                  ? "show equivalent command"
                  : "hide the command"}{" "}
              </a>
            </span>
          </Grid>
        </GridItem>
        {isExpanded && (
          <GridItem span={7} className={css(styles.left_padding_with_border)}>
            <Title size="lg" className={css(styles.bottom_padding)}>
              {`Configuration details  `}
              <Tooltip
                id="iot-preview-feedback"
                position={TooltipPosition.top}
                enableFlip={false}
                trigger={"manual"}
                content={<div>Successfully copied to the clipboard</div>}
                isVisible={isCopied}
              >
                <span>
                  <Tooltip
                    id="iot-preview-copy-feedback"
                    position={TooltipPosition.top}
                    enableFlip={false}
                    content={
                      <div>Copy the configuration details to the clipboard</div>
                    }
                  >
                    <Button
                      id="preview-iot-copy-button"
                      variant={ButtonVariant.link}
                      aria-label="copy iot configuration"
                      onClick={() => {
                        //   navigator.clipboard.writeText(data.addressSpaceCommand);
                        setIsCopied(true);
                      }}
                      onMouseLeave={() => setIsCopied(false)}
                    >
                      <OutlinedCopyIcon id="preview-iot-copy-icon" size="md" />
                    </Button>
                  </Tooltip>
                </span>
              </Tooltip>
            </Title>
            <AceEditor
              mode="xml"
              theme="github"
              fontSize={14}
              onChange={() => {}}
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
