import React, { useState } from "react";
import { Editor } from "components";
import { Switch } from "@patternfly/react-core";
interface IFormAndJsonProps {
  detail?: string;
  detailInJson?: any;
  setDetail: (value: string) => void;
  setDetailInJson: (value: any) => void;
}
const FormAndJson: React.FunctionComponent<IFormAndJsonProps> = ({
  detail,
  detailInJson,
  setDetail,
  setDetailInJson
}) => {
  const [isValid, setIsValid] = useState<boolean>(true);
  const [showJson, setShowJson] = useState<boolean>(true);
  //   const setDetail1 = () => {
  //     const obj = {
  //       persons: [
  //         { name: "John", age: 30, city: "New York" },
  //         { name: "John", age: 30, city: "New York" },
  //         { name: "John", age: 30, city: "New York" }
  //       ]
  //     };
  //     setDetail(JSON.stringify(obj, undefined, 2));
  //     setDetailJson(obj);
  //   };
  //   if (!detail) {
  //     setDetail1();
  //   }
  const onToggle = () => {
    if (showJson) {
      try {
        if (detail) {
          const obj = JSON.parse(detail);
          setDetail(JSON.stringify(obj, undefined, 2));
          setDetailInJson(obj);
          setShowJson(false);
          setIsValid(true);
        }
      } catch {
        setIsValid(false);
      }
    } else {
      setShowJson(!showJson);
    }
  };
  const onChange = (value: string) => {
    setDetail(value);
  };
  return (
    <>
      {!isValid && <h1 style={{ color: "red" }}>inValid</h1>}
      <Switch id="iot-enable-switch" isChecked={showJson} onChange={onToggle} />
      {showJson ? (
        <>
          <Editor
            mode="json"
            value={detail}
            onChange={onChange}
            enableBasicAutocompletion={true}
            enableLiveAutocompletion={true}
          />
        </>
      ) : (
        <>{console.log(detailInJson)}</>
      )}
    </>
  );
};

export { FormAndJson };
