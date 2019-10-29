import * as React from "react";
import {
  PageSection,
  PageSectionVariants,
  Split,
  SplitItem,
  GridItem,
  Grid
} from "@patternfly/react-core";
import {
  LockIcon,
  LockOpenIcon,
  AngleDownIcon,
  AngleUpIcon
} from "@patternfly/react-icons";
import "./ConnectionDetail.css";
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
  return (
    <PageSection variant={PageSectionVariants.light}>
      <Grid>
        <GridItem rowSpan={12} className="connection_detail_split_m_height">
          {hostname}
        </GridItem>
        <GridItem rowSpan={12}>
          <Split>
            <SplitItem className="connection_detail_split_m_MarginRight">
              in container <b>{containerId}</b>
            </SplitItem>
            <SplitItem className="connection_detail_split_m_gutter_MarginRight"></SplitItem>
            <SplitItem className="connection_detail_split_m_MarginRight">
              {protocol} {generateIcons()}
            </SplitItem>
            <SplitItem
              className="connection_detail_split_m_dropdown"
              onClick={() => setIsHidden(!isHidden)}>
              {isHidden ? (
                <>
                  See more details <AngleDownIcon color="black" />
                </>
              ) : (
                <>
                  Hide Details <AngleUpIcon color="black" />
                </>
              )}
            </SplitItem>
          </Split>
        </GridItem>
        {isHidden ? (
          ""
        ) : (
          <>
            <Split>
              {/* <Grid> */}
                {/* <GridItem rowSpan={8}> */}
                  <SplitItem>
                    <Grid>
                      <GridItem rowSpan={12}>
                        <Split>
                          <SplitItem className="connection_detail_split_m_MarginRight">
                            <b>Product</b> {product}
                          </SplitItem>
                          <SplitItem className="connection_detail_split_m_MarginRight">
                            <b>Version</b> {version}
                          </SplitItem>
                        </Split>
                      </GridItem>
                      <GridItem rowSpan={12}>
                        <Split>
                          <SplitItem className="connection_detail_split_m_MarginRight">
                            <b>Platform JVM : </b> {platform}
                          </SplitItem>
                          <SplitItem className="connection_detail_split_m_MarginRight">
                            <b>OS:</b> {os}
                          </SplitItem>
                        </Split>
                      </GridItem>
                    </Grid>
                  </SplitItem>
                {/* </GridItem> */}
                <SplitItem className="connection_detail_split_m_large_gutter_MarginRight"></SplitItem>
                {/* <GridItem rowSpan={3}> */}
                <SplitItem className="connection_detail_split_m_MeassageItem">
                  {messagesIn || messagesIn === 0 ? messagesIn : "-"}
                  <br />
                  Messages in
                </SplitItem>
                <SplitItem className="connection_detail_split_m_MeassageItem">
                  {messagesOut || messagesOut === 0 ? messagesOut : "-"}
                  <br />
                  Messages out
                </SplitItem>
                {/* </GridItem> */}
              {/* </Grid> */}
            </Split>
          </>
        )}
      </Grid>
    </PageSection>
  );
};
