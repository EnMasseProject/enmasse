/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React, { useState } from "react";
import { FileUpload } from "@patternfly/react-core";

interface IUploadFileProps {
  id?: string;
  value?: string;
  setValue: (value: string) => void;
  isRejected: boolean;
  setIsRejected: (value: boolean) => void;
  fileRestriction?: string;
}

const UploadFile: React.FunctionComponent<IUploadFileProps> = ({
  id = "file-upload",
  value,
  setValue,
  isRejected,
  setIsRejected,
  fileRestriction = ""
}) => {
  const [filename, setFilename] = useState<string>();
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const handleFileReadStarted = (fileHandle: File) => setIsLoading(true);
  const handleFileReadFinished = (fileHandle: File) => setIsLoading(false);
  const handleFileChange = (
    value: string | File,
    filename: string,
    event: any
  ) => {
    setFilename(filename);
    setValue(value.toString());
    if (isRejected) {
      setIsRejected(false);
    }
  };
  const handleFileRejected = (rejectedFiles: File[], event: any) => {
    setIsRejected(true);
  };

  return (
    <>
      <FileUpload
        id={id}
        type="text"
        value={value}
        filename={filename}
        onChange={handleFileChange}
        onReadStarted={handleFileReadStarted}
        onReadFinished={handleFileReadFinished}
        isLoading={isLoading}
        dropzoneProps={{
          accept: fileRestriction,
          onDropRejected: handleFileRejected
        }}
        validated={isRejected ? "error" : "default"}
      />
    </>
  );
};

export { UploadFile };
