import { useState, useEffect } from "react";
import {
  useQuery as graphqlQuery,
  QueryHookOptions
} from "@apollo/react-hooks";
import { DocumentNode } from "graphql";
import { ApolloError, OperationVariables } from "apollo-boost";
import { QueryResult } from "@apollo/react-common";

export const useQuery = <TData = any, TVariables = OperationVariables>(
  query: DocumentNode,
  options?: QueryHookOptions,
  callbackOnError?: Function,
  callbackOnComplete?: Function
): QueryResult<TData, TVariables> => {
  const [graphqlState, setGraphqlState] = useState<any>({
    loading: true,
    data: null,
    error: null
  });

  const { data, error, loading } = graphqlQuery<TData>(query, {
    ...options,
    onError(error: ApolloError) {
      callbackOnError && callbackOnError(error);
    },
    onCompleted(data: TData) {
      callbackOnComplete && callbackOnComplete(data);
    }
  });

  useEffect(() => {
    setGraphqlState({ data, error, loading });
  }, [query, data, error, loading]);

  return graphqlState;
};
