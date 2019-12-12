import * as React from "react";
import { Wizard } from "@patternfly/react-core";
import { AddressSpaceConfiguration } from "src/Pages/CreateAddressSpace/CreateAddressSpaceConfiguration";
import { ReviewAddressSpace } from "src/Pages/CreateAddressSpace/ReviewAddressSpace";
import { useApolloClient } from "@apollo/react-hooks";
import { CREATE_ADDRESS_SPACE } from "src/Queries/Queries";
import { getPlanAndTypeForAddressEdit } from "src/Components/Common/AddressFormatter";
interface ICreateAddressSpaceProps {
  refetch?: () => void;
  isCreateWizardOpen: boolean;
  setIsCreateWizardOpen: (value: boolean) => void;
}

interface ICreateAddressSpaceProps {
  isCreateWizardOpen: boolean;
  setIsCreateWizardOpen: (value: boolean) => void;
}
export const CreateAddressSpace: React.FunctionComponent<ICreateAddressSpaceProps> = ({
  isCreateWizardOpen,
  setIsCreateWizardOpen,
  refetch
}) => {
  const [addressSpaceName, setAddressSpaceName] = React.useState("");
  const [addressSpaceType, setAddressSpaceType] = React.useState(" ");
  const [addressSpacePlan, setAddressSpacePlan] = React.useState(" ");
  const [namespace, setNamespace] = React.useState(" ");
  const [authenticationService, setAuthenticationService] = React.useState(" ");
  const client = useApolloClient();

  const handleSave = async () => {
    if (
      addressSpaceName &&
      authenticationService &&
      addressSpaceType &&
      addressSpacePlan &&
      namespace
    ) {
      const data = await client.mutate({
        mutation: CREATE_ADDRESS_SPACE,
        variables: {
          as: {
            ObjectMeta: {
              Name: addressSpaceName,
              Namespace: namespace
            },
            Spec: {
              Type: addressSpaceType.toLowerCase(),
              Plan: addressSpacePlan.toLowerCase()
            }
          }
        }
      });
      console.log(data);
      if (data.data) {
        setIsCreateWizardOpen(false);
        setAddressSpaceType("");
        setAddressSpacePlan("");
        setAddressSpaceName("");
        setNamespace("");
        setAuthenticationService("");
      }
      if (refetch) refetch();
    }
  };

  const steps = [
    {
      name: "Configuration",
      component: (
        <AddressSpaceConfiguration
          name={addressSpaceName}
          setName={setAddressSpaceName}
          namespace={namespace}
          setNamespace={setNamespace}
          type={addressSpaceType}
          setType={setAddressSpaceType}
          plan={addressSpacePlan}
          setPlan={setAddressSpacePlan}
          authenticationService={authenticationService}
          setAuthenticationService={setAuthenticationService}
        />
      ),
      backButton: "hide"
    },
    {
      name: "Review",
      component: (
        <ReviewAddressSpace
          name={addressSpaceName}
          namespace={namespace}
          type={addressSpaceType}
          plan={addressSpacePlan}
          authenticationService={authenticationService}
        />
      ),
      nextButtonText: "Finish"
    }
  ];
  const onClose = () => {
    setIsCreateWizardOpen(!isCreateWizardOpen);
  };
  return (
    <React.Fragment>
      <Wizard
        id="create-as-wizard"
        isOpen={true}
        isFullHeight={true}
        isFullWidth={true}
        onClose={onClose}
        title="Create an Instance"
        steps={steps}
        onNext={() => {
          console.log("next");
        }}
        onSave={() => {
          console.log("Save");
          setIsCreateWizardOpen(!isCreateWizardOpen);
        }}
      />
      )}
    </React.Fragment>
  );
};
