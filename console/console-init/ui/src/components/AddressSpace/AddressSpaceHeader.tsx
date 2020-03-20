/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import {
  DropdownPosition,
  DropdownItem,
  Title,
  Flex,
  FlexItem,
  Split,
  SplitItem,
  Badge
} from "@patternfly/react-core";
import { FormatDistance } from "use-patternfly";
import { css, StyleSheet } from "@patternfly/react-styles";
import { AddressSpaceType } from "components/common/AddressSpaceListFormatter";
import { DropdownWithKababToggle } from "components";

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
  onEdit: () => void;
}
export const AddressSpaceHeader: React.FunctionComponent<IAddressSpaceHeaderProps> = ({
  name,
  namespace,
  createdOn,
  type,
  onDownload,
  onDelete,
  onEdit
}) => {
  const dropdownItems = [
    <DropdownItem
      key="edit"
      id="as-header-edit"
      aria-label="edit"
      onClick={() => onEdit()}
    >
      Edit
    </DropdownItem>,
    <DropdownItem
      key="delete"
      id="as-header-delete"
      aria-label="delete"
      onClick={() => onDelete({ name, namespace })}
    >
      Delete
    </DropdownItem>,
    <DropdownItem
      key="download"
      id="as-header-download"
      aria-label="download"
      onClick={() => onDownload({ name, namespace })}
      style={{ paddingRight: 50 }}
    >
      Download Certificate
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
          <DropdownWithKababToggle
            id="as-header-dropdown"
            toggleId={"as-header-kebab"}
            position={DropdownPosition.right}
            isPlain={true}
            dropdownItems={dropdownItems}
          />
        </SplitItem>
      </Split>
    </>
  );
};
