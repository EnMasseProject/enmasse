/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Split,
  SplitItem,
  DropdownItem,
  DropdownPosition,
  Title,
  Flex,
  FlexItem,
  PageSectionVariants,
  PageSection
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { TypeBadge } from "modules/address-detail/components";
import { AddressTypes } from "constant";
import { DropdownWithKebabToggle } from "components";

export interface IAddressDetailHeaderProps {
  type: string;
  topic?: string | null;
  name: string;
  plan: string;
  storedMessages: number | string;
  onEdit: (name: string) => void;
  onDelete: (name: string) => void;
  onPurge: (name: string) => void;
}
const styles = StyleSheet.create({
  flex_left_border: {
    paddingLeft: "1em",
    borderLeft: "0.05em solid",
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
  },
  no_bottom_padding: {
    paddingBottom: 0
  }
});

export const AddressDetailHeader: React.FunctionComponent<IAddressDetailHeaderProps> = ({
  type,
  topic,
  name,
  plan,
  storedMessages,
  onEdit,
  onDelete,
  onPurge
}) => {
  const AddressTitle = () => (
    <Split hasGutter>
      <SplitItem>
        <Title headingLevel="h1" size="4xl" id="adheader-name">
          {name}
        </Title>
      </SplitItem>
    </Split>
  );

  const AddressDetailInFlex = () => (
    <Flex className={css(styles.namespace_info_margin)}>
      <FlexItem id="adheader-plans">
        Plan : <b>{plan}</b>
      </FlexItem>
      {topic && topic !== null && (
        <FlexItem
          id="adheader-subscription-topic"
          className={css(styles.flex_left_border)}
        >
          Topic : <b>{topic}</b>
        </FlexItem>
      )}
      {type && (type === "anycast" || type === "multicast") ? (
        ""
      ) : (
        <FlexItem
          id="adheader-stored-messages"
          className={css(styles.flex_left_border)}
        >
          <b>{storedMessages && storedMessages !== "" ? storedMessages : 0}</b>{" "}
          stored messages
        </FlexItem>
      )}
    </Flex>
  );

  const AddressDetailLayout = () => (
    <>
      <SplitItem className={css(styles.address_icon_align)}>
        <TypeBadge type={type} />
      </SplitItem>
      <SplitItem>
        <AddressTitle />
        <AddressDetailInFlex />
      </SplitItem>
    </>
  );

  const KebabOptionsLayout = () => {
    const dropdownItems = [
      <DropdownItem
        id="addr-header-edit-item-dropdown"
        key="download"
        aria-label="download"
        onClick={() => onEdit(name)}
      >
        Edit
      </DropdownItem>,
      <DropdownItem
        id="addr-header-delete-item-dropdown"
        key="delete"
        aria-label="delete"
        onClick={() => onDelete(name)}
      >
        Delete
      </DropdownItem>
    ];
    if (
      type &&
      (type.toLowerCase() === AddressTypes.QUEUE ||
        type.toLowerCase() === AddressTypes.SUBSCRIPTION)
    ) {
      dropdownItems.push(
        <DropdownItem
          id="addr-header-purge-item-dropdown"
          key="purge"
          aria-label="purge"
          onClick={() => onPurge(name)}
        >
          Purge
        </DropdownItem>
      );
    }
    return (
      <DropdownWithKebabToggle
        id="addr-header-kebab-dropdown"
        isPlain={true}
        position={DropdownPosition.right}
        toggleId="adheader-kebab"
        dropdownItems={dropdownItems}
      />
    );
  };

  return (
    <PageSection
      variant={PageSectionVariants.light}
      className={css(styles.no_bottom_padding)}
    >
      <Split>
        <AddressDetailLayout />
        <SplitItem isFilled />
        <SplitItem className={css(styles.kebab_toggle_margin)}>
          <KebabOptionsLayout />
        </SplitItem>
      </Split>
    </PageSection>
  );
};
