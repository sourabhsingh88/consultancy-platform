import React from 'react';
import {NavigationContainer} from '@react-navigation/native';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import {Provider, useSelector} from 'react-redux';
import {store, RootState} from './store';
import {AuthScreen} from '@features/auth/AuthScreen';
import {ConsultantsScreen} from '@features/booking/ConsultantsScreen';
import {SlotsScreen} from '@features/booking/SlotsScreen';
import {SeminarsScreen} from '@features/seminars/SeminarsScreen';
import {MeetingScreen} from '@features/chat/MeetingScreen';
import {AiSupportScreen} from '@features/ai/AiSupportScreen';

const Stack = createNativeStackNavigator();

function Navigator() {
  const accessToken = useSelector((state: RootState) => state.auth.accessToken);
  return (
    <NavigationContainer>
      <Stack.Navigator>
        {!accessToken ? (
          <Stack.Screen name="Auth" component={AuthScreen} />
        ) : (
          <>
            <Stack.Screen name="Consultants" component={ConsultantsScreen} />
            <Stack.Screen name="Slots" component={SlotsScreen} />
            <Stack.Screen name="Seminars" component={SeminarsScreen} />
            <Stack.Screen name="Meeting" component={MeetingScreen} />
            <Stack.Screen name="AI" component={AiSupportScreen} />
          </>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}

export default function App() {
  return (
    <Provider store={store}>
      <Navigator />
    </Provider>
  );
}
