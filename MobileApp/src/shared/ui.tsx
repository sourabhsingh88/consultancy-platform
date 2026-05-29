import React from 'react';
import {ActivityIndicator, Pressable, StyleSheet, Text, TextInput, TextInputProps, View, ViewProps} from 'react-native';

export function Screen({children, style}: ViewProps) {
  return <View style={[styles.screen, style]}>{children}</View>;
}

export function Button({title, onPress, loading}: {title: string; onPress: () => void; loading?: boolean}) {
  return (
    <Pressable style={styles.button} onPress={onPress} disabled={loading}>
      {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>{title}</Text>}
    </Pressable>
  );
}

export function Field(props: TextInputProps) {
  return <TextInput placeholderTextColor="#667085" style={styles.input} {...props} />;
}

export const styles = StyleSheet.create({
  screen: {flex: 1, backgroundColor: '#f8fafc', padding: 16, gap: 12},
  title: {fontSize: 24, fontWeight: '700', color: '#101828'},
  subtitle: {fontSize: 14, color: '#475467'},
  card: {backgroundColor: '#fff', borderRadius: 8, padding: 14, gap: 8, borderWidth: 1, borderColor: '#e4e7ec'},
  button: {backgroundColor: '#175cd3', borderRadius: 8, paddingVertical: 12, paddingHorizontal: 16, alignItems: 'center'},
  buttonText: {color: '#fff', fontWeight: '700'},
  input: {backgroundColor: '#fff', borderWidth: 1, borderColor: '#d0d5dd', borderRadius: 8, padding: 12, color: '#101828'},
});
