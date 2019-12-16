import gql from "graphql-tag";
import { ISortBy } from "@patternfly/react-table";

export const DOWNLOAD_CERTIFICATE = gql`
  query messagingCertificateChain($as: ObjectMeta_v1_Input!) {
    messagingCertificateChain(input: $as)
  }
`;
export const DELETE_ADDRESS_SPACE = gql`
  mutation delete_as($a: ObjectMeta_v1_Input!) {
    deleteAddressSpace(input: $a)
  }
`;
export const DELETE_ADDRESS = gql`
  mutation delete_addr($a: ObjectMeta_v1_Input!) {
    deleteAddress(input: $a)
  }
`;

export const RETURN_ALL_ADDRESS_SPACES = (
  page: number,
  perPage: number,
  filter_Names?: string[],
  filter_NameSpace?: string[],
  filter_Type?: string | null
) => {
  let filter = "";
  if (filter_Names && filter_Names.length > 0) {
    filter += "`$.ObjectMeta.Name` ='" + filter_Names[0].trim() + "' ";
    let i;
    for (i = 1; i < filter_Names.length; i++) {
      filter += "OR `$.ObjectMeta.Name` ='" + filter_Names[i].trim() + "' ";
    }
  }
  if (
    filter_Names &&
    filter_Names.length > 0 &&
    filter_NameSpace &&
    filter_NameSpace.length > 0
  ) {
    filter += " AND ";
  }
  if (filter_NameSpace && filter_NameSpace.length > 0) {
    filter += "`$.ObjectMeta.Namespace` ='" + filter_NameSpace[0].trim() + "' ";
    let i;
    for (i = 1; i < filter_NameSpace.length; i++) {
      filter +=
        "OR `$.ObjectMeta.Namespace` ='" + filter_NameSpace[i].trim() + "' ";
    }
  }
  if (
    ((filter_Names && filter_Names.length > 0) ||
      (filter_NameSpace && filter_NameSpace.length > 0)) &&
    filter_Type &&
    filter_Type.trim() !== ""
  ) {
    filter += " AND ";
  }
  if (filter_Type && filter_Type.trim() != "") {
    filter += "`$.Spec.Type` ='" + filter_Type.toLowerCase().trim() + "' ";
  }
  console.log(filter);
  const ALL_ADDRESS_SPACES = gql`
    query all_address_spaces {
      addressSpaces(filter: "${filter}"  
      first:${perPage} offset:${perPage * (page - 1)}) {
        Total
        AddressSpaces {
          ObjectMeta {
            Namespace
            Name
            CreationTimestamp
          }
          Spec {
            Type
            Plan {
              Spec {
                DisplayName
              }
            }
          }
          Status {
            IsReady
          }
        }
      }
    }
  `;

  return ALL_ADDRESS_SPACES;
};

export const RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE = (
  page: number,
  perPage: number,
  name?: string,
  namespace?: string,
  filterNames?: string[],
  typeValue?: string | null,
  statusValue?: string | null,
  sortBy?: ISortBy
) => {
  let filterString = "";
  if (name && name.trim() !== "") {
    filterString += "`$.Spec.AddressSpace` = '" + name + "' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filterString += "`$.ObjectMeta.Namespace` = '" + namespace + "'";
  }
  if ((filterNames && filterNames.length > 0) || typeValue || statusValue) {
    filterString += " AND ";
  }
  if (filterNames && filterNames.length > 0) {
    filterString += "`$.ObjectMeta.Name` ='" + filterNames[0].trim() + "' ";
    let i;
    for (i = 1; i < filterNames.length; i++) {
      filterString +=
        "OR `$.ObjectMeta.Name` ='" + filterNames[i].trim() + "' ";
    }
  }
  if (filterNames && filterNames.length > 0 && (typeValue || statusValue)) {
    filterString += " AND ";
  }
  if (typeValue) {
    filterString += "`$.Spec.Type` = '" + typeValue.toLowerCase() + "'";
  }
  if (typeValue && statusValue) {
    filterString += " AND ";
  }
  if (statusValue) {
    let status = "";
    if (statusValue === "Failed") {
      status = "Pending";
    } else {
      status = statusValue;
    }
    filterString += "`$.Status.Phase` = '" + status + "'";
  }
  if (sortBy) {
    let sort_filter = "";
    switch (sortBy.index) {
      case 3:
        break;
      case 4:
        break;
      case 5:
        break;
    }
    if (sortBy.direction === "desc") {
      //Change
    } else if (sortBy.direction === "asc") {
    }
  }

  const ALL_ADDRESS_FOR_ADDRESS_SPACE = gql`
  query all_addresses_for_addressspace_view {
    addresses( first:${perPage} offset:${perPage * (page - 1)}
      filter:"${filterString}"
    ) {
      Total
      Addresses {
        ObjectMeta {
          Namespace
          Name
        }
        Spec {
          Address
          Type
          Plan {
            Spec {
              DisplayName
            }
          }
        }
        Status {
          PlanStatus{
            Partitions
          }
          Phase
          IsReady
          Messages
        }
        Metrics {
          Name
          Type
          Value
          Units
        }
      }
    }
  }
`;
  return ALL_ADDRESS_FOR_ADDRESS_SPACE;
};

