import React, {useState} from 'react';
import {Text} from 'react-native';
import * as Keychain from 'react-native-keychain';
import {useDispatch} from 'react-redux';
import {api, setAccessToken} from '@shared/api';
import {Button, Field, Screen, styles} from '@shared/ui';
import {signedIn} from '@app/store';

export function AuthScreen() {
  const dispatch = useDispatch();
  const [googleIdToken, setGoogleIdToken] = useState('');
  const [adminEmail, setAdminEmail] = useState('');
  const [adminPassword, setAdminPassword] = useState('');
  const [loading, setLoading] = useState(false);

  async function saveSession(data: any) {
    setAccessToken(data.accessToken);
    await Keychain.setGenericPassword('mobile-device', data.refreshToken, {service: 'refreshToken'});
    dispatch(signedIn({accessToken: data.accessToken, userPublicId: data.user.publicId, roles: data.user.roles}));
  }

  async function loginGoogle() {
    setLoading(true);
    try {
      const response = await api.post('/auth/oauth/login', {provider: 'GOOGLE', providerToken: googleIdToken, deviceId: 'mobile-device'});
      await saveSession(response.data.data);
    } finally {
      setLoading(false);
    }
  }

  async function loginAdmin() {
    setLoading(true);
    try {
      const response = await api.post('/auth/admin/login', {email: adminEmail, password: adminPassword, deviceId: 'mobile-device'});
      await saveSession(response.data.data);
    } finally {
      setLoading(false);
    }
  }

  return (
    <Screen>
      <Text style={styles.title}>Consultancy Platform</Text>
      <Text style={styles.subtitle}>Use a real Google ID token from native Google Sign-In, or admin credentials from backend properties.</Text>
      <Field placeholder="Google ID token" value={googleIdToken} onChangeText={setGoogleIdToken} autoCapitalize="none" />
      <Button title="Continue with Google" onPress={loginGoogle} loading={loading} />
      <Field placeholder="Admin email" value={adminEmail} onChangeText={setAdminEmail} autoCapitalize="none" />
      <Field placeholder="Admin password" value={adminPassword} onChangeText={setAdminPassword} secureTextEntry />
      <Button title="Admin login" onPress={loginAdmin} loading={loading} />
    </Screen>
  );
}
