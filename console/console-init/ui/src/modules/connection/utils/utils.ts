/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { IConnection } from "modules/connection";

const getFilteredConnectionNames = (connections: IConnection[]) => {
  return connections && connections.map(connection => connection.name);
};

const getHeaderTextForCloseAll = (connections: IConnection[]) => {
  return connections && connections.length > 1
    ? "Close these Connections ?"
    : "Close this Connection ?";
};
const getDetailTextForCloseAll = (connections: IConnection[]) => {
  let detail = "";
  if (connections && connections.length > 1) {
    detail = `Are you sure you want to close all of these connections: ${connections.map(
      connection => " " + connection.hostname
    )} ?`;
  } else {
    detail = `Are you sure you want to close connection: ${connections &&
      connections[0].hostname} ?`;
  }
  return detail;
};

export {
  getFilteredConnectionNames,
  getHeaderTextForCloseAll,
  getDetailTextForCloseAll
};