export const RETURN_ADDRESS_SPACE_DETAIL = (
  name?: string,
  namespace?: string
) => {
  const ADDRESS_SPACE_DETAIL = gql`
    query all_address_spaces {
      addressSpaces(
        filter: "\`$..Name\` = '${name}' AND \`$..Namespace\` = '${namespace}'"
      ) {
        AddressSpaces {
          ObjectMeta {
            Namespace
            Name
            CreationTimestamp
          }
          Spec {
            Type
            Plan {
              ObjectMeta {
                Name
              }
              Spec {
                DisplayName
              }   
            }
          }
          Status {
            IsReady
            Messages
          }
        }
      }
    }`;
  return ADDRESS_SPACE_DETAIL;
};

export const CURRENT_ADDRESS_SPACE_PLAN = (
  name?: string,
  namespace?: string
) => {
  const ADDRESS_SPACE_PLAN = gql`
    query all_address_spaces {
      addressSpaces(
        filter: "\`$..Name\` = '${name}' AND \`$..Namespace\` = '${namespace}'"
      ) {
        AddressSpaces {
          Spec {
            Plan {
              ObjectMeta {
                Name
              }   
            }
          }
        }
      }
    }`;
  return ADDRESS_SPACE_PLAN;
};

export const RETURN_ADDRESS_DETAIL = (
  addressSpace?: string,
  namespace?: string,
  addressName?: string
) => {
  let filter = "";
  if (addressSpace) {
    filter += "`$.Spec.AddressSpace` = '" + addressSpace + "' AND ";
  }
  if (namespace) {
    filter += "`$.ObjectMeta.Namespace` = '" + namespace + "' AND ";
  }
  if (addressName) {
    filter += "`$.ObjectMeta.Name` = '" + addressName + "'";
  }
  const ADDRESSDETAIL = gql`
  query single_addresses {
    addresses(
      filter: "${filter}" 
    ) {
      Total
      Addresses {
        ObjectMeta {
          Namespace
          Name
          CreationTimestamp
        }
        Spec {
          Address
          Plan {
            Spec {
              DisplayName
              AddressType
            }
          }
        }
        Status {
          IsReady
          Messages
          Phase
          PlanStatus {
            Partitions
          }
        }
        Metrics {
          Name
          Type
          Value
          Units
        }
      }
    }
  }
  `;
  return ADDRESSDETAIL;
};

export const RETURN_ADDRESS_LINKS = (
  page: number,
  perPage: number,
  addressSpace?: string,
  namespace?: string,
  addressName?: string
) => {
  let filter = "";
  if (addressSpace) {
    filter += "`$.Spec.AddressSpace` = '" + addressSpace + "' AND ";
  }
  if (namespace) {
    filter += "`$.ObjectMeta.Namespace` = '" + namespace + "' AND ";
  }
  if (addressName) {
    filter += "`$.ObjectMeta.Name` = '" + addressName + "'";
  }
  const query = gql`
  query single_address_with_links_and_metrics {
    addresses(
      filter: "${filter}" 
    ) {
      Total
      Addresses {
        ObjectMeta {
          Name
        }
        Spec {
          AddressSpace
        }
        Links (first:${perPage} offset:${perPage * (page - 1)}){
          Total
          Links {
            ObjectMeta {
              Name
            }
            Spec {
              Role
              Connection {
                ObjectMeta{
                  Name
                  Namespace
                }
                Spec {
                  ContainerId
                }
              }
            }
            Metrics {
              Name
              Type
              Value
              Units
            }
          }
        }
      }
    }
  }
  `;
  return query;
};

