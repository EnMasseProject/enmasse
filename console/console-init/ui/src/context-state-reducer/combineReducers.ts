function getInitialState(reducerDict: any) {
  return Object.keys(reducerDict).reduce((acc, curr) => {
    const slice = reducerDict[curr](undefined, { type: undefined });
    return { ...acc, [curr]: slice };
  }, {});
}

export function combineReducers(reducerDict: any) {
  const _initialState = getInitialState(reducerDict);
  return function(state: any = _initialState, action: any) {
    return Object.keys(reducerDict).reduce((acc, curr) => {
      let slice = reducerDict[curr](state[curr], action);
      return { ...acc, [curr]: slice };
    }, state);
  };
}
