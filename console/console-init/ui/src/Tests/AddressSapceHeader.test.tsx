import * as React from 'react';
import {
  IAddressSpace,
  AddressSpaceHeader,
} from 'src/Components/AddressSpace/AddressSpaceHeader';
import { render } from '@testing-library/react';

describe('Address Space Detail', () => {
  test('it renders address space headers at top', () => {
    const props: IAddressSpace = {
      name: 'jBoss',
      namespace: 'devops_jbosstest1',
      createdOn: '2 days ago',
      type: 'Brokered',
      onDownload: () => {},
      onDelete: () => {},
    };

    const { getByText } = render(<AddressSpaceHeader {...props} />);

    const nameNode = getByText(props.name);
    const namespaceNode = getByText(props.namespace);
    const typeNode= getByText(props.type);
    const createdOnNode = getByText(props.createdOn);

    expect(nameNode).toBeDefined();
    expect(namespaceNode).toBeDefined();
    expect(typeNode).toBeDefined();
    expect(createdOnNode).toBeDefined();
  });
});
