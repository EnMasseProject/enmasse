import React, { useState } from "react";
import AceEditor, { IAceEditorProps } from "react-ace";

const Editor: React.FunctionComponent<IAceEditorProps> = ({
  value,
  mode,
  name,
  onChange,
  enableBasicAutocompletion,
  enableLiveAutocompletion
}) => {
  return (
    <>
      <AceEditor
        mode={mode}
        theme="github"
        fontSize={14}
        onChange={onChange}
        value={value}
        name={name}
        enableBasicAutocompletion={enableBasicAutocompletion}
        enableLiveAutocompletion={enableLiveAutocompletion}
        style={{
          width: 700,
          border: "1px solid",
          borderColor: "lightgrey"
        }}
      />
    </>
  );
};

export { Editor };
