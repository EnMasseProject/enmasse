import * as React from 'react';
import { Flex, FlexItem } from '@patternfly/react-core';

export interface IMessagesDetail {
  messagesIn: number;
  messagesOut: number;
  isMobileView:boolean;
}

export const MessagesDetail: React.FunctionComponent<IMessagesDetail> = ({
  messagesIn,
  messagesOut,
  isMobileView,
}) => {
  return (
    <Flex breakpointMods={[{ modifier: 'row', breakpoint: 'sm' }]}>
      <FlexItem style={{
                    paddingTop: '15px',
                    textAlign: 'center',
                    fontSize: '21px',
                    marginRight:'48px'
                  }}>
        {messagesIn || messagesIn === 0 ? messagesIn : '-'} {isMobileView ? '' :<br/>} Message in
      </FlexItem>
      <FlexItem style={{
                    paddingTop: '15px',
                    textAlign: 'center',
                    fontSize: '21px',
                  }}>
        {messagesOut || messagesOut === 0 ? messagesOut : '-'}  {isMobileView ? '' :<br/>} Message out
      </FlexItem>
    </Flex>
  );
};
