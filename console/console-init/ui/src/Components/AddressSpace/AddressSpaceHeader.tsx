import * as React from "react";
import {
  Dropdown,
  DropdownPosition,
  KebabToggle,
  DropdownItem,
  Title,
  Flex,
  FlexItem,
  Split,
  SplitItem,
  Card,
  CardHeader,
  CardBody
} from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";

const styles = StyleSheet.create({
  flex_right_border: {
    paddingRight: "1em",
    borderRight: "0.1em solid",
    borderRightColor: "lightgrey"
  }
});
export interface IAddressSpaceProps {
  name: string;
  namespace: string;
  createdOn: string;
  type: string;
  onDownload: (name: string) => void;
  onDelete: (name: string) => void;
}
export const AddressSpaceHeader: React.FunctionComponent<IAddressSpace> = ({
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
      onClick={() => onDownload(name)}
    >
      Download Certificate
    </DropdownItem>,
    <DropdownItem
      key="delete"
      aria-label="delete"
      onClick={() => onDelete(name)}
    >
      Delete
    </DropdownItem>
  ];
  return (
    <Card>
      <CardHeader>
        <Split gutter="md">
          <SplitItem>
            <Title headingLevel="h1" size="4xl">
              {name}
            </Title>
          </SplitItem>
          <SplitItem isFilled></SplitItem>
          <SplitItem>
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
      </CardHeader>
      <CardBody>
        <Flex>
          <FlexItem className={css(styles.flex_right_border)}>
            in namespace <b>{namespace}</b>
          </FlexItem>
          <FlexItem className={css(styles.flex_right_border)}>
            <b>{type}</b>
          </FlexItem>
          <FlexItem>
            Created <b>{createdOn}</b>
          </FlexItem>
        </Flex>
      </CardBody>
    </Card>
  );
};
