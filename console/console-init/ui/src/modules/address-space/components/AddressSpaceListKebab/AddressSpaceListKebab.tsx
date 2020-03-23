/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  OverflowMenu,
  OverflowMenuContent,
  OverflowMenuGroup,
  OverflowMenuItem,
  OverflowMenuControl
} from "@patternfly/react-core/dist/js/experimental";
import {
  Dropdown,
  DropdownItem,
  Button,
  ButtonVariant,
  KebabToggle
} from "@patternfly/react-core";

export interface IAddressSpaceListKebabProps {
  onCreateAddressSpace: () => void;
  isDeleteAllDisabled: boolean;
  onSelectDeleteAll: (event: any) => void;
}

export const AddressSpaceListKebab: React.FC<IAddressSpaceListKebabProps> = ({
  isDeleteAllDisabled,
  onCreateAddressSpace,
  onSelectDeleteAll
}) => {
  const [isKebabOpen, setIsKebabOpen] = useState(false);

  const onKebabToggle = (isOpen: boolean) => {
    setIsKebabOpen(isOpen);
  };

  const dropdownItems = [
    <DropdownItem
      id="as-list-delete-all"
      key="delete-all"
      component="button"
      value="deleteAll"
      isDisabled={isDeleteAllDisabled}
    >
      Delete Selected
    </DropdownItem>
  ];

  return (
    <>
      <OverflowMenu breakpoint="lg">
        <OverflowMenuContent isPersistent>
          <OverflowMenuGroup groupType="button" isPersistent>
            {/* Remove is Persistent after fixing dropdown items for overflow menu */}
            <OverflowMenuItem isPersistent>
              <Button
                id="al-filter-overflow-button"
                variant={ButtonVariant.primary}
                onClick={onCreateAddressSpace}
              >
                Create Address Space
              </Button>
            </OverflowMenuItem>
          </OverflowMenuGroup>
        </OverflowMenuContent>
        <OverflowMenuControl hasAdditionalOptions>
          <Dropdown
            id="al-filter-overflow-dropdown"
            onSelect={onSelectDeleteAll}
            toggle={
              <KebabToggle
                id="al-filter-overflow-kebab"
                onToggle={onKebabToggle}
              />
            }
            isOpen={isKebabOpen}
            isPlain
            dropdownItems={dropdownItems}
          />
        </OverflowMenuControl>
      </OverflowMenu>
    </>
  );
};
