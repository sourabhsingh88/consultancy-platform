import React, {useEffect, useRef, useState} from 'react';
import {FlatList, Text} from 'react-native';
import {Client, Message, Stomp} from 'stompjs';
import {useSelector} from 'react-redux';
import {api} from '@shared/api';
import {Button, Field, Screen, styles} from '@shared/ui';
import type {RootState} from '@app/store';

export function MeetingScreen({route, navigation}: any) {
  const meetingPublicId = route.params.meetingPublicId;
  const accessToken = useSelector((state: RootState) => state.auth.accessToken);
  const [messages, setMessages] = useState<any[]>([]);
  const [body, setBody] = useState('');
  const client = useRef<Client | null>(null);

  useEffect(() => {
    api.get(`/meetings/${meetingPublicId}/messages`).then(response => setMessages(response.data.data));
    const socket = new WebSocket('ws://10.0.2.2:8080/ws');
    client.current = Stomp.over(socket);
    client.current.connect({Authorization: `Bearer ${accessToken}`}, () => {
      client.current?.subscribe(`/topic/meetings/${meetingPublicId}/messages`, (message: Message) => {
        setMessages(current => [...current, JSON.parse(message.body)]);
      });
    });
    return () => client.current?.disconnect(() => undefined);
  }, [accessToken, meetingPublicId]);

  function send() {
    if (!body.trim()) {
      return;
    }
    client.current?.send(`/app/meetings/${meetingPublicId}/chat.send`, {}, JSON.stringify({
      clientMessageId: `${Date.now()}`,
      body,
      messageType: 'TEXT',
    }));
    setBody('');
  }

  return (
    <Screen>
      <Text style={styles.title}>Meeting</Text>
      <FlatList data={messages} keyExtractor={item => item.publicId} renderItem={({item}) => (
        <Screen style={styles.card}>
          <Text style={styles.subtitle}>{item.senderPublicId}</Text>
          <Text>{item.body}</Text>
        </Screen>
      )} />
      <Field placeholder="Message" value={body} onChangeText={setBody} />
      <Button title="Send" onPress={send} />
      <Button title="AI Support" onPress={() => navigation.navigate('AI', {meetingPublicId})} />
    </Screen>
  );
}
