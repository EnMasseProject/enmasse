import * as React from 'react';
import { Wizard } from '@patternfly/react-core';
import { AddressSpaceDefinition } from './AddressDefinitionSpace';

export const CreateAddressSpace: React.FunctionComponent<any> = () => {
  const [isOpen, setIsOpen] = React.useState(true);
  const [name, setName] = React.useState('');
  const [type, setType] = React.useState('topic');
  const [plan, setPlan] = React.useState('small');
  const toggleOpen = () => {
    setIsOpen(!isOpen);
  };
  const steps = [
    {
      name: 'Definition',
      component: (
        <AddressSpaceDefinition
          name={name}
          setName={setName}
          type={type}
          setType={setType}
          plan={plan}
          setPlan={setPlan}
        />
      ),
    },
    {
      name: 'Review',
      component: (
        <p>
          {name}
          {type}
          {plan}
        </p>
      ),
      nextButtonText: 'Finish',
    },
  ];
  return (
    <>
      {isOpen && (
        <Wizard
          isOpen={isOpen}
          onClose={toggleOpen}
          title="Simple Wizard"
          description="Simple Wizard Description"
          steps={steps}
        />
      )}
    </>
  );
};
