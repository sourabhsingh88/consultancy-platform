import React, {useEffect, useState} from 'react';
import {FlatList, Text} from 'react-native';
import {api} from '@shared/api';
import {Button, Screen, styles} from '@shared/ui';

export function SlotsScreen({route, navigation}: any) {
  const consultant = route.params.consultant;
  const [slots, setSlots] = useState<any[]>([]);

  useEffect(() => {
    const from = new Date().toISOString();
    const to = new Date(Date.now() + 7 * 86400000).toISOString();
    api.get(`/consultants/${consultant.publicId}/slots`, {params: {from, to}}).then(response => setSlots(response.data.data));
  }, [consultant.publicId]);

  async function book(slot: any) {
    const response = await api.post('/bookings/consultations', {
      consultantPublicId: consultant.publicId,
      startsAt: slot.startsAt,
      endsAt: slot.endsAt,
      notes: '',
    }, {headers: {'Idempotency-Key': `${Date.now()}`}});
    navigation.navigate('Meeting', {meetingPublicId: response.data.data.meetingPublicId});
  }

  return (
    <Screen>
      <Text style={styles.title}>{consultant.displayName}</Text>
      <FlatList
        data={slots}
        keyExtractor={item => item.publicId}
        renderItem={({item}) => (
          <Screen style={styles.card}>
            <Text>{new Date(item.startsAt).toLocaleString()}</Text>
            <Button title="Book" onPress={() => book(item)} />
          </Screen>
        )}
      />
    </Screen>
  );
}
