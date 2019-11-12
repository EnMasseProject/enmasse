import * as React from "react";
import {
  Split,
  SplitItem,
  Dropdown,
  DropdownItem,
  DropdownPosition,
  KebabToggle,
  Card,
  CardHeader,
  Title,
  CardBody,
  Flex,
  FlexItem
} from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";

const styles = StyleSheet.create({
  flex_right_border: {
    paddingRight: "1em",
    borderRight: "0.1em solid",
    borderRightColor: "lightgrey"
  }
});

export interface IAddressDetailHeaderProps {
  type: string;
  name: string;
  plan: string;
  shards: number;
  onEdit: (name: string) => void;
  onDelete: (name: string) => void;
}

export const AddressDetailHeader: React.FunctionComponent<
  IAddressDetailHeaderProps
> = ({ type, name, plan, shards, onEdit, onDelete }) => {
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
      onClick={() => onEdit(name)}
    >
      Edit
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
            <b>{plan}</b>
          </FlexItem>
          <FlexItem>
            Stored in <b>{shards}</b> Shard
          </FlexItem>
        </Flex>
      </CardBody>
    </Card>
  );
};
