import React from "react";
import { useA11yRouteChange, useDocumentTitle } from "use-patternfly";
import { PageSection, Button, Modal } from "@patternfly/react-core";
import { AddressList, IAddress } from "../Components/AddressSpace/AddressList";
import {
  ConnectionList,
  IConnection
} from "../Components/AddressSpace/ConnectionList";
import { EditAddress } from "../Pages/EditAddressPage";

const addressRows: IAddress[] = [
  {
    name: 'foo',
    namespace:"foo",
    type: 'Queue',
    plan: 'small',
    messagesIn: 123,
    messagesOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    shards: 123,
    status: 'running',
  },
  {
    name: 'foo',
    namespace:"foo",
    type: 'Queue',
    plan: 'small',
    messagesIn: 123,
    messagesOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    shards: 123,
    status: 'creating',
  },
  {
    name: 'foo',
    namespace:"foo",
    type: 'Queue',
    plan: 'small',
    messagesIn: 123,
    messagesOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    shards: 123,
    status: 'deleting',
  },
];

const IndexPage: React.FC = ({ children }) => {
  useA11yRouteChange();
  useDocumentTitle('Index Page');

  const [
    addressBeingEdited,
    setAddressBeingEdited,
  ] = React.useState<IAddress | null>();

  const handleDelete = (data: IAddress) => void 0;
  const handleEdit = (data: IAddress) => {
    if (!addressBeingEdited) {
      setAddressBeingEdited(data);
    }
  };
  const handleCancelEdit = () => setAddressBeingEdited(null);
  const handleSaving = () => void 0;
  const handleEditChange = (address: IAddress) =>
    setAddressBeingEdited(address);

  return (
    <React.Fragment>
      <PageSection>
        <AddressList
          rows={addressRows}
          onEdit={handleEdit}
          onDelete={handleDelete}
        />
      </PageSection>
      {addressBeingEdited && (
        <Modal
          title="Modal Header"
          isOpen={true}
          onClose={handleCancelEdit}
          actions={[
            <Button key="confirm" variant="primary" onClick={handleSaving}>
              Confirm
            </Button>,
            <Button key="cancel" variant="link" onClick={handleCancelEdit}>
              Cancel
            </Button>,
          ]}
          isFooterLeftAligned
        >
          <EditAddress
            address={addressBeingEdited}
            onChange={handleEditChange}
          />
        </Modal>
      )}
    </React.Fragment>
  );
};

export default IndexPage;
