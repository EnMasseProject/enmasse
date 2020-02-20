import { types } from "./actions";
import { initialState } from "./initialState";

export interface IActionType {
  type: string;
  payload?: any;
}

export const reducer = (state = initialState, action: IActionType) => {
  switch (action.type) {
    case types.SET_SERVER_ERROR:
      return {
        ...state,
        hasServerError: true,
        errors: action.payload
      };
    case types.RESET_SERVER_ERROR:
      return initialState;
    default:
      return state;
  }
};
