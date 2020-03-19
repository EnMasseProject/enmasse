import React, { useState } from "react";
import {
  DropdownItem,
  Button,
  ButtonVariant,
  Dropdown,
  KebabToggle
} from "@patternfly/react-core";
import {
  OverflowMenu,
  OverflowMenuContent,
  OverflowMenuGroup,
  OverflowMenuItem,
  OverflowMenuControl
} from "@patternfly/react-core/dist/js/experimental";

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
  const [isKebabOpen, setIsKebabOpen] = useState(false);
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
  const onKebabToggle = (isOpen: boolean) => {
    setIsKebabOpen(isOpen);
  };

  const onKebabSelect = async (event: any) => {
    if (event.target.value) {
      if (event.target.value === "purgeAll") {
        await onPurgeAllAddress();
      } else if (event.target.value === "deleteAll") {
        await onDeleteAllAddress();
      }
    }
    setIsKebabOpen(!isKebabOpen);
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
          <Dropdown
            id="al-filter-overflow-dropdown"
            onSelect={onKebabSelect}
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
