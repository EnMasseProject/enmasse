/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useParams } from "react-router";
import { useQuery } from "@apollo/react-hooks";
import { RETURN_IOT_CREDENTIALS } from "graphql-module/queries";
import { ICredentialsReponse } from "schema";
import { ConfigurationInfo } from "modules/iot-device-detail/components";
import {
  getCredentialFilterType,
  getCredentialFilterValue
} from "modules/iot-device-detail/utils";

export const ConfigurationInfoContainer: React.FC<{ id: string }> = ({
  id
}) => {
  const { projectname, deviceid, namespace } = useParams();
  const [filterType, setFilterType] = useState<string>("enabled");
  const [filterValue, setFilterValue] = useState<string>("");

  const propertyName = getCredentialFilterType(filterType, filterValue);
  const value = getCredentialFilterValue(filterType, filterValue);

  const { data } = useQuery<ICredentialsReponse>(
    RETURN_IOT_CREDENTIALS(
      projectname,
      namespace,
      deviceid,
      propertyName,
      filterType,
      value
    )
  );

  const { credentials } = data?.credentials || {};
  const credentialsJson = credentials && JSON.parse(credentials);

  return (
    <ConfigurationInfo
      id={id}
      credentials={credentialsJson}
      setFilterType={setFilterType}
      setFilterValue={setFilterValue}
      filterType={filterType}
      filterValue={filterValue}
    />
  );
};
