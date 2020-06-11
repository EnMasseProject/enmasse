/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import ReactDom from "react-dom";
import { render, cleanup, wait } from "@testing-library/react";
import { MockedProvider } from "@apollo/react-testing";
import { ConnectionLinksContainer } from "./ConnectionLinksContainer";
import { RETURN_CONNECTION_LINKS } from "graphql-module/queries";

const props = {
  name: "name",
  namespace: "namespace",
  connectionName: "connectionname",
  page: 1,
  perPage: 10,
  setTotalLinks: jest.fn(),
  filterNames: [],
  filterAddresses: [],
  filterRole: "",
  sortValue: undefined,
  setSortValue: jest.fn()
};

const connections = {
  connections: [
    {
      metadata: {
        name: "juno:62422",
        namespace: "app1_ns",
        creationTimestamp: "2020-04-03T06:54:35.179Z"
      },
      spec: {
        hostname: "juno:62422",
        containerId: "3a54a5a1-7579-11ea-8dfc-15c495820fd3",
        protocol: "amqps",
        encrypted: true,
        properties: []
      },
      metrics: [
        {
          name: "enmasse_messages_in",
          type: "gauge",
          value: 7,
          units: "msg/s"
        },
        {
          name: "enmasse_messages_out",
          type: "gauge",
          value: 7,
          units: "msg/s"
        },
        {
          name: "enmasse_senders",
          type: "gauge",
          value: 2,
          units: "total"
        },
        {
          name: "enmasse_receivers",
          type: "gauge",
          value: 4,
          units: "total"
        }
      ]
    }
  ]
};

afterEach(cleanup);

const setup = (mocks: any, props: any) => {
  return render(
    <MockedProvider mocks={mocks} addTypename={false}>
      <ConnectionLinksContainer {...props} />
    </MockedProvider>
  );
};

describe("<ConnectionLinksContainer/>", () => {
  it("should render without crashing", () => {
    const div = document.createElement("div");
    ReactDom.render(
      <MockedProvider>
        <ConnectionLinksContainer {...props} />
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
          query: RETURN_CONNECTION_LINKS(
            1,
            10,
            [],
            [],
            "test-name",
            "test-namespace",
            "test-connectionname",
            undefined,
            "test-role"
          )
        },
        result: {
          data: { connections: { connections } }
        }
      }
    ];

    const { container } = setup(mocks, props);
    //wait for response
    wait(() => expect(container).not.toHaveTextContent("Loading"));
  });

  it("should render <ConnectionLinksList/> component if loading is false", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_CONNECTION_LINKS(
            0,
            10,
            [],
            [],
            "jupiter_as1",
            "app1_ns",
            "juno:62422",
            undefined,
            undefined
          )
        },
        result: {
          data: { connections: { connections } }
        }
      }
    ];

    const { container, getByText } = setup(mocks, props);
    //wait for response
    //check table headers
    await wait(() => expect(container).toHaveTextContent("Role"));
    expect(container).toHaveTextContent("Name");
    expect(container).toHaveTextContent("Address");
  });

  it("should render <EmptyConnectionLinks/> if total links is not greater than zero (0)", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_CONNECTION_LINKS(
            0,
            10,
            [],
            [],
            "jupiter_as1",
            "app1_ns",
            "juno:62422",
            undefined,
            undefined
          )
        },
        result: {
          data: { total: 0, connections: { connections } }
        }
      }
    ];

    const { container } = setup(mocks, props);
    await wait(() =>
      expect(container).toHaveTextContent("You currently don't have any links")
    );
  });

  it("should not render <EmptyConnectionLinks/> if total links is greater than zero (0)", async () => {
    const mocks = [
      {
        request: {
          query: RETURN_CONNECTION_LINKS(
            0,
            10,
            [],
            [],
            "jupiter_as1",
            "app1_ns",
            "juno:62422",
            undefined,
            undefined
          )
        },
        result: {
          data: { total: 1, connections: { connections } }
        }
      }
    ];

    const { container } = setup(mocks, props);
    cleanup();
    await wait(() =>
      expect(container).not.toHaveTextContent(
        "You currently don't have any links"
      )
    );
  });
});
