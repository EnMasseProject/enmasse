/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { useEffect, useState } from "react";
import { useMutation } from "@apollo/react-hooks";
import { ApolloError, OperationVariables } from "apollo-client";
import { DocumentNode } from "graphql";
import { useStoreContext, types } from "context-state-reducer";
import { ActionStatus } from "constant";
import { getTitleForSuccessQuery, getTitleForFailedQuery } from "utils";

export const useMutationQuery = <TData = any, TVariables = OperationVariables>(
  query: DocumentNode,
  refetchQueries?: string[],
  callbackOnError?: Function,
  callbackOnCompleted?: Function
) => {
  const [variables, setVariables] = useState<TVariables>();
  const { dispatch } = useStoreContext();

  const getQueryName = () => {
    const queryName =
      Array.isArray(query.definitions) &&
      query.definitions[0].selectionSet?.selections[0].name?.value;
    return queryName;
  };

  const [addVariables] = useMutation<TData>(query, {
    onError(errors: ApolloError) {
      const queryName = getQueryName();
      dispatch &&
        dispatch({
          type: types.SET_SERVER_ERROR,
          payload: {
            errors: [errors],
            status: ActionStatus.Failed,
            title: getTitleForFailedQuery(queryName)
          }
        });
      callbackOnError && callbackOnError(errors);
    },
    onCompleted(data: TData) {
      const queryName = getQueryName();
      dispatch &&
        dispatch({
          type: types.SET_SERVER_ERROR,
          payload: {
            errors: [],
            status: ActionStatus.Success,
            title: getTitleForSuccessQuery(queryName)
          }
        });
      callbackOnCompleted && callbackOnCompleted(data);
    },
    refetchQueries,
    awaitRefetchQueries: true,
    errorPolicy: "all"
  });

  useEffect(() => {
    async function executeQuery() {
      if (variables) {
        await addVariables({ variables });
      }
    }
    executeQuery();
  }, [variables, query, addVariables]);
  return [setVariables];
};
