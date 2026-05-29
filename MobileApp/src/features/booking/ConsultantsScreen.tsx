import React, {useEffect, useState} from 'react';
import {FlatList, Text, Pressable} from 'react-native';
import {api} from '@shared/api';
import {Screen, styles} from '@shared/ui';

export function ConsultantsScreen({navigation}: any) {
  const [consultants, setConsultants] = useState<any[]>([]);

  useEffect(() => {
    api.get('/consultants').then(response => setConsultants(response.data.data));
  }, []);

  return (
    <Screen>
      <Text style={styles.title}>Consultants</Text>
      <FlatList
        data={consultants}
        keyExtractor={item => item.publicId}
        renderItem={({item}) => (
          <Pressable style={styles.card} onPress={() => navigation.navigate('Slots', {consultant: item})}>
            <Text style={styles.title}>{item.displayName}</Text>
            <Text style={styles.subtitle}>{item.headline}</Text>
            <Text>{item.defaultPriceAmount / 100} {item.currency}</Text>
          </Pressable>
        )}
      />
    </Screen>
  );
}
