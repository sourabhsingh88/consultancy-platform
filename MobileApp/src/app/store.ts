import {configureStore, createSlice, PayloadAction} from '@reduxjs/toolkit';

type AuthState = {
  accessToken: string | null;
  userPublicId: string | null;
  roles: string[];
};

const authSlice = createSlice({
  name: 'auth',
  initialState: {accessToken: null, userPublicId: null, roles: []} as AuthState,
  reducers: {
    signedIn: (state, action: PayloadAction<AuthState>) => {
      state.accessToken = action.payload.accessToken;
      state.userPublicId = action.payload.userPublicId;
      state.roles = action.payload.roles;
    },
    signedOut: state => {
      state.accessToken = null;
      state.userPublicId = null;
      state.roles = [];
    },
  },
});

export const {signedIn, signedOut} = authSlice.actions;

export const store = configureStore({
  reducer: {
    auth: authSlice.reducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
