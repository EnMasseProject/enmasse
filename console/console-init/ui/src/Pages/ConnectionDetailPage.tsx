import * as React from 'react';
import { ConnectionDetailHeader } from 'src/Components/ConnectionDetail/ConnectionDetailHeader';
import { PageSection, PageSectionVariants } from '@patternfly/react-core';
import {
  ConnectionList,
  IConnection,
} from 'src/Components/AddressSpace/ConnectionList';

export default function ConnectionDetailPage() {
  const props = {
    hostname: '1.219.2.1.33904',
    containerId: 'myapp1',
    protocol: 'AMQP',
    product: 'QpidJMS',
    version: '0.31.0',
    platform: '0.8.0_152.25.125.b16',
    os: 'Mac OS X 10.12.6,x86_64',
    messagesIn: 0,
    messagesOut: 0,
  };
  const rows: IConnection[] = [
    {
      hostname: 'foo',
      containerId: '123',
      protocol: 'AMQP',
      messagesIn: 123,
      messagesOut: 123,
      senders: 123,
      receivers: 123,
      status: 'running',
    },
    {
      hostname: 'foo',
      containerId: '123',
      protocol: 'AMQP',
      messagesIn: 123,
      messagesOut: 123,
      senders: 123,
      receivers: 123,
      status: 'running',
    },
    {
      hostname: 'foo',
      containerId: '123',
      protocol: 'AMQP',
      messagesIn: 123,
      messagesOut: 123,
      senders: 123,
      receivers: 123,
      status: 'running',
    },
  ];
  return (
    <>
      <PageSection variant={PageSectionVariants.light}>
        <h1>Connection Detial Page</h1>
      </PageSection>
      <ConnectionDetailHeader
        hostname={props.hostname}
        containerId={props.containerId}
        protocol={props.protocol}
        product={props.product}
        version={props.version}
        platform={props.platform}
        os={props.os}
        messagesIn={props.messagesIn}
        messagesOut={props.messagesOut}
      />
      <PageSection>
        <ConnectionList rows={rows} />
      </PageSection>
    </>
  );
}
