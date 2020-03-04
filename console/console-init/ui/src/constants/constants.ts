/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

const UNKNOWN: string = "unknown";
const POLL_INTERVAL: number = 5000;
const POLL_INTERVAL_USER: number = 20000;
const QUERY: string = "query";
const MAX_ITEM_TO_FETCH_IN_TYPEAHEAD_DROPDOWN = 100;
const NUMBER_OF_RECORDS_TO_DISPLAY_IF_SERVER_HAS_MORE_DATA = 10;
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

export {
  UNKNOWN,
  POLL_INTERVAL,
  POLL_INTERVAL_USER,
  FetchPolicy,
  QUERY,
  TYPEAHEAD_REQUIRED_LENGTH,
  MAX_ITEM_TO_FETCH_IN_TYPEAHEAD_DROPDOWN as MAX_ITEM_TO_DISPLAY_IN_TYPEAHEAD_DROPDOWN,
  NUMBER_OF_RECORDS_TO_DISPLAY_IF_SERVER_HAS_MORE_DATA,
  TypeAheadMessage,
  ErrorCodes
};
