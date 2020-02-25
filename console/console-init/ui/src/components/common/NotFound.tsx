/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { NavLink, Link } from "react-router-dom";
import { Alert, PageSection } from "@patternfly/react-core";

const NotFound: React.FunctionComponent = () => (
  <PageSection>
    <Alert variant="danger" title="Connection Error">
      You have been disconnected from the server. Please login again.
    </Alert>
    <br />
    <a href="oauth/sign_in" className="pf-c-nav__link">
      Take me home
    </a>
  </PageSection>
);

export { NotFound };
