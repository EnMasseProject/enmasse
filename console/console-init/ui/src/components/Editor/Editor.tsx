import React, { useState } from "react";
import AceEditor, { IAceEditorProps } from "react-ace";

const Editor: React.FunctionComponent<IAceEditorProps> = ({
  value,
  readOnly,
  mode,
  style,
  name,
  onChange,
  enableBasicAutocompletion,
  enableLiveAutocompletion
}) => {
  return (
    <AceEditor
      mode={mode}
      theme="github"
      fontSize={14}
      readOnly={readOnly}
      onChange={onChange}
      value={value}
      name={name}
      enableBasicAutocompletion={enableBasicAutocompletion}
      enableLiveAutocompletion={enableLiveAutocompletion}
      style={style}
    />
  );
};

export { Editor };
