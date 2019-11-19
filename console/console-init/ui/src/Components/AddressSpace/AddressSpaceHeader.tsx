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
  CardBody,
  Badge
} from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";
import { AddressSpaceType } from "../Common/AddressSpaceListFormatter";
import { FormatDistance } from "use-patternfly";

const styles = StyleSheet.create({
  flex_right_border: {
    paddingRight: "1em",
    borderRight: "0.05em solid",
    borderRightColor: "lightgrey"
  },
  address_space_icon_margin: {
    backgroundColor: "#EC7A08",
    fontSize: 25
  },
  address_icon_align: {
    paddingTop: 24,
    paddingLeft: 18
  },
  kebab_toggle_margin: {
    marginTop: 30,
    marginLeft: 10,
    fontSize: 15
  }
});
export interface IAddressSpaceHeaderProps {
  name: string;
  namespace: string;
  createdOn: string;
  type: string;
  onDownload: (name: string) => void;
  onDelete: (name: string) => void;
}
export const AddressSpaceHeader: React.FunctionComponent<
  IAddressSpaceHeaderProps
> = ({ name, namespace, createdOn, type, onDownload, onDelete }) => {
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
      style={{paddingRight:50}}
      >
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
    <Card>
      <Split>
        <SplitItem className={css(styles.address_icon_align)}>
          <Badge className={css(styles.address_space_icon_margin)}>AS</Badge>
        </SplitItem>
        <SplitItem>
          <CardHeader>
            <Split gutter="md">
              <SplitItem>
                <Title headingLevel="h1" size="4xl">
                  {name}
                </Title>
              </SplitItem>
            </Split>
          </CardHeader>
          <CardBody>
            <Flex>
              <FlexItem className={css(styles.flex_right_border)}>
                in namespace <b>{namespace}</b>
              </FlexItem>
              <FlexItem className={css(styles.flex_right_border)}>
                <b>
                  <AddressSpaceType type={type} />
                </b>
              </FlexItem>
              <FlexItem>
                Created{" "}
                <b>
                  <FormatDistance date={createdOn} />
                </b>
              </FlexItem>
            </Flex>
          </CardBody>
        </SplitItem>
        <SplitItem isFilled></SplitItem>
        <SplitItem className={css(styles.kebab_toggle_margin)}>
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
    </Card>
  );
};
