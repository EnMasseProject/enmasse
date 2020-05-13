/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";

interface IAddressErrorMessageProps {
  messages: Array<string>;
}

export const AddressErrorMessage: React.FunctionComponent<IAddressErrorMessageProps> = ({
  messages
}) => {
  return (
    <>
      {messages &&
        messages.map(message => (
          <>
            {message}
            <br />
          </>
        ))}
    </>
  );
};
