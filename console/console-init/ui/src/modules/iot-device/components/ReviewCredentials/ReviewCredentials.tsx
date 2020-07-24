/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { ICredential } from "modules/iot-device";

interface IReviewCredentialsProps {
  credentials: ICredential[];
}
interface IReviewCredentialProps {
  credential: ICredential;
}

const ReviewCredential: React.FunctionComponent<IReviewCredentialProps> = ({
  credential
}) => {
  // const {secrets,enabled,ext,type,"auth-id"} = credential
  return <></>;
};

const ReviewCredentials: React.FunctionComponent<IReviewCredentialsProps> = ({
  credentials
}) => {
  return (
    <>
      {credentials.map(credential => (
        <ReviewCredential credential={credential} />
      ))}
    </>
  );
};

export { ReviewCredentials };
