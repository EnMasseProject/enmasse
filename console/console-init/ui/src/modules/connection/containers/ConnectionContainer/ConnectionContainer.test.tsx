/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import ReactDom from "react-dom";
import { render, cleanup, wait } from "@testing-library/react";
import { MockedProvider } from "@apollo/react-testing";
import { ConnectionContainer } from "./ConnectionContainer";
import { RETURN_ALL_CONECTION_LIST } from "graphql-module/queries";

const props = {
  name: "name",
  namespace: "namespace",
  hostnames: [],
  containerIds: [],
  setTotalConnections: jest.fn(),
  page: 1,
  perPage: 10,
  sortValue: undefined,
  setSortValue: jest.fn(),
  addressSpaceType: undefined
};

afterEach(cleanup);

const setup = (mocks: any, props: any) => {
  return render(
    <MockedProvider mocks={mocks} addTypename={false}>
      <ConnectionContainer {...props} />
    </MockedProvider>
  );
};

describe("<ConnectionContainer/>", () => {
  it("should render without crashing", () => {
    const div = document.createElement("div");
    ReactDom.render(
      <MockedProvider>
        <ConnectionContainer {...props} />
      </MockedProvider>,
      div
    );
    ReactDom.unmountComponentAtNode(div);
  });

  it("should render loader if loading is true", () => {
    const { container } = setup([], props);
    wait(() => expect(container).toHaveTextContent("Loading"));
  });

  it("should not render loader if loading false", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_ALL_CONECTION_LIST(
            1,
            10,
            [],
            [],
            "name",
            "namespace",
            undefined
          )
        },
        result: {
          data: null
        }
      }
    ];

    const { container } = setup(mocks, props);
    //wait for response

    await wait(() => expect(container).not.toHaveTextContent("Loading"));
  });

  it("should render <ConnectionList/> component if loading is false", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_ALL_CONECTION_LIST(
            1,
            10,
            [],
            [],
            "name",
            "namespace",
            undefined
          )
        },
        result: {
          data: null
        }
      }
    ];

    const { container } = setup(mocks, props);
    //wait for response

    //check table headers
    await wait(() => expect(container).toHaveTextContent("Hostname"));
    expect(container).toHaveTextContent("Protocol");
  });

  it("should render <EmptyConnection/> if total links is not greater than zero (0)", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_ALL_CONECTION_LIST(
            1,
            10,
            [],
            [],
            "name",
            "namespace",
            undefined
          )
        },
        result: {
          data: { connections: { total: 0, connections: [] } }
        }
      }
    ];

    const { container } = setup(mocks, props);

    await wait(() => expect(container).toHaveTextContent("No connections"));
    expect(container).toHaveTextContent(
      "You currently don't have any connections"
    );
  });

  it("should not render <EmptyConnection/> if total links is greater than zero (0)", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_ALL_CONECTION_LIST(
            1,
            10,
            [],
            [],
            "name",
            "namespace",
            undefined
          )
        },
        result: {
          data: { connections: { total: 5 } }
        }
      }
    ];

    const { container } = setup(mocks, props);
    cleanup();

    await wait(() => expect(container).not.toHaveTextContent("No connections"));
    expect(container).not.toHaveTextContent(
      "You currently don't have any connections"
    );
  });
});
