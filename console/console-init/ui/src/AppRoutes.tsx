import * as React from 'react';
import { SwitchWith404, LazyRoute } from 'use-patternfly';
import {
  LastLocationProvider,
  useLastLocation,
} from 'react-router-last-location';
import { Redirect } from 'react-router';
import IndexPage from 'src/Pages/IndexPage';
let routeFocusTimer: number;

const getAddressSpaceListPage = () => import('./Pages/AddressSpaceListPage');
const getAddressSpaceDetail = () => import('./Pages/AddressSpaceDetailPage');
const getAddressDetail = () => import('./Pages/AddressDetailPage');
const getConnectionDetail = () => import('./Pages/ConnectionDetailPage');
const getConnectionsList = () => import('./Pages/ConnectionsListPage');
export const AppRoutes = () => (
  <LastLocationProvider>
    <SwitchWith404>
      <Redirect path="/" to="/address-spaces" exact={true} />
      {/* <LazyRoute path="/" getComponent={IndexPage}/> */}
      <LazyRoute
        path="/address-spaces"
        exact={true}
        getComponent={getAddressSpaceListPage}
      />
      {/* <Redirect path="/adress-space/:id" to="/main/items" exact={true}/> */}
      <LazyRoute
        path="/address-space/:id/addresses"
        exact={true}
        getComponent={getAddressSpaceDetail}
      />
      <LazyRoute
        path="/address-space/:id/address/:id"
        getComponent={getAddressDetail}
      />
      <LazyRoute
        path="/address-space/:id/connection/:id"
        getComponent={getAddressDetail}
      />
    </SwitchWith404>
  </LastLocationProvider>
);
