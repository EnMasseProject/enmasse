/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { SwitchProps, Switch, Route, useRouteMatch } from "react-router-dom";
import { NotFound } from "./NotFound";

export const SwitchWith404: React.FunctionComponent<SwitchProps> = ({
  children,
  ...props
}) => {
  const match = useRouteMatch();
  const defaultMatch = React.useMemo(
    () => match && <Route path={match.path} exact={true} />,
    [match]
  );
  return (
    <Switch {...props}>
      {children}
      {/*
       * Default route that matches the parent route, to avoid showing a 404
       * for "junction" pages . See the "Dashboard" example.
       */}
      {defaultMatch}
      <Route>
        <NotFound />
      </Route>
    </Switch>
  );
};
