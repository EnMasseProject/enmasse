/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render, fireEvent } from "@testing-library/react";
import { NoResultFound, INoResultFound } from "./NoResultFound";

describe("<NoResultFound />", () => {
  const props: INoResultFound = {
    clearFilters: jest.fn()
  };

  it("should render an empty state if filters return no result", () => {
    const { getByText } = render(<NoResultFound {...props} />);

    const titleNode = getByText("No results found");
    const bodyNode = getByText("No results match the filter criteria.");
    const clearBtn = getByText("Clear all filters");

    expect(titleNode).toBeInTheDocument();
    expect(bodyNode).toBeInTheDocument();
    expect(clearBtn).toBeInTheDocument();

    fireEvent.click(clearBtn);
  });
});
