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
} from "@patternfly/react-core";
import { DropdownItem } from "@patternfly/react-core";
import { DropdownWithKebabToggle } from "components";
import { CreateProject } from "modules/project/dailogs";

export interface IProjectListKebabProps {
  isDeleteAllDisabled: boolean;
  onSelectDeleteAll: (event: any) => void;
}

export const ProjectListKebab: React.FC<IProjectListKebabProps> = ({
  isDeleteAllDisabled,
  onSelectDeleteAll
}) => {
  const dropdownItems = [
    <DropdownItem
      id="project-list-kebab-delete-selected-dropdownitem"
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
              <CreateProject />
            </OverflowMenuItem>
          </OverflowMenuGroup>
        </OverflowMenuContent>
        <OverflowMenuControl hasAdditionalOptions>
          <DropdownWithKebabToggle
            id="project-list-kebab-dropdowntoggle"
            toggleId="project-list-kebab-dropdown-toggle"
            onSelect={onSelectDeleteAll}
            dropdownItems={dropdownItems}
            isPlain
          />
        </OverflowMenuControl>
      </OverflowMenu>
    </>
  );
};
