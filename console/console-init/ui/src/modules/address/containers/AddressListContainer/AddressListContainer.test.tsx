/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import ReactDom from "react-dom";
import { render, cleanup } from "@testing-library/react";
import { MockedProvider } from "@apollo/react-testing";
import wait from "waait";
import { AddressListContainer } from "./AddressListContainer";
import { RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE } from "graphql-module/queries";

const props = {
  name: "name",
  namespace: "namespace",
  filterNames: [],
  typeValue: "",
  statusValue: "",
  setTotalAddress: jest.fn(),
  page: 1,
  perPage: 10,
  addressSpacePlan: "",
  sortValue: undefined,
  setSortValue: jest.fn(),
  isWizardOpen: false,
  setIsWizardOpen: jest.fn(),
  selectedAddresses: [],
  onSelectAddress: jest.fn(),
  onSelectAllAddress: jest.fn()
};

const addresses = {
  addresses: [
    {
      metadata: {
        namespace: "app1_ns",
        name: "jupiter_as1",
        creationTimestamp: "2020-04-02T21:00:56.510Z"
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

afterEach(cleanup);

const setup = (mocks: any, props: any) => {
  return render(
    <MockedProvider mocks={mocks} addTypename={false}>
      <AddressListContainer {...props} />
    </MockedProvider>
  );
};

describe("<AddressListContainer/>", () => {
  it("should render without crashing", () => {
    const div = document.createElement("div");
    ReactDom.render(
      <MockedProvider>
        <AddressListContainer {...props} />
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
          query: RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE(
            1,
            10,
            "test-name",
            "test-namespace",
            [],
            undefined,
            "",
            undefined
          )
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

  it("should render <AddressList/> component if loading is false", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE(
            1,
            10,
            "test-name",
            "test-namespace",
            [],
            undefined,
            "",
            undefined
          )
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
    expect(container).toHaveTextContent("Address");
    expect(container).toHaveTextContent("Status");
  });

  it("should render <EmptyAddress/> if total links is not greater than zero (0)", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE(
            1,
            10,
            "test-name",
            "test-namespace",
            [],
            undefined,
            "",
            undefined
          )
        },
        result: {
          data: {}
        }
      }
    ];

    const { container } = setup(mocks, props);
    await wait(0);
    expect(container).toHaveTextContent("Create an address");
  });

  it("should not render <EmptyAddress/> component if total links is greater than zero (0)", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE(
            1,
            10,
            "test-name",
            "test-namespace",
            [],
            undefined,
            "",
            undefined
          )
        },
        result: {
          data: { total: 1, addresses: { addresses } }
        }
      }
    ];

    const { container } = setup(mocks, props);
    cleanup();
    await wait(0);
    expect(container).not.toHaveTextContent("Create an address");
  });
});
