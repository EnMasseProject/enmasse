import { useState, useEffect } from "react";
import { useQuery, QueryHookOptions } from "@apollo/react-hooks";
import { DocumentNode } from "graphql";
import { ApolloError, OperationVariables } from "apollo-boost";
import { QueryResult } from "@apollo/react-common";

export const useClientQuery = <TData = any, TVariables = OperationVariables>(
  query: DocumentNode,
  options?: QueryHookOptions
): QueryResult<TData, TVariables> => {
  const [graphqlState, setGraphqlState] = useState<any>({
    loading: true,
    data: null,
    error: null
  });

  const { data, error, loading } = useQuery<TData>(query, {
    ...options,
    onError(error: ApolloError) {
      console.log("error", error);
    },
    onCompleted(data) {
      console.log(data);
    }
  });

  useEffect(() => {
    setGraphqlState({ data, error, loading });
  }, [query, data, error, loading]);

  return graphqlState;
};
