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
import { endpointProtocolOptions } from "modules/project/utils";

export interface IMessagingReviewProps {
  name?: string;
  type?: string;
  plan?: string;
  namespace: string;
  authenticationService: string;
  data: {
    addressSpaceCommand: string;
  };
  protocols?: string[];
  customizeEndpoint: boolean;
  addRoutes: boolean;
  tlsCertificate?: string;
}

const style = StyleSheet.create({
  left_padding_with_border: {
    paddingLeft: 32,
    borderLeft: "0.1em solid",
    borderLeftColor: "lightgrey"
  },
  left_padding: {
    paddingLeft: 32
  },
  left_padding_for_endpoints: {
    paddingLeft: 40
  },
  bottom_padding: {
    paddingBottom: 16
  },
  left_margin_gridItem: {
    marginBottom: 16,
    marginLeft: 10,
    marginRight: 5
  },
  preview_info_gridItem: {
    marginBottom: 16,
    marginRight: 5
  },
  grid_border: {
    borderRight: "0.1em solid",
    borderRightColor: "lightgrey"
  },
  editor: {
    border: "1px solid",
    borderColor: "lightgrey"
  }
});
interface IReviewGridProps {
  labelText: string;
  value?: string;
  valueId: string;
  addLeftMargin?: boolean;
}

const ReviewGridItem: React.FunctionComponent<IReviewGridProps> = ({
  labelText,
  valueId,
  value,
  addLeftMargin = false
}) => {
  const className = addLeftMargin
    ? css(style.preview_info_gridItem)
    : css(style.left_margin_gridItem);
  return (
    <>
      {((labelText && labelText.trim() !== "") ||
        (value && value.trim() !== "")) && (
        <>
          <GridItem span={5} className={className}>
            {labelText?.trim()}
          </GridItem>
          <GridItem id={valueId} span={7}>
            {value?.trim()}
          </GridItem>
        </>
      )}
    </>
  );
};
export const MessagingReview: React.FC<IMessagingReviewProps> = ({
  name,
  type,
  plan,
  namespace,
  authenticationService,
  data,
  protocols,
  customizeEndpoint,
  addRoutes,
  tlsCertificate
}) => {
  const [isCopied, setIsCopied] = useState<boolean>(false);
  const [isExpanded, setIsExpanded] = useState<boolean>(false);
  const certificate = tlsCertificate;
  let protocolOptions = [];
  if (protocols) {
    for (const protocol of protocols) {
      protocolOptions.push(
        endpointProtocolOptions.find((option: any) => option.value === protocol)
      );
    }
  }

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
        <GridItem span={5}>
          <Grid>
            <ReviewGridItem
              valueId="preview-addr-name"
              value={name}
              labelText="Instance name"
            />
            <ReviewGridItem
              valueId="preview-addr-namespace"
              value={namespace}
              labelText="Namespace"
            />
            <ReviewGridItem
              valueId="preview-addr-type"
              value={type}
              labelText="Type"
            />
            <ReviewGridItem
              valueId="preview-addr-plan"
              value={plan}
              labelText="Plan"
            />
            <ReviewGridItem
              valueId="preview-addr-authenticationService"
              value={authenticationService}
              labelText="Authentication Service"
            />
            {customizeEndpoint && (
              <>
                <ReviewGridItem
                  valueId="preview-addr-cutom-endpoint"
                  value={""}
                  labelText="Endpoint customization"
                />

                {protocolOptions.length > 0 && (
                  <>
                    <GridItem
                      span={5}
                      className={css(style.left_padding_for_endpoints)}
                    >
                      Protocols
                    </GridItem>
                    <GridItem
                      span={7}
                      className={css(style.preview_info_gridItem)}
                    >
                      {protocolOptions &&
                        protocolOptions.map(protocol => (
                          <>
                            {protocol?.label}
                            <br />
                          </>
                        ))}
                    </GridItem>
                  </>
                )}
                {certificate && (
                  <>
                    <GridItem
                      span={5}
                      className={css(style.left_padding_for_endpoints)}
                    >
                      TLS Certificates
                    </GridItem>
                    <GridItem
                      span={7}
                      className={css(style.preview_info_gridItem)}
                    >
                      {certificate}
                    </GridItem>
                  </>
                )}
                <>
                  <GridItem
                    span={5}
                    className={css(style.left_padding_for_endpoints)}
                  >
                    Create Routes
                  </GridItem>
                  <GridItem
                    span={7}
                    className={css(style.preview_info_gridItem)}
                  >
                    {addRoutes ? "True" : "False"}
                  </GridItem>
                </>
              </>
            )}
            <br />
            <span>
              Click here to{" "}
              <Button
                variant={ButtonVariant.link}
                isInline
                onClick={() => setIsExpanded(!isExpanded)}
              >
                {" "}
                {!isExpanded
                  ? "show equivalent command"
                  : "hide the command"}{" "}
              </Button>
            </span>
          </Grid>
        </GridItem>
        {isExpanded && (
          <GridItem span={7} className={css(style.left_padding_with_border)}>
            <Title size={"lg"} className={css(style.bottom_padding)}>
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
              width="auto"
              fontSize={14}
              value={data.addressSpaceCommand}
              name="UNIQUE_ID_OF_DIV"
              editorProps={{ $blockScrolling: true }}
              className={css(style.editor)}
            />
          </GridItem>
        )}
      </Grid>
    </PageSection>
  );
};
