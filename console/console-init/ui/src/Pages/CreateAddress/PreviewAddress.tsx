import * as React from "react";
import {
  Grid,
  GridItem,
  Title,
  PageSection,
  PageSectionVariants,
  ClipboardCopy,
  ClipboardCopyVariant
} from "@patternfly/react-core";
import gql from "graphql-tag";
import { Loading } from "use-patternfly";
import { useQuery } from "@apollo/react-hooks";
import { CopyIcon, OutlinedCopyIcon } from "@patternfly/react-icons";
import { StyleSheet, css } from "@patternfly/react-styles";
import SyntaxHighlighter from "react-syntax-highlighter";

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
interface AddressSpaceSpec_enmasse_io_v1beta1_Input {
  Plan?: string;
  Type?: string;
}
interface Address_enmasse_io_v1beta1_Input {
  as: {
    ObjectMeta: ObjectMeta_v1_Input;
    Spec: AddressSpaceSpec_enmasse_io_v1beta1_Input;
  };
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
  const addressPreviewCode = data;
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
            {type && type.trim() != "" && (
              <>
                <GridItem span={4} style={{ marginBottom: 16, marginRight: 5 }}>
                  Type
                </GridItem>
                <GridItem span={8}>{type}</GridItem>
              </>
            )}
            {plan && plan.trim() != "" && (
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
            Configuration details
            <OutlinedCopyIcon
              color="blue"
              onClick={() => {
                navigator.clipboard.writeText(data.addressSpaceCommand);
              }}
            />
          </Title>
          <SyntaxHighlighter language="javascript">
            {data.addressSpaceCommand}
          </SyntaxHighlighter>
        </GridItem>
      </Grid>
    </PageSection>
  );
};
