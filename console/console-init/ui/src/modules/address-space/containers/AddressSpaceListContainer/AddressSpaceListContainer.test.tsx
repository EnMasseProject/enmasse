/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import ReactDom from "react-dom";
import { render, cleanup } from "@testing-library/react";
import { MockedProvider } from "@apollo/react-testing";
import wait from "waait";
import { AddressSpaceListContainer } from "./AddressSpaceListContainer";
import { RETURN_ALL_ADDRESS_SPACES } from "graphql-module/queries";

afterEach(cleanup);

const setup = (mocks: any, props: any) => {
  return render(
    <MockedProvider mocks={mocks} addTypename={false}>
      <AddressSpaceListContainer {...props} />
    </MockedProvider>
  );
};

describe("<AddressSpaceListContainer/>", () => {
  const props = {
    page: 1,
    perPage: 10,
    totalItemsCount: 5,
    filterNames: [],
    filterNamespaces: [],
    filterType: "",
    sortValue: undefined,
    selectedAddressSpaces: [],
    setTotalAddressSpaces: jest.fn(),
    setSortValue: jest.fn(),
    onSelectAddressSpace: jest.fn(),
    onSelectAllAddressSpace: jest.fn()
  };

  const addressSpaces = {
    addressSpaces: [
      {
        metadata: {
          namespace: "app1_ns",
          name: "jupiter_as1",
          creationTimestamp: "2020-03-29T18:19:15.068Z"
        },
        spec: {
          type: "standard",
          plan: {
            metadata: {
              name: "standard-small"
            },
            spec: {
              displayName: "Small"
            }
          },
          authenticationService: {
            name: "none-authservice"
          }
        },
        status: {
          isReady: true,
          phase: "Active",
          messages: []
        }
      }
    ]
  };

  it("should render without crashing", () => {
    const div = document.createElement("div");
    ReactDom.render(
      <MockedProvider>
        <AddressSpaceListContainer {...props} />
      </MockedProvider>,
      div
    );
    ReactDom.unmountComponentAtNode(div);
  });

  it("should render loader if loading is true", () => {
    const { container } = setup([], props);
    expect(container).toHaveTextContent("Loading");
  });

  it("should not render loader if loading false", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_ALL_ADDRESS_SPACES(1, 10, [], [], "")
        },
        result: {
          data: {}
        }
      }
    ];

    const { container } = setup(mocks, props);
    //wait for response
    await wait(0);
    expect(container).not.toHaveTextContent("Loading");
  });

  it("should render <AddressSpaceList/> component if loading is false", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_ALL_ADDRESS_SPACES(1, 10, [], [], "")
        },
        result: {
          data: {}
        }
      }
    ];
    const { container } = setup(mocks, props);
    //wait for response
    await wait(0);
    //check table headers
    expect(container).toHaveTextContent("Name");
    expect(container).toHaveTextContent("Namespace");
  });

  it("should render <EmptyAddressSpace/> component if addressSpace total is not greater than zero (0)", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_ALL_ADDRESS_SPACES(1, 10, [], [], "")
        },
        result: {
          data: {
            addressSpaces: {
              total: 0
            }
          }
        }
      }
    ];
    const { container } = setup(mocks, props);
    await wait(0);
    expect(container).toHaveTextContent("Create an address space");
  });

  it("should not render <EmptyAddressSpace/>  component if addressSpace total is greater than zero (0)", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_ALL_ADDRESS_SPACES(1, 10, [], [], "")
        },
        result: {
          data: {
            addressSpaces: {
              total: 10,
              addressSpaces
            }
          }
        }
      }
    ];

    const { container } = setup(mocks, props);
    cleanup();
    await wait(0);
    expect(container).not.toHaveTextContent("Create an address space");
  });

  it("should render list of spaces", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_ALL_ADDRESS_SPACES(
            1,
            10,
            ["jupiter_as1"],
            ["app1_ns"],
            "standard"
          )
        },
        result: {
          data: {
            addressSpaces: {
              total: 10,
              addressSpaces
            }
          }
        }
      }
    ];

    const { findByText } = await setup(mocks, props);

    cleanup();
    await wait(0);

    expect(
      findByText(addressSpaces.addressSpaces[0].metadata.name)
    ).toBeDefined();
    expect(
      findByText(addressSpaces.addressSpaces[0].metadata.namespace)
    ).toBeDefined();
  });
});
