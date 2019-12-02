import * as React from "react";
import {
  Grid,
  GridItem,
  Title,
  PageSection,
  PageSectionVariants
} from "@patternfly/react-core";
import gql from "graphql-tag";
import { Loading } from "use-patternfly";
import { useQuery } from "@apollo/react-hooks";
import { OutlinedCopyIcon } from "@patternfly/react-icons";
import { StyleSheet, css } from "@patternfly/react-styles";
import AceEditor from "react-ace";

import "ace-builds/src-noconflict/mode-java";
import "ace-builds/src-noconflict/theme-github";
export interface IAddressPreview {
  name?: string;
  type?: string;
  plan?: string;
  namespace: string;
}
const Style = StyleSheet.create({
  left_padding: {
    paddingLeft: 32
  },
  bottom_padding: {
    paddingBottom: 16
  }
});

const ADDRESS_PRIVEW_DETAIL = gql`
  query cmd($as: AddressSpace_enmasse_io_v1beta1_Input!) {
    addressSpaceCommand(input: $as)
  }
`;

interface ObjectMeta_v1_Input {
  Name?: string;
  Namespace?: string;
  ResourceVersion?: string;
}

export const PreviewAddress: React.FunctionComponent<IAddressPreview> = ({
  name,
  type,
  plan,
  namespace
}) => {
  const { data, loading, error } = useQuery(ADDRESS_PRIVEW_DETAIL, {
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
  });
  if (loading) return <Loading />;
  if (error) console.log("Address Priview Query Error", error);
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
                  Address name
                </GridItem>
                <GridItem span={8}>{name}</GridItem>
              </>
            )}
            {type && type.trim() !== "" && (
              <>
                <GridItem span={4} style={{ marginBottom: 16, marginRight: 5 }}>
                  Type
                </GridItem>
                <GridItem span={8}>{type}</GridItem>
              </>
            )}
            {plan && plan.trim() !== "" && (
              <>
                <GridItem span={4} style={{ marginBottom: 16, marginRight: 5 }}>
                  Plan
                </GridItem>
                <GridItem span={8}>{plan}</GridItem>
              </>
            )}
          </Grid>
        </GridItem>
        <GridItem span={7} className={css(Style.left_padding)}>
          <Title size={"lg"} className={css(Style.bottom_padding)}>
            {`Configuration details  `}
            <OutlinedCopyIcon
              size="md"
              color="blue"
              onClick={() => {
                navigator.clipboard.writeText(data.addressSpaceCommand);
                alert("coopied successfully");
              }}
            />
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
