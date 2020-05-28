/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import { PageSection } from "@patternfly/react-core";
import {
  CredentialList,
  ICredential,
  IExtension,
  ISecret
} from "modules/iot-device/components";
import { uniqueId, findIndexByProperty } from "utils";

export const AddCredential: React.FC<{}> = () => {
  const getExtensionsFieldsInitialState = () => {
    const initialState: IExtension = {
      id: uniqueId(),
      parameter: "",
      type: "",
      value: ""
    };
    return initialState;
  };

  const getCredentialsFieldsInitialState = () => {
    const initialState: ICredential = {
      id: uniqueId(),
      "auth-id": "",
      type: "hashed_password",
      secrets: [{ "pwd-hash": "" }],
      ext: [getExtensionsFieldsInitialState()],
      enabled: true,
      isExpandedAdvancedSetting: false
    };
    return initialState;
  };

  const [credentials, setCredentials] = useState([
    getCredentialsFieldsInitialState()
  ]);
  const [type, setType] = useState<string>("hashed_password");
  const [activeCredentialFormId, setActiveCredentialFormId] = useState();

  const getSecretsFieldsInitialState = () => {
    let initialState: ISecret = {};
    switch (type && type.toLowerCase()) {
      case "hashed_password":
        initialState = {
          "pwd-hash": "",
          "not-before": "",
          "not-after": "",
          comment: ""
        };
        break;
      case "x509":
        initialState = { "not-before": "", "not-after": "", comment: "" };
        break;
      case "psk":
        initialState = {
          key: "",
          "not-before": "",
          "not-after": "",
          comment: ""
        };
        break;
      default:
        break;
    }
    return initialState;
  };

  const getFormInitialStateByProperty = (property: string) => {
    let initialState = {};
    if (property === "secrets") {
      initialState = getSecretsFieldsInitialState();
    } else if (property === "ext") {
      initialState = getExtensionsFieldsInitialState();
    } else if (property === "credentials") {
      initialState = getCredentialsFieldsInitialState();
    }
    return initialState;
  };

  const setSecretsInitialFormState = () => {
    const newCredentials = [...credentials];
    const activeFormId =
      activeCredentialFormId || newCredentials[newCredentials.length - 1]?.id;
    const index = findIndexByProperty(credentials, "id", activeFormId);
    if (index >= 0) {
      const initialState = getFormInitialStateByProperty("secrets");
      newCredentials[index]["secrets"] = [{ id: uniqueId(), ...initialState }];
      setCredentials(newCredentials);
    }
  };

  useEffect(() => {
    setSecretsInitialFormState();
  }, [type]);

  const onSelectType = (id: string, event: any, value: string) => {
    setType(value);
    setActiveCredentialFormId(id);
    handleInputChange(id, event, value);
  };

  const addMoreItem = (id?: string, property?: string) => {
    let newCredentials: ICredential[];
    /**
     * add more items for child fields i.e. secrets and extensions
     */
    if (property && id) {
      newCredentials = [...credentials];
      const index: number = findIndexByProperty(credentials, "id", id);
      if (index >= 0) {
        const initialState = getFormInitialStateByProperty(property);
        const secrets = (newCredentials[index] as any)[property];
        (newCredentials[index] as any)[property] = [
          ...secrets,
          { id: uniqueId(), ...initialState }
        ];
        setCredentials(newCredentials);
      }
    } else {
      /**
       * add more credentials
       */
      newCredentials = [...credentials, getCredentialsFieldsInitialState()];
    }
    setCredentials(newCredentials);
  };

  const onDeleteItem = (id: string, property?: string, childObjId?: string) => {
    let newCredentials: ICredential[] = [...credentials];
    const index: number = findIndexByProperty(credentials, "id", id);
    /**
     * delete items for child fields i.e. secrets and extensions
     */
    if (id && property && childObjId && index >= 0) {
      const items = (newCredentials[index] as any)[property];
      const itemIndex = findIndexByProperty(items, "id", childObjId);
      itemIndex >= 0 &&
        (newCredentials[index] as any)[property].splice(itemIndex, 1);
    } else if (id && index >= 0) {
      /**
       * delete credentilas
       */
      newCredentials.splice(index, 1);
    }
    setCredentials(newCredentials);
  };

  const handleInputChange = (
    id: string,
    evt: any,
    value: string | boolean,
    childObjId?: string,
    property?: string
  ) => {
    const elementName: string = evt.target.name;
    const newCredentials: ICredential[] = [...credentials];
    const index: number = findIndexByProperty(newCredentials, "id", id);
    /**
     * save child object's fields value i.e. screts and extensions
     */
    if (index >= 0 && elementName) {
      if (childObjId && property) {
        const items = (newCredentials[index] as any)[property];
        const itemIndex = findIndexByProperty(items, "id", childObjId);
        if (itemIndex >= 0) {
          (newCredentials[index] as any)[property][itemIndex][
            elementName
          ] = value;
        }
      } else {
        /**
         * save crentials (parent object) fields value
         */
        (newCredentials[index] as any)[elementName] = value;
      }
      setCredentials(newCredentials);
    }
  };

  const onToggleAdvancedSetting = (id: string) => {
    const newCredentials: ICredential[] = [...credentials];
    const index = findIndexByProperty(newCredentials, "id", id);
    if (index >= 0) {
      const isExpandedAdvancedSetting =
        newCredentials[index]["isExpandedAdvancedSetting"];
      newCredentials[index][
        "isExpandedAdvancedSetting"
      ] = !isExpandedAdvancedSetting;
      setCredentials(newCredentials);
    }
  };

  return (
    <PageSection>
      <CredentialList
        credentials={credentials}
        handleInputChange={handleInputChange}
        onToggleAdvancedSetting={onToggleAdvancedSetting}
        onSelectType={onSelectType}
        addMoreItem={addMoreItem}
        onDeleteItem={onDeleteItem}
      />

      {/* below line of code will remove later */}
      <pre>{JSON.stringify(credentials, null, 2)}</pre>
    </PageSection>
  );
};
