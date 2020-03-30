/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import AceEditor from "react-ace";
import {
  Grid,
  GridItem,
  Title,
  PageSection,
  PageSectionVariants,
  TooltipPosition,
  Tooltip,
  Button,
  ButtonVariant
} from "@patternfly/react-core";
import { OutlinedCopyIcon } from "@patternfly/react-icons";
import { StyleSheet, css } from "@patternfly/react-styles";

export interface IAddressSpaceReviewProps {
  name?: string;
  type?: string;
  plan?: string;
  namespace: string;
  authenticationService: string;
  data: {
    addressSpaceCommand: string;
  };
}

const Style = StyleSheet.create({
  left_padding: {
    paddingLeft: 32
  },
  bottom_padding: {
    paddingBottom: 16
  }
});

export const AddressSpaceReview: React.FC<IAddressSpaceReviewProps> = ({
  name,
  type,
  plan,
  namespace,
  authenticationService,
  data
}) => {
  const [isCopied, setIsCopied] = useState<boolean>(false);
  return (
    <PageSection variant={PageSectionVariants.light}>
      <Title size="3xl" style={{ marginBottom: 32 }}>
        Review your configuration
      </Title>
      <Title size="xl" style={{ marginBottom: 32 }}>
        {" "}
        Review the information below and Click Finish to create the new address
        space. Use the Back button to make changes.
      </Title>
      <Grid>
        <GridItem
          span={5}
          style={{
            borderRight: "0.1em solid",
            borderRightColor: "lightgrey"
          }}
        >
          <Grid>
            {name && name.trim() !== "" && (
              <>
                <GridItem span={5} style={{ marginBottom: 16, marginRight: 5 }}>
                  Instance name
                </GridItem>
                <GridItem id="preview-addr-name" span={7}>
                  {name}
                </GridItem>
              </>
            )}
            {namespace && namespace.trim() !== "" && (
              <>
                <GridItem span={5} style={{ marginBottom: 16, marginRight: 5 }}>
                  Namespace
                </GridItem>
                <GridItem id="preview-addr-name" span={7}>
                  {namespace}
                </GridItem>
              </>
            )}
            {type && type.trim() !== "" && (
              <>
                <GridItem span={5} style={{ marginBottom: 16, marginRight: 5 }}>
                  Type
                </GridItem>
                <GridItem id="preview-addr-type" span={7}>
                  {type}
                </GridItem>
              </>
            )}
            {plan && plan.trim() !== "" && (
              <>
                <GridItem span={5} style={{ marginBottom: 16, marginRight: 5 }}>
                  Plan
                </GridItem>
                <GridItem id="preview-addr-plan" span={7}>
                  {plan}
                </GridItem>
              </>
            )}
            {authenticationService && authenticationService.trim() !== "" && (
              <>
                <GridItem span={5} style={{ marginBottom: 16, marginRight: 5 }}>
                  Authentication Service
                </GridItem>
                <GridItem id="preview-addr-plan" span={7}>
                  {authenticationService}
                </GridItem>
              </>
            )}
          </Grid>
        </GridItem>
        <GridItem span={7} className={css(Style.left_padding)}>
          <Title size={"lg"} className={css(Style.bottom_padding)}>
            {`Configuration details  `}
            <Tooltip
              id="preview-as-feedback-tooltip"
              position={TooltipPosition.top}
              enableFlip={false}
              trigger={"manual"}
              content={<div>Successfully copied to the clipboard</div>}
              isVisible={isCopied}
            >
              <span>
                <Tooltip
                  id="preview-as-copy-tooltip"
                  position={TooltipPosition.top}
                  enableFlip={false}
                  content={
                    <div>Copy the configuration details to the clipboard</div>
                  }
                >
                  <Button
                    id="preview-addr-copy-configuration-button"
                    variant={ButtonVariant.link}
                    aria-label="copy-configuration"
                    onClick={() => {
                      navigator.clipboard.writeText(data.addressSpaceCommand);
                      setIsCopied(true);
                    }}
                    onMouseLeave={() => {
                      setIsCopied(false);
                    }}
                  >
                    <OutlinedCopyIcon id="preview-addr-copy-btn" size="md" />
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
            value={data.addressSpaceCommand}
            name="UNIQUE_ID_OF_DIV"
            editorProps={{ $blockScrolling: true }}
            style={{
              width: 700,
              border: "1px solid",
              borderColor: "lightgrey"
            }}
          />
        </GridItem>
      </Grid>
    </PageSection>
  );
};
