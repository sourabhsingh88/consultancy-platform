import axios, {AxiosError, InternalAxiosRequestConfig} from 'axios';
import * as Keychain from 'react-native-keychain';

export const API_BASE_URL = 'http://10.0.2.2:8080/api/v1';
let accessToken: string | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  response => response,
  async (error: AxiosError) => {
    if (error.response?.status !== 401) {
      throw error;
    }
    const saved = await Keychain.getGenericPassword({service: 'refreshToken'});
    if (!saved) {
      throw error;
    }
    const refresh = await axios.post(`${API_BASE_URL}/auth/token/refresh`, {
      refreshToken: saved.password,
      deviceId: saved.username,
    });
    const data = refresh.data.data;
    accessToken = data.accessToken;
    await Keychain.setGenericPassword(saved.username, data.refreshToken, {service: 'refreshToken'});
    const original = error.config as InternalAxiosRequestConfig;
    original.headers.Authorization = `Bearer ${accessToken}`;
    return api(original);
  },
);
