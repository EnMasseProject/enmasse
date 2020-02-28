/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { NavLink } from "react-router-dom";
import { Alert, PageSection } from "@patternfly/react-core";

interface INotFoundProps {
  updateState: (error: boolean) => void;
}

const NotFound: React.FunctionComponent<INotFoundProps> = ({ updateState }) => (
  <PageSection>
    <Alert variant="danger" title="Unexpected Error">
      Something went wrong. Please try again!
    </Alert>
    <br />
    <NavLink
      to="/"
      className="pf-c-nav__link"
      onClick={() => updateState(false)}
    >
      Sign in
    </NavLink>
  </PageSection>
);

export { NotFound };
