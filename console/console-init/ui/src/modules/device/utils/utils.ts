import { IDeviceFilterCriteria } from "../components";

const getFilterCriteriaIndex = (
  criteriaList: IDeviceFilterCriteria[],
  criteria: IDeviceFilterCriteria
) => {
  for (let i = 0; i < criteriaList.length; i++) {
    if (compareFilterCriteria(criteriaList[i], criteria)) {
      return i;
    }
  }
  return -1;
};

const compareFilterCriteria = (
  criteria1: IDeviceFilterCriteria,
  criteria2: IDeviceFilterCriteria
) => {
  if (
    criteria1.key === criteria2.key &&
    criteria1.parameter === criteria2.parameter &&
    criteria1.value === criteria2.value
  ) {
    return true;
  } else {
    return false;
  }
};
export { getFilterCriteriaIndex };
