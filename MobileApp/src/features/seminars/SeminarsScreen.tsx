import React, {useEffect, useState} from 'react';
import {FlatList, Text} from 'react-native';
import {api} from '@shared/api';
import {Button, Screen, styles} from '@shared/ui';

export function SeminarsScreen({navigation}: any) {
  const [seminars, setSeminars] = useState<any[]>([]);
  useEffect(() => {
    api.get('/seminars').then(response => setSeminars(response.data.data));
  }, []);

  async function register(item: any) {
    const response = await api.post(`/seminars/${item.publicId}/registrations`, {}, {headers: {'Idempotency-Key': `${Date.now()}`}});
    navigation.navigate('Meeting', {meetingPublicId: response.data.data.meetingPublicId});
  }

  return (
    <Screen>
      <Text style={styles.title}>Seminars</Text>
      <FlatList
        data={seminars}
        keyExtractor={item => item.publicId}
        renderItem={({item}) => (
          <Screen style={styles.card}>
            <Text style={styles.title}>{item.title}</Text>
            <Text>{new Date(item.startsAt).toLocaleString()}</Text>
            <Text>{item.confirmedCount}/{item.maxParticipants}</Text>
            <Button title="Register" onPress={() => register(item)} />
          </Screen>
        )}
      />
    </Screen>
  );
}
