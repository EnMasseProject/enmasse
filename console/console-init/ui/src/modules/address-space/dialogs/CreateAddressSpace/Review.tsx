/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { Loading } from "use-patternfly";
import { useQuery } from "@apollo/react-hooks";
import "ace-builds/src-noconflict/mode-java";
import "ace-builds/src-noconflict/theme-github";
import { ADDRESS_SPACE_COMMAND_REVIEW_DETAIL } from "graphql-module/queries";
import { AddressSpaceReview } from "modules/address-space/components";

export interface IReviewProps {
  name?: string;
  type?: string;
  plan?: string;
  namespace: string;
  authenticationService: string;
}

export const Review: React.FunctionComponent<IReviewProps> = ({
  name,
  type,
  plan,
  namespace,
  authenticationService
}) => {
  const { data, loading } = useQuery(ADDRESS_SPACE_COMMAND_REVIEW_DETAIL, {
    variables: {
      as: {
        metadata: {
          name: name,
          namespace: namespace
        },
        spec: {
          plan: plan ? plan.toLowerCase() : "",
          type: type ? type.toLowerCase() : "",
          authenticationService: {
            name: authenticationService
          }
        }
      }
    }
  });

  if (loading) return <Loading />;

  return (
    <AddressSpaceReview
      name={name}
      plan={plan}
      type={type}
      namespace={namespace}
      authenticationService={authenticationService}
      data={data}
    />
  );
};