export const RETURN_ADDRESS_PLANS = (
  addressSpacePlan: string,
  addressType: string
) => {
  const ADDRESS_PLANS = gql`
  query all_address_plans {
    addressPlans(addressSpacePlan: "${addressSpacePlan}", addressType: ${addressType}) {
      ObjectMeta {
        Name
      }
      Spec {
        AddressType
        DisplayName
        LongDescription
        ShortDescription
      }
    }
  }
`;
  return ADDRESS_PLANS;
};

export const CREATE_ADDRESS = gql`
  mutation create_addr($a: Address_enmasse_io_v1beta1_Input!) {
    createAddress(input: $a) {
      Name
      Namespace
      Uid
    }
  }
`;

export const CREATE_ADDRESS_SPACE = gql`
  mutation create_as($as: AddressSpace_enmasse_io_v1beta1_Input!) {
    createAddressSpace(input: $as) {
      Name
      Uid
      CreationTimestamp
    }
  }
`;

export const EDIT_ADDRESS = gql`
  mutation patch_addr(
    $a: ObjectMeta_v1_Input!
    $jsonPatch: String!
    $patchType: String!
  ) {
    patchAddress(input: $a, jsonPatch: $jsonPatch, patchType: $patchType)
  }
`;

export const ADDRESS_COMMAND_PRIVEW_DETAIL = gql`
  query cmd($a: Address_enmasse_io_v1beta1_Input!) {
    addressCommand(input: $a)
  }
`;

export const ADDRESS_SPACE_COMMAND_REVIEW_DETAIL = gql`
  query cmd($as: AddressSpace_enmasse_io_v1beta1_Input!) {
    addressSpaceCommand(input: $as)
  }
`;

export const RETURN_ALL_CONECTION_LIST = (
  page: number,
  perPage: number,
  hosts: string[],
  containers: string[],
  name?: string,
  namespace?: string
) => {
  let filter = "";
  if (name) {
    filter += "`$.Spec.AddressSpace.ObjectMeta.Name` = '" + name + "'";
  }
  if (namespace) {
    filter +=
      " AND `$.Spec.AddressSpace.ObjectMeta.Namespace` = '" + namespace + "'";
  }
  if ((hosts && hosts.length > 0) || (containers && containers.length > 0)) {
    filter += " AND ";
  }
  if (hosts) {
    let i;
    if (hosts.length > 0) {
      filter += "`$.Spec.Hostname` ='" + hosts[0].trim() + "' ";
      let i;
      for (i = 1; i < hosts.length; i++) {
        filter += "OR `$.Spec.Hostname` ='" + hosts[i].trim() + "' ";
      }
    }
  }
  if (containers) {
    if (containers.length > 0) {
      if (hosts && hosts.length <= 0) {
        filter += "`$.Spec.ContainerId` ='" + containers[0].trim() + "' ";
        let i;
        for (i = 1; i < containers.length; i++) {
          filter += "OR `$.Spec.ContainerId` ='" + containers[i].trim() + "' ";
        }
      } else {
        let i;
        for (i = 0; i < containers.length; i++) {
          filter += "OR `$.Spec.ContainerId` ='" + containers[i].trim() + "' ";
        }
      }
    }
  }
  const ALL_CONECTION_LIST = gql(
    `query all_connections_for_addressspace_view {
      connections(
        filter: "${filter}" first:${perPage} offset:${perPage * (page - 1)}
      ) {
      Total
      Connections {
        ObjectMeta {
          Name
        }
        Spec {
          Hostname
          ContainerId
          Protocol
          Encrypted
        }
        Metrics {
          Name
          Type
          Value
          Units
        }
      }
    }
  }`
  );
  return ALL_CONECTION_LIST;
};

