/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Editor } from "components";
interface IJsonViewEditorProps {
  detailInJson?: any;
  readOnly: boolean;
}
const JsonViewEditor: React.FunctionComponent<IJsonViewEditorProps> = ({
  detailInJson,
  readOnly
}) => {
  return (
    <Editor
      mode="json"
      readOnly={readOnly}
      value={JSON.stringify(detailInJson, undefined, 2)}
    />
  );
};

export { JsonViewEditor };
