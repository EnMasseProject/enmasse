import * as React from 'react';
import { Flex, FlexItem } from '@patternfly/react-core';

export interface IConnectionDetailProps {
  product: string;
  version: string;
  jvm: string;
  os: string;
  isMobileView:boolean;
}
export const ConnectionDetail: React.FunctionComponent<IConnectionDetailProps> = ({product,version,jvm,os,isMobileView}) => {
  return (
      <Flex breakpointMods={[{modifier: "column", breakpoint:"sm"}]}
      style={!isMobileView ? {paddingRight:'48px', marginRight:'48px',borderRight:'2px solid',borderRightColor:'lightgrey'} : {borderBottom:'2px solid',borderBottomColor:'lightgrey',paddingBottom:'12px'}}
      >
        <Flex>
            <FlexItem><b>Product</b> {product}</FlexItem>
            <FlexItem><b>Version</b>{version} SNAPSHOT</FlexItem>
        </Flex>
        <Flex>
            <FlexItem><b>Platform</b></FlexItem>
            <FlexItem>
            <Flex breakpointMods={[{modifier: "row", breakpoint:"lg"},{modifier:"column",breakpoint:"sm"}]}>
                <FlexItem><b>JVM: </b>{jvm}</FlexItem>
                <FlexItem><b>OS: </b>{os}</FlexItem>
            </Flex>
            </FlexItem>
      </Flex>
      </Flex>
  );
};
