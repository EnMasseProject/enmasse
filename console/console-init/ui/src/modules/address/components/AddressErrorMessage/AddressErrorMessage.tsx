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
