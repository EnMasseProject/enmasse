import * as React from "react";
import {
  PageSection,
  PageSectionVariants,
  Split,
  SplitItem,
  Dropdown,
  DropdownPosition,
  KebabToggle,
  DropdownItem
} from "@patternfly/react-core";
import { AwardIcon } from "@patternfly/react-icons";
import "./AddressSpaceHeader.css";

export interface AddressSpace {
  name: string;
  namespace: string;
  createdOn: string;
  type: string;
  onDownload: (name: string) => void;
  onDelete: (name: string) => void;
}
export const AddressSpaceHeader: React.FunctionComponent<AddressSpace> = ({
  name,
  namespace,
  createdOn,
  type,
  onDownload,
  onDelete
}) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const onSelect = (result: any) => {
    setIsOpen(!isOpen);
  };
  const onToggle = () => {
    setIsOpen(!isOpen);
  };
  const dropdownItems = [
    <DropdownItem
      key="download"
      aria-label="download"
      onClick={() => onDownload(name)}>
      Download Certificate
    </DropdownItem>,
    <DropdownItem
      key="delete"
      aria-label="delete"
      onClick={() => onDelete(name)}>
      Delete
    </DropdownItem>
  ];
  return (
    <PageSection variant={PageSectionVariants.light}>
      <Split>
        <SplitItem className="address_space_split_m_padding">
          <AwardIcon size="lg" />
        </SplitItem>
        <SplitItem className="address_space_split_m_height">
          {name}
          <br />
          <Split>
            <SplitItem className="address_space_split_m_gutter_MarginRight">
              in namespace <b>{namespace}</b>
            </SplitItem>
            <SplitItem className="address_space_split_m_gutter_MarginRight"> | </SplitItem>
            <SplitItem className="address_space_split_m_gutter_MarginRight">
              <b> {type} </b>
            </SplitItem>
            <SplitItem className="address_space_split_m_gutter_MarginRight"> | </SplitItem>
            <SplitItem className="address_space_split_m_gutter_MarginRight">
              Created <b>{createdOn} </b>
            </SplitItem>
          </Split>
        </SplitItem>
        <SplitItem isFilled></SplitItem>
        <SplitItem className="address_space_split_m_gutter_MarginRight">
            <Dropdown
              onSelect={onSelect}
              position={DropdownPosition.right}
              toggle={<KebabToggle onToggle={onToggle} />}
              isOpen={isOpen}
              isPlain={true}
              dropdownItems={dropdownItems}
            />
        </SplitItem>
      </Split>
    </PageSection>
  );
};
