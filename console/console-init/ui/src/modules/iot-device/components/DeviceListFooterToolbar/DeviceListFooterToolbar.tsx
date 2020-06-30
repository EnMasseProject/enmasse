/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { TablePagination } from "components";
import {
  ToolbarContent,
  Toolbar,
  ToolbarItem,
  PaginationProps
} from "@patternfly/react-core";

export const DeviceListFooterToolbar: React.FunctionComponent<PaginationProps> = ({
  itemCount,
  perPage,
  page,
  onSetPage,
  onPerPageSelect
}) => {
  return (
    <Toolbar id="device-footer-toolbar" data-codemods="true">
      <ToolbarContent id="device-footer-toolbar-content">
        <ToolbarItem
          id="device-footer-toolbar-item-1"
          variant="pagination"
          key="pagination"
          alignment={{ md: "alignRight" }}
          aria-label="Device List pagination"
          data-codemods="true"
        >
          <TablePagination
            id="device-list-footer-pagination"
            itemCount={itemCount}
            perPage={perPage}
            page={page}
            onSetPage={onSetPage}
            onPerPageSelect={onPerPageSelect}
          />
        </ToolbarItem>
      </ToolbarContent>
    </Toolbar>
  );
};
