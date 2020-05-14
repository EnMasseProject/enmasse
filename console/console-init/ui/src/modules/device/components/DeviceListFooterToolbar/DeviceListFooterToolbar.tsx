/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { TablePagination } from "components";
import {
  DataToolbarContent,
  DataToolbar,
  DataToolbarItem,
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
    <DataToolbar id="device-footer-toolbar">
      <DataToolbarContent id="device-footer-toolbar-content">
        <DataToolbarItem
          id="device-footer-toolbar-item-1"
          variant="pagination"
          key="pagination"
          breakpointMods={[{ modifier: "align-right", breakpoint: "md" }]}
          aria-label="Device List pagination"
        >
          <TablePagination
            id="device-list-footer-pagination"
            itemCount={itemCount}
            perPage={perPage}
            page={page}
            onSetPage={onSetPage}
            onPerPageSelect={onPerPageSelect}
          />
        </DataToolbarItem>
      </DataToolbarContent>
    </DataToolbar>
  );
};
