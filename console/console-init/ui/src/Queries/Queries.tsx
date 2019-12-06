import gql from "graphql-tag";

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

export const ALL_ADDRESS_SPACES = gql`
  query all_address_spaces {
    addressSpaces {
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

export const RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE = (
  page: number,
  perPage: number,
  name?: string,
  namespace?: string,
  filter?: string | null,
  inputValue?: string | null,
  typeValue?: string | null,
  statusValue?: string | null
) => {
  let filterString = "";
  if (name && name.trim() !== "") {
    filterString += "`$.Spec.AddressSpace` = '" + name + "' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filterString += "`$.ObjectMeta.Namespace` = '" + namespace + "'";
  }
  if (filter) {
    if (filter.trim().toLowerCase() === "name") {
      if (inputValue && inputValue.trim() !== "") {
        filterString += "AND `$.ObjectMeta.Name` = '" + inputValue + "'";
      }
    } else if (filter.trim().toLowerCase() === "type") {
      if (typeValue) {
        filterString += "AND `$.Spec.Type` = '" + typeValue.toLowerCase() + "'";
      }
    } else if (filter.trim().toLowerCase() === "status") {
      if (statusValue) {
        let status = "";
        if (statusValue === "Failed") {
          status = "Pending";
        } else {
          status = statusValue;
        }
        filterString += "AND `$.Status.Phase` = '" + status + "'";
      }
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

export const RETURN_ADDRESS_DETAIL = (
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
  console.log("page,perpage", page, perPage);
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
  page:number,
  perPage:number,
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
  console.log(filter);
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
              Namespace
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

export const RETURN_ADDRESS_PLANS = gql`
  query all_address_plans {
    addressPlans (
      addressSpacePlan:"standard-small"
    ) {
      Spec {
        AddressType,
        DisplayName,
        LongDescription,
        ShortDescription
      }
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

export const RETURN_ADDRESS_TYPES = gql`
  query addressTypes {
    addressTypes_v2(addressSpaceType :standard) {
      ObjectMeta
      {Name}
      Spec {
        DisplayName
        LongDescription
        ShortDescription
      }
    }
  }
`;