export const RETURN_CONNECTION_DETAIL = (
  addressSpaceName?: string,
  addressSpaceNameSpcae?: string,
  connectionName?: string
) => {
  let filter = "";
  if (addressSpaceName) {
    filter +=
      "`$.Spec.AddressSpace.ObjectMeta.Name` = '" + addressSpaceName + "' AND ";
  }
  if (addressSpaceNameSpcae) {
    filter +=
      "`$.Spec.AddressSpace.ObjectMeta.Namespace` = '" +
      addressSpaceNameSpcae +
      "' AND ";
  }
  if (connectionName) {
    filter += "`$.ObjectMeta.Name` = '" + connectionName + "'";
  }
  const CONNECTION_DETAIL = gql`
  query single_connections {
    connections(
      filter: "${filter}" 
    ) {
      Total
      Connections {
        ObjectMeta {
          Name
          Namespace
          CreationTimestamp
        }
        Spec {
          Hostname
          ContainerId
          Protocol,
          Properties{
            Key
            Value
          }
        }
        Metrics {
          Name
          Type
          Value
          Units
        }
      }
    }
  }
  `;
  return CONNECTION_DETAIL;
};

export const RETURN_ADDRESS_TYPES = gql`
  query addressTypes {
    addressTypes_v2(addressSpaceType: standard) {
      ObjectMeta {
        Name
      }
      Spec {
        DisplayName
        LongDescription
        ShortDescription
      }
    }
  }
`;
export const RETURN_NAMESPACES = gql`
  query all_namespaces {
    namespaces {
      ObjectMeta {
        Name
      }
      Status {
        Phase
      }
    }
  }
`;

export const RETURN_ADDRESS_SPACE_PLANS = gql`
  query all_address_space_plans {
    addressSpacePlans {
      ObjectMeta {
        Name
        Uid
        CreationTimestamp
      }
      Spec {
        AddressSpaceType
      }
    }
  }
`;

export const RETURN_CONNECTION_LINKS = (
  page: number,
  perPage: number,
  addressSpaceName?: string,
  addressSpaceNameSpcae?: string,
  connectionName?: string
) => {
  let filter = "";
  if (addressSpaceName) {
    filter +=
      "`$.Spec.AddressSpace.ObjectMeta.Name` = '" + addressSpaceName + "' AND ";
  }
  if (addressSpaceNameSpcae) {
    filter +=
      "`$.Spec.AddressSpace.ObjectMeta.Namespace` = '" +
      addressSpaceNameSpcae +
      "' AND ";
  }
  if (connectionName) {
    filter += "`$.ObjectMeta.Name` = '" + connectionName + "'";
  }
  const CONNECTION_DETAIL = gql`
  query single_connections {
    connections(
      filter: "${filter}" 
    ) {
      Total
      Connections {
        ObjectMeta {
          Name
          Namespace
        }
        Links(first:${perPage} offset:${perPage * (page - 1)}) {
          Total
          Links {
            ObjectMeta {
              Name
            }
            Spec {
              Role
              Address
            }
            Metrics {
              Name
              Type
              Value
              Units
            }
          }
        }
      }
    }
  }
  `;
  return CONNECTION_DETAIL;
};

export const RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION = (
  name: string,
  namespace: string,
  type: string
) => {
  let filterString = "";
  if (name && name.trim() !== "") {
    filterString += "`$.Spec.AddressSpace` = '" + name + "' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filterString += "`$.ObjectMeta.Namespace` = '" + namespace + "'";
  }
  if (type.trim().toLowerCase() === "subscription") {
    filterString += " AND `$.Spec.Type` = 'topic'";
  }
  const ALL_TOPICS_FOR_ADDRESS_SPACE = gql`
  query all_addresses_for_addressspace_view {
    addresses(
      filter:"${filterString}"
    ) {
      Total
      Addresses {
        ObjectMeta {
          Namespace
          Name
        }
        Spec {
          Address
          Type
          Plan {
            Spec {
              DisplayName
            }
          }
        }
      }
    }
  }
`;
  return ALL_TOPICS_FOR_ADDRESS_SPACE;
};
