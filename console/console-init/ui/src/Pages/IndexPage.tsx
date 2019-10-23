import React from "react";
import { useA11yRouteChange, useDocumentTitle } from "use-patternfly";
import { PageSection, Button, Modal } from "@patternfly/react-core";
import ResourceList, { IAddress } from "src/Components/ResourceList";

const rows: IAddress[] = [
  {
    name: "foo",
    typePlan: "small",
    messagesIn: 123,
    messagesOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    shards: 123,
    status: "running"
  },
  {
    name: "foo",
    typePlan: "small",
    messagesIn: 123,
    messagesOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    shards: 123,
    status: "creating"
  },
  {
    name: "foo",
    typePlan: "small",
    messagesIn: 123,
    messagesOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    shards: 123,
    status: "deleting"
  }
];

interface IEditAddressProps {
  address: IAddress;
  onChange: (address: IAddress) => void;
}
const EditAddress: React.FC<IEditAddressProps> = ({ address }) => {
  return <pre>{JSON.stringify(address, null, 2)}</pre>;
};

const IndexPage: React.FC = ({ children }) => {
  useA11yRouteChange();
  useDocumentTitle("Index Page");

  const [
    addressBeingEdited,
    setAddressBeingEdited
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
        <h1>Index page</h1>
        <ResourceList rows={rows} onEdit={handleEdit} onDelete={handleDelete} />
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
            </Button>
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
