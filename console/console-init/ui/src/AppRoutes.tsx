import * as React from "react";
import { SwitchWith404, LazyRoute } from "use-patternfly";
import { Redirect } from "react-router";

const getAddressSpaceListPage = () => import("./Pages/AddressSpaceListPage");
const getAddressSpaceDetail = () => import("./Pages/AddressSpaceDetailPage");
const getAddressDetail = () => import("./Pages/AddressDetailPage");
const getConnectionDetail = () => import("./Pages/ConnectionDetailPage");

export const AppRoutes = () => (
  <SwitchWith404>
    <Redirect path="/" to="/address_spaces" exact={true} />
    <LazyRoute
      path="/address_spaces"
      exact={true}
      getComponent={getAddressSpaceListPage}
    />
    <LazyRoute
      path="/address_space/name=:name&namespace=:namespace/:subList"
      exact={true}
      getComponent={getAddressSpaceDetail}
    />
    <LazyRoute
      path="/address_space/name=:name&namespace=:namespace/address/name=:addressname&namespace=:addressnamespace"
      getComponent={getAddressDetail}
      exact={true}
    />
    <LazyRoute
      path="/address_space/name=:name&namespace=:namespace/connection/name=:conectionname&namespace=:connectionnamespace"
      getComponent={getConnectionDetail}
      exact={true}
    />
  </SwitchWith404>
);
