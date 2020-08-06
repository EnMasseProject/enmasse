/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

const UNKNOWN: string = "unknown";
const POLL_INTERVAL: number = 5000;
const POLL_INTERVAL_USER: number = 20000;
const QUERY: string = "query";
const MAX_ITEM_TO_DISPLAY_IN_TYPEAHEAD_DROPDOWN = 10;
const SERVER_DATA_THRESHOLD = 10;
const TYPEAHEAD_REQUIRED_LENGTH: number = 5;
enum FetchPolicy {
  NETWORK_ONLY = "network-only",
  CACHE_FIRST = "cache-first",
  CACHE_ONLY = "cache-only",
  NO_CACHE = "no-cache",
  STAND_BY = "standby",
  CACHE_AND_NETWORK = "cache-and-network"
}

enum TypeAheadMessage {
  NO_RESULT_FOUND = "No Results Found",
  MORE_CHAR_REQUIRED = "Enter more characters"
}
enum ErrorCodes {
  FORBIDDEN = 403
}

enum AddressTypes {
  QUEUE = "queue",
  ANYCAST = "anycast",
  MULTICAST = "multicast",
  TOPIC = "topic",
  SUBSCRIPTION = "subscription"
}

enum TemplateType {
  DIRECTLY_CONNECTED = "directly-connected",
  VIA_GATEWAY = "via-gateway"
}

enum DataType {
  ARRAY = "array",
  OBJECT = "object"
}

enum Protocols {
  HTTP = "http",
  HTTPS = "https",
  MQTT = "mqtt",
  AMQP = "amqp",
  AMQPS = "amqps",
  COAP = "coap"
}

enum DialogTypes {
  ENABLE = "Enable",
  DISABLE = "Disable",
  DELETE = "Delete"
}

enum CredentialsType {
  PASSWORD = "hashed-password",
  PSK = "psk",
  X509_CERTIFICATE = "x509-cert"
}
const WIZARD_HEIGHT = 400;

enum DeviceConnectionType {
  VIA_GATEWAYS = "Via gateways",
  CONNECTED_DIRECTLY = "Connected directly",
  NA = "N/A"
}

enum OperationType {
  ADD = "add",
  EDIT = "edit"
}

enum Messages {
  InvalidJson = "Invalid json, please try again with valid json"
}

export {
  UNKNOWN,
  POLL_INTERVAL,
  POLL_INTERVAL_USER,
  FetchPolicy,
  QUERY,
  TYPEAHEAD_REQUIRED_LENGTH,
  MAX_ITEM_TO_DISPLAY_IN_TYPEAHEAD_DROPDOWN,
  SERVER_DATA_THRESHOLD,
  TypeAheadMessage,
  ErrorCodes,
  AddressTypes,
  TemplateType,
  DataType,
  Protocols,
  DialogTypes,
  CredentialsType,
  WIZARD_HEIGHT,
  DeviceConnectionType,
  OperationType,
  Messages
};
