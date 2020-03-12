/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useCallback } from "react";
import { useHistory, useLocation } from "react-router";
import {
  Pagination as TablePagination,
  PaginationProps
} from "@patternfly/react-core";

export const Pagination: React.FC<PaginationProps> = ({
  page,
  perPage,
  itemCount,
  variant
}) => {
  const history = useHistory();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);

  const setSearchParam = React.useCallback(
    (name: string, value: string) => {
      searchParams.set(name, value.toString());
    },
    [searchParams]
  );

  const onSetPage = useCallback(
    (_: any, newPage: number) => {
      setSearchParam("page", newPage.toString());
      history.push({
        search: searchParams.toString()
      });
    },
    [setSearchParam, history, searchParams]
  );

  const onPerPageSelect = useCallback(
    (_: any, newPerPage: number) => {
      setSearchParam("page", "1");
      setSearchParam("perPage", newPerPage.toString());
      history.push({
        search: searchParams.toString()
      });
    },
    [setSearchParam, history, searchParams]
  );

  if (itemCount && itemCount > 0) {
    return (
      <TablePagination
        itemCount={itemCount}
        perPage={perPage}
        page={page}
        onSetPage={onSetPage}
        variant={variant || "top"}
        onPerPageSelect={onPerPageSelect}
      />
    );
  }
  return null;
};
