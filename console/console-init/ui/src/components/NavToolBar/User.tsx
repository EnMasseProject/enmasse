import * as React from "react";
import { useQuery } from "@apollo/react-hooks";
import { IUserDetail } from "types/ResponseTypes";
import { RETURN_WHOAMI } from "queries";
import { FetchPolicy, UNKNOWN } from "constants/constants";

export const UserDetail: React.FunctionComponent = () => {
  const { data } = useQuery<IUserDetail>(RETURN_WHOAMI, {
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
