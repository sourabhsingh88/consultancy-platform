import React, {useState} from 'react';
import {Text} from 'react-native';
import {api} from '@shared/api';
import {Button, Field, Screen, styles} from '@shared/ui';

export function AiSupportScreen({route}: any) {
  const meetingPublicId = route.params.meetingPublicId;
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState('');

  async function ask() {
    const response = await api.post(`/meetings/${meetingPublicId}/ai-support/messages`, {question});
    setAnswer(response.data.data.answer);
  }

  async function summary() {
    const response = await api.post(`/meetings/${meetingPublicId}/summary/generate`);
    setAnswer(response.data.data.summary);
  }

  return (
    <Screen>
      <Text style={styles.title}>AI Support</Text>
      <Field placeholder="Ask a doubt from this meeting" value={question} onChangeText={setQuestion} />
      <Button title="Ask" onPress={ask} />
      <Button title="Generate Summary" onPress={summary} />
      <Text>{answer}</Text>
    </Screen>
  );
}
