/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { useEffect, useState } from "react";
import { useMutation } from "@apollo/react-hooks";
import { ApolloError, OperationVariables } from "apollo-client";
import { DocumentNode } from "graphql";
import { useErrorContext, types } from "context-state-reducer";

export const useMutationQuery = <TData = any, TVariables = OperationVariables>(
  query: DocumentNode,
  callbackOnError?: Function,
  callbackOnCompleted?: Function
) => {
  const [variables, setVariables] = useState<TVariables>();
  const { dispatch } = useErrorContext();

  const [addVariables] = useMutation<TData>(query, {
    onError(errors: ApolloError) {
      dispatch &&
        dispatch({
          type: types.SET_SERVER_ERROR,
          payload: { errors: [errors] }
        });
      callbackOnError && callbackOnError(errors);
    },
    onCompleted(data: TData) {
      callbackOnCompleted && callbackOnCompleted(data);
    }
  });

  useEffect(() => {
    async function executeQuery() {
      if (variables) {
        await addVariables({ variables });
      }
    }
    executeQuery();
  }, [variables, query]);
  return [setVariables];
};
