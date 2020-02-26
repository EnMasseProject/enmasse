/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import {
  Split,
  SplitItem,
  Dropdown,
  DropdownItem,
  DropdownPosition,
  KebabToggle,
  Title,
  Flex,
  FlexItem,
  PageSectionVariants,
  PageSection
} from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";
import { TypeBadge } from "components/common/TypePlan";

export interface IAddressDetailHeaderProps {
  type: string;
  topic: string | null;
  name: string;
  plan: string;
  partitions: number | string;
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
  partitions,
  storedMessages,
  onEdit,
  onDelete,
  onPurge
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
      id="adheader-dropdown-item-edit"
      key="download"
      aria-label="download"
      onClick={() => onEdit(name)}
    >
      Edit
    </DropdownItem>,
    <DropdownItem
      id="adheader-dropdown-item-delete"
      key="delete"
      aria-label="delete"
      onClick={() => onDelete(name)}
    >
      Delete
    </DropdownItem>
  ];
  if (
    type &&
    (type.toLowerCase() === "queue" || type.toLowerCase() === "subscription")
  ) {
    dropdownItems.push(
      <DropdownItem
        id="adheader-dropdown-item-purge"
        key="purge"
        aria-label="purge"
        onClick={() => onPurge(name)}
      >
        Purge
      </DropdownItem>
    );
  }
  return (
    <PageSection
      variant={PageSectionVariants.light}
      className={css(styles.no_bottom_padding)}
    >
      <Split>
        <SplitItem className={css(styles.address_icon_align)}>
          <TypeBadge type={type} />
        </SplitItem>
        <SplitItem>
          <Split gutter="md">
            <SplitItem>
              <Title headingLevel="h1" size="4xl" id="adheader-name">
                {name}
              </Title>
            </SplitItem>
          </Split>
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
                <b>
                  {storedMessages && storedMessages !== "" ? storedMessages : 0}
                </b>{" "}
                stored messages
              </FlexItem>
            )}
          </Flex>
        </SplitItem>
        <SplitItem isFilled></SplitItem>
        <SplitItem className={css(styles.kebab_toggle_margin)}>
          <Dropdown
            id="adheader-dropdown"
            onSelect={onSelect}
            position={DropdownPosition.right}
            toggle={<KebabToggle id="adheader-kebab" onToggle={onToggle} />}
            isOpen={isOpen}
            isPlain={true}
            dropdownItems={dropdownItems}
          />
        </SplitItem>
      </Split>
    </PageSection>
  );
};
