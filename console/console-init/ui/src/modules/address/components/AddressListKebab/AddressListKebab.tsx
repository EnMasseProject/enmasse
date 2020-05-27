/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { DropdownItem, Button, ButtonVariant } from "@patternfly/react-core";
import {
  OverflowMenu,
  OverflowMenuContent,
  OverflowMenuGroup,
  OverflowMenuItem,
  OverflowMenuControl
} from "@patternfly/react-core/dist/js/experimental";
import { DropdownWithKebabToggle } from "components";

interface IAddressListKebabProps {
  createAddressOnClick: () => void;
  onDeleteAllAddress: () => void;
  onPurgeAllAddress: () => void;
  isDeleteAllDisabled: boolean;
  isPurgeAllDisabled: boolean;
}

export const AddressListKebab: React.FunctionComponent<IAddressListKebabProps> = ({
  createAddressOnClick,
  onDeleteAllAddress,
  onPurgeAllAddress,
  isDeleteAllDisabled,
  isPurgeAllDisabled
}) => {
  const dropdownItems = [
    <DropdownItem
      id="al-filter-dropdown-item-deleteall"
      key="delete-all"
      value="deleteAll"
      component="button"
      isDisabled={isDeleteAllDisabled}
    >
      Delete Selected
    </DropdownItem>,
    <DropdownItem
      id="al-filter-dropdown-item-purgeall"
      key="purge-all"
      value="purgeAll"
      component="button"
      isDisabled={isPurgeAllDisabled}
    >
      Purge Selected
    </DropdownItem>
  ];

  const onKebabSelect = async (event: any) => {
    if (event.target.value) {
      if (event.target.value === "purgeAll") {
        await onPurgeAllAddress();
      } else if (event.target.value === "deleteAll") {
        await onDeleteAllAddress();
      }
    }
  };
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
                onClick={createAddressOnClick}
              >
                Create Address
              </Button>
            </OverflowMenuItem>
          </OverflowMenuGroup>
        </OverflowMenuContent>
        <OverflowMenuControl hasAdditionalOptions>
          <DropdownWithKebabToggle
            id="al-select-kebab-overflow-dropdown-"
            toggleId="al-filter-overflow-kebab"
            onSelect={onKebabSelect}
            dropdownItems={dropdownItems}
            isPlain
          />
        </OverflowMenuControl>
      </OverflowMenu>
    </>
  );
};
