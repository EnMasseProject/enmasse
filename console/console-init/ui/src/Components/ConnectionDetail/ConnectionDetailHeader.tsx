import * as React from "react";
import {
  PageSection,
  PageSectionVariants,
  Split,
  SplitItem,
  GridItem,
  Grid
} from "@patternfly/react-core";
import { LockIcon, LockOpenIcon } from "@patternfly/react-icons";
import '../AddressSpace/AddressSpaceHeader.css'
export interface ConnectionHeaderDetailProps {
  hostname: string;
  containerId: string;
  protocol: string;
}
export const ConnectionDetailHeader: React.FunctionComponent<
  ConnectionHeaderDetailProps
> = ({ hostname, containerId, protocol }) => {
  const generateIcons = () => {
    switch (protocol) {
      case "AMQP":
        return <LockIcon />;
      default:
        return <LockOpenIcon />;
    }
  };
  return (
    <PageSection variant={PageSectionVariants.light}>
      <Grid>
        <GridItem rowSpan={12} className="l_split_m_height">
          {hostname}
        </GridItem>
        <GridItem>
          <Split>
            <SplitItem className="l_split_m_gutter_MarginRight">
              in container <b>{containerId}</b>
            </SplitItem>
            <SplitItem className="l_split_m_gutter_MarginRight"> | </SplitItem>
            <SplitItem className="l_split_m_gutter_MarginRight">
              {protocol} {generateIcons()}
            </SplitItem>
          </Split>
        </GridItem>
      </Grid>
    </PageSection>
  );
};
