/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";

const RETURN_NAMESPACES = gql`
  query all_namespaces {
    namespaces {
      ObjectMeta {
        Name
      }
      Status {
        Phase
      }
    }
  }
`;

export { RETURN_NAMESPACES };
