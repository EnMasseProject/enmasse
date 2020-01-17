/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

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
    paddingTop: 5,
    paddingRight: 16
  },
  kebab_toggle_margin: {
    marginTop: 20,
    marginLeft: 10,
    fontSize: 15
  },
  namespace_info_margin: {
    marginTop: 16,
    marginBottom: 24
  }
});
export interface IAddressSpaceHeaderProps {
  name: string;
  namespace: string;
  createdOn: string;
  type: string;
  onDownload: (data: { name: string; namespace: string }) => void;
  onDelete: (data: { name: string; namespace: string }) => void;
}
export const AddressSpaceHeader: React.FunctionComponent<IAddressSpaceHeaderProps> = ({
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
      onClick={() => onDownload({ name, namespace })}
      style={{ paddingRight: 50 }}
    >
      Download Certificate
    </DropdownItem>,
    <DropdownItem
      key="delete"
      aria-label="delete"
      onClick={() => onDelete({ name, namespace })}
    >
      Delete
    </DropdownItem>
  ];
  return (
    <>
      <Split>
        <SplitItem className={css(styles.address_icon_align)}>
          <Badge className={css(styles.address_space_icon_margin)}>AS</Badge>
        </SplitItem>
        <SplitItem>
          <Split gutter="md">
            <SplitItem>
              <Title headingLevel="h1" size="4xl" id="as-header-title">
                {name}
              </Title>
            </SplitItem>
          </Split>
          <Flex className={css(styles.namespace_info_margin)}>
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
                <FormatDistance date={createdOn} /> ago
              </b>
            </FlexItem>
          </Flex>
        </SplitItem>
        <SplitItem isFilled></SplitItem>
        <SplitItem className={css(styles.kebab_toggle_margin)}>
          <Dropdown
            id="as-header-dropdown"
            onSelect={onSelect}
            position={DropdownPosition.right}
            toggle={<KebabToggle onToggle={onToggle} />}
            isOpen={isOpen}
            isPlain={true}
            dropdownItems={dropdownItems}
          />
        </SplitItem>
      </Split>
    </>
  );
};
