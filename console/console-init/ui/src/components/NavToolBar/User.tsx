import * as React from "react";
import { useQuery } from "@apollo/react-hooks";
import { IUserDetail } from "types/ResponseTypes";
import { RETURN_WHOAMI } from "queries";
import { FetchPolicy, UNKNOWN, POLL_INTERVAL_USER } from "constants/constants";

export const UserDetail: React.FunctionComponent = () => {
  const { data, loading, error } = useQuery<IUserDetail>(RETURN_WHOAMI, {
    pollInterval: POLL_INTERVAL_USER,
    fetchPolicy: FetchPolicy.NETWORK_ONLY
  });
  let userName = UNKNOWN;
  if (
    data &&
    data.whoami &&
    (data.whoami.fullName || data.whoami.metadata.name)
  ) {
    if (data.whoami.fullName) {
      userName = data.whoami.fullName;
    } else {
      userName = data.whoami.metadata.name;
    }
  }
  return <>{userName}</>;
};
