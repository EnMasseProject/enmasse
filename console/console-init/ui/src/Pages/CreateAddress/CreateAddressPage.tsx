import * as React from "react";
import { Wizard } from "@patternfly/react-core";
import { AddressDefinitaion } from "src/Components/AddressDetail/CreateAddressDefinition";
import { PreviewAddress } from "./PreviewAddress";
import gql from "graphql-tag";
import { useApolloClient } from "@apollo/react-hooks";
import { getPlanAndTypeForAddress } from "src/Components/Common/AddressFormatter";

const CREATE_ADDRESS = gql`
  mutation create_addr($a: Address_enmasse_io_v1beta1_Input!) {
    createAddress(input: $a) {
      Name
      Namespace
      Uid
    }
  }
`;
interface ICreateAddressProps {
  namespace: string;
  addressSpace: string;
  type: string;
  refetch?: () => void;
  isCreateWizardOpen: boolean;
  setIsCreateWizardOpen: (value: boolean) => void;
}
export const CreateAddress: React.FunctionComponent<ICreateAddressProps> = ({
  namespace,
  addressSpace,
  type,
  refetch,
  isCreateWizardOpen,
  setIsCreateWizardOpen
}) => {
  const [addressName, setAddressName] = React.useState("");
  const [addressType, setAddressType] = React.useState(" ");
  const [plan, setPlan] = React.useState(" ");
  const client = useApolloClient();
  const handleAddressChange = (name: string) => {
    setAddressName(name);
  };
  const handleSave = async () => {
    if (addressSpace && addressName && addressType && plan && namespace) {
      const data = await client.mutate({
        mutation: CREATE_ADDRESS,
        variables: {
          a: {
            ObjectMeta: {
              Name: addressSpace + "." + addressName,
              Namespace: namespace
            },
            Spec: {
              Type: addressType.toLowerCase(),
              Plan: getPlanAndTypeForAddress(plan, addressType, type || ""),
              Address: addressName,
              AddressSpace: addressSpace
            }
          }
        }
      });
      console.log(data);
      if (data.data) {
        setIsCreateWizardOpen(false);
        setAddressType("");
        setPlan("");
      }
      if (refetch) refetch();
    }
  };
  const steps = [
    {
      name: "Definition",
      component: (
        <AddressDefinitaion
          addressName={addressName}
          handleAddressChange={handleAddressChange}
          type={addressType}
          setType={setAddressType}
          plan={plan}
          setPlan={setPlan}
        />
      ),
      backButton: "hide"
    },
    {
      name: "Review",
      component: (
        <PreviewAddress
          name={addressName}
          plan={plan}
          type={addressType}
          namespace={namespace || ""}
        />
      ),
      nextButtonText: "Finish"
    }
  ];
  return (
    <Wizard
      isOpen={isCreateWizardOpen}
      isFullHeight={true}
      isFullWidth={true}
      onClose={() => {
        setIsCreateWizardOpen(!isCreateWizardOpen);
        setAddressName("");
      }}
      title="Create new Address"
      steps={steps}
      onNext={() => {
        console.log("next");
      }}
      onSave={handleSave}
    />
  );
};
