export interface IInitialState {
  hasServerError: boolean;
  errors: any;
}

export const initialState: IInitialState = {
  hasServerError: false,
  errors: ""
};
