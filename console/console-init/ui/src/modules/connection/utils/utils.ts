/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { IConnection } from "modules/connection";

const getFilteredConnectionNames = (connections: IConnection[]) => {
  return connections && connections.map(connection => connection.hostname);
};

const getHeaderTextForCloseAll = (connections: IConnection[]) => {
  return connections && connections.length > 1
    ? "Close these Connections ?"
    : "Close this Connection ?";
};
const getDetailTextForCloseAll = (connections: IConnection[]) => {
  let detailText = "";
  if (connections && connections.length > 1) {
    detailText = `Are you sure you want to close all of these connections: ${connections.map(
      connection => " " + connection.hostname
    )} ?`;
  } else {
    detailText = `Are you sure you want to close connection: ${connections &&
      connections[0].hostname} ?`;
  }
  return detailText;
};

export {
  getFilteredConnectionNames,
  getHeaderTextForCloseAll,
  getDetailTextForCloseAll
};
