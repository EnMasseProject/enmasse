/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useParams } from "react-router";
import { useQuery } from "@apollo/react-hooks";
import { RETURN_IOT_CREDENTIALS } from "graphql-module/queries";
import { ICredentialsReponse } from "schema";
import {
  ConfigurationInfo,
  IConfigurationInfoProps
} from "modules/iot-device-detail/components";
import { mock_adapters } from "mock-data";
import {
  getCredentialFilterType,
  getCredentialFilterValue
} from "modules/iot-device-detail/utils";

export const ConfigurationInfoContainer: React.FC<Pick<
  IConfigurationInfoProps,
  "id"
>> = ({ id }) => {
  const { projectname, deviceid } = useParams();
  const [filterType, setFilterType] = useState<string>("");
  const [filterValue, setFilterValue] = useState<string>("");

  const { data } = useQuery<ICredentialsReponse>(
    RETURN_IOT_CREDENTIALS(
      projectname,
      deviceid,
      getCredentialFilterType(filterType, filterValue),
      getCredentialFilterValue(filterType, filterValue)
    )
  );

  /**
   * TODO: add adapters api query and remove mock adapter data
   */

  const { credentials } = data?.credentials || {};
  const credentialsJson = credentials && JSON.parse(credentials);

  return (
    <ConfigurationInfo
      id={id}
      adapters={mock_adapters}
      credentials={credentialsJson}
      onSelectFilterType={setFilterType}
      onSelectFilterValue={setFilterValue}
    />
  );
};
