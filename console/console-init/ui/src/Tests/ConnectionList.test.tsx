import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import {
  IConnection,
  ConnectionList
} from "../Components/AddressSpace/Connection/ConnectionList";

describe("Connection List", () => {
  test("it renders a list of connections", () => {
    const connections: IConnection[] = [
      {
        hostname: "foo1",
        containerId: "123",
        protocol: "AMQP",
        messagesIn: 1,
        messagesOut: 2,
        senders: 3,
        receivers: 4,
        status: "running",
        encrypted:true
      },
      {
        hostname: "foo2",
        containerId: "1234",
        protocol: "AMQ",
        messagesIn: 12,
        messagesOut: 5,
        senders: 6,
        receivers: 7,
        status: "running",
        encrypted:true
      }
    ];

    const { getByText } = render(
      <MemoryRouter>
        <ConnectionList rows={connections} />
      </MemoryRouter>
    );

    //Testing elements of first row
    const hostnameNodeOne = getByText(connections[0].hostname);
    const containerIdOne = getByText(connections[0].containerId);
    const protocolNodeOne = getByText(connections[0].protocol);
    const messagesInNodeOne = getByText(connections[0].messagesIn.toString());

    expect(hostnameNodeOne).toBeDefined();
    expect(containerIdOne).toBeDefined();
    expect(protocolNodeOne).toBeDefined();
    expect(messagesInNodeOne).toBeDefined();

    //Testing elements of second row
    const hostnameNodeTwo = getByText(connections[1].hostname);
    const containerIdTwo = getByText(connections[1].containerId);
    const protocolNodeTwo = getByText(connections[1].protocol);
    const messagesInNodeTwo = getByText(connections[1].messagesIn.toString());

    expect(hostnameNodeTwo).toBeDefined();
    expect(containerIdTwo).toBeDefined();
    expect(protocolNodeTwo).toBeDefined();
    expect(messagesInNodeTwo).toBeDefined();
  });
});
