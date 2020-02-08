/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

const UNKNOWN:string = "unknown";
const POLL_INTERVAL:number=5000;
const POLL_INTERVAL_USER:number=20000;
enum FetchPolicy{
    NETWORK_ONLY="network-only",
    CACHE_FIRST="cache-first",
    CACHE_ONLY="cache-only",
    NO_CACHE="no-cache",
    STAND_BY="standby",
    CACHE_AND_NETWORK="cache-and-network"
};

export { 
    UNKNOWN,
    POLL_INTERVAL,
    POLL_INTERVAL_USER,
    FetchPolicy 
};
