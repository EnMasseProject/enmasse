/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { useEffect, DependencyList } from "react";
import { useHistory, useLocation } from "react-router-dom";

// a custom hook for setting the page search param
export const useSearchParamsPageChange = (
  dependencies: DependencyList,
  pageValue: string = "1"
) => {
  const history = useHistory();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10);

  useEffect(() => {
    if (page > 1) {
      searchParams.set("page", pageValue);
      history.push({
        search: searchParams.toString()
      });
    }
  }, [...dependencies]);
};
