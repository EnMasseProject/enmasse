import * as React from "react";
import { useQuery } from "@apollo/react-hooks";
import { IUserDetail } from "src/Types/ResponseTypes";
import { RETURN_WHOAMI } from "src/Queries/Queries";

export const UserDetail: React.FunctionComponent = () => {
  const { data, loading, error } = useQuery<IUserDetail>(RETURN_WHOAMI, {
    pollInterval: 20000,
    fetchPolicy: "network-only"
  });
  let userName = "unknown";
  if (
    data &&
    data.whoami &&
    (data.whoami.fullName || data.whoami.objectMeta.name)
  ) {
    if (data.whoami.fullName) {
      userName = data.whoami.fullName;
    } else {
      userName = data.whoami.objectMeta.name;
    }
  }
  return <>{userName}</>;
};
