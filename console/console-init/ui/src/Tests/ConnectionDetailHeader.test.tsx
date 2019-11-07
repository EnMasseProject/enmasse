import * as React from 'react';
import {
  IConnectionHeaderDetailProps,
  ConnectionDetailHeader,
} from 'src/Components/ConnectionDetail/ConnectionDetailHeader';
import { render, fireEvent } from '@testing-library/react';

describe('Connection Detail Header with all connection details', () => {
  test('it renders ConnectionDetailHeader component with all props details at top of the page', () => {
    const props: IConnectionHeaderDetailProps = {
      hostname: 'myapp1',
      containerId: '1.219.2.1.33904',
      protocol: 'AMQP',
      product: 'QpidJMS',
      version: '0.31.0',
      platform: '0.8.0_152.25.125.b16, Oracle Corporation',
      os: 'Mac OS X 10.13.6,x86_64',
      messagesIn: 0,
      messagesOut: 1,
    };

    const { getByText } = render(<ConnectionDetailHeader {...props} />),
      hostnameNode = getByText(props.hostname),
      containerIdNode = getByText(props.containerId),
      protocolNode = getByText(props.protocol),
      seeMoreNode = getByText('see more details');

    expect(hostnameNode).toBeDefined();
    expect(containerIdNode).toBeDefined();
    expect(protocolNode).toBeDefined();
    expect(seeMoreNode).toBeDefined();

    fireEvent.click(seeMoreNode);

    const hideDetailsNode = getByText('hide details'),
      productNode = getByText(props.product),
      versionNode = getByText(props.version + ' SNAPSHOT'),
      platformNode = getByText(props.platform),
      osNode = getByText(props.os),
      messagesInNode = getByText(props.messagesIn + ' Message in'),
      messagesOutNode = getByText(props.messagesOut + ' Message out');

    expect(hideDetailsNode).toBeDefined();
    expect(productNode).toBeDefined();
    expect(versionNode).toBeDefined();
    expect(platformNode).toBeDefined();
    expect(osNode).toBeDefined();
    expect(messagesInNode).toBeDefined();
    expect(messagesOutNode).toBeDefined();
  });
});
