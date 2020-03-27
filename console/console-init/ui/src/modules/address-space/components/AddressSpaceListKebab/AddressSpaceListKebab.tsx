/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  OverflowMenu,
  OverflowMenuContent,
  OverflowMenuGroup,
  OverflowMenuItem,
  OverflowMenuControl
} from "@patternfly/react-core/dist/js/experimental";
import { DropdownItem, Button, ButtonVariant } from "@patternfly/react-core";
import { DropdownWithKebabToggle } from "components";

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
          <DropdownWithKebabToggle
            id="al-filter-overflow-dropdown"
            onSelect={onSelectDeleteAll}
            dropdownItems={dropdownItems}
            isPlain
          />
        </OverflowMenuControl>
      </OverflowMenu>
    </>
  );
};
