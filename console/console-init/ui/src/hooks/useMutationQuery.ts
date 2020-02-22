import { useEffect, useState } from "react";
import { useMutation } from "@apollo/react-hooks";
import { ApolloError, OperationVariables } from "apollo-client";
import { DocumentNode } from "graphql";

export const useMutationQuery = <TData = any, TVariables = OperationVariables>(
  query: DocumentNode,
  callbackOnError?: Function,
  callbackOnCompleted?: Function
) => {
  const [variables, setVariables] = useState<TVariables>();

  const [addVariables] = useMutation<TData>(query, {
    onError(error: ApolloError) {
      callbackOnError && callbackOnError(error);
    },
    onCompleted(data: any) {
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
