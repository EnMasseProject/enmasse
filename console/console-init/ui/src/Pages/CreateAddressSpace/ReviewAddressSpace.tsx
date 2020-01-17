/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import {
  Grid,
  GridItem,
  Title,
  PageSection,
  PageSectionVariants,
  TooltipPosition,
  Tooltip
} from "@patternfly/react-core";
import { Loading } from "use-patternfly";
import { useQuery } from "@apollo/react-hooks";
import { OutlinedCopyIcon } from "@patternfly/react-icons";
import { StyleSheet, css } from "@patternfly/react-styles";
import AceEditor from "react-ace";
import "ace-builds/src-noconflict/mode-java";
import "ace-builds/src-noconflict/theme-github";
import { ADDRESS_SPACE_COMMAND_REVIEW_DETAIL } from "src/Queries/Queries";

export interface IAddressSpaceReview {
  name?: string;
  type?: string;
  plan?: string;
  namespace: string;
  authenticationService: string;
}
const Style = StyleSheet.create({
  left_padding: {
    paddingLeft: 32
  },
  bottom_padding: {
    paddingBottom: 16
  }
});

export const ReviewAddressSpace: React.FunctionComponent<IAddressSpaceReview> = ({
  name,
  type,
  plan,
  namespace,
  authenticationService
}) => {
  const [keepInViewChecked, setKeepInViewChecked] = React.useState<boolean>(
    false
  );
  const { data, loading, error } = useQuery(
    ADDRESS_SPACE_COMMAND_REVIEW_DETAIL,
    {
      variables: {
        as: {
          ObjectMeta: {
            Name: name,
            Namespace: namespace
          },
          Spec: {
            Plan: plan ? plan.toLowerCase() : "",
            Type: type ? type.toLowerCase() : ""
          }
        }
      }
    }
  );
  if (loading) return <Loading />;
  if (error) console.log("Address Space Review Query Error", error);
  console.log(data);
  return (
    <PageSection variant={PageSectionVariants.light}>
      <Title size="3xl" style={{ marginBottom: 32 }}>
        Review your configuration
      </Title>
      <Title size="xl" style={{ marginBottom: 32 }}>
        {" "}
        Review the information below and Click Finish to create the new address.
        Use the Back button to make changes.
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
            {name && (
              <>
                <GridItem span={4} style={{ marginBottom: 16, marginRight: 5 }}>
                  Instance name
                </GridItem>
                <GridItem id="preview-addr-name" span={8}>
                  {name}
                </GridItem>
              </>
            )}
            {namespace && (
              <>
                <GridItem span={4} style={{ marginBottom: 16, marginRight: 5 }}>
                  Namespace
                </GridItem>
                <GridItem id="preview-addr-name" span={8}>
                  {namespace}
                </GridItem>
              </>
            )}
            {type && type.trim() !== "" && (
              <>
                <GridItem span={4} style={{ marginBottom: 16, marginRight: 5 }}>
                  Type
                </GridItem>
                <GridItem id="preview-addr-type" span={8}>
                  {type}
                </GridItem>
              </>
            )}
            {plan && plan.trim() !== "" && (
              <>
                <GridItem span={4} style={{ marginBottom: 16, marginRight: 5 }}>
                  Plan
                </GridItem>
                <GridItem id="preview-addr-plan" span={8}>
                  {plan}
                </GridItem>
              </>
            )}
            {authenticationService && authenticationService.trim() !== "" && (
              <>
                <GridItem span={4} style={{ marginBottom: 16, marginRight: 5 }}>
                  Authentication Service
                </GridItem>
                <GridItem id="preview-addr-plan" span={8}>
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
            id="preview-addr-copy-tooltip"
              position={TooltipPosition.top}
              enableFlip={keepInViewChecked}
              content={<div>Copy the configuration details on clipboard</div>}
            >
              <OutlinedCopyIcon
                id="preview-addr-copy-btn"
                size="md"
                color="blue"
                onClick={() => {
                  navigator.clipboard.writeText(data.addressSpaceCommand);
                  // alert("coopied successfully");
                }}
              />
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
