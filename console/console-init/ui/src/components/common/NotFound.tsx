/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { NavLink } from "react-router-dom";
import { Alert, PageSection } from "@patternfly/react-core";

const NotFound: React.FunctionComponent = () => (
  <PageSection>
    <Alert variant="danger" title="Something went wrong!" />
    <br />
    <NavLink to="oauth/sign_in" className="pf-c-nav__link">
      Take me home
    </NavLink>
  </PageSection>
);

export { NotFound };
