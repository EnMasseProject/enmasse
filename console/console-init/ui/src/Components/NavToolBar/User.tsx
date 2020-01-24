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
    (data.whoami.FullName || data.whoami.ObjectMeta.Name)
  ) {
    if (data.whoami.FullName) {
      userName = data.whoami.FullName;
    } else {
      userName = data.whoami.ObjectMeta.Name;
    }
  }
  return <>{userName}</>;
};
