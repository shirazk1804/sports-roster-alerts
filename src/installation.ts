import AsyncStorage from "@react-native-async-storage/async-storage";

import * as SecureStore from "expo-secure-store";

const INSTALLATION_ID_KEY =
  "sports_roster_alerts_installation_id";

const AUTH_TOKEN_KEY =
  "sports_roster_alerts_auth_token";

function createInstallationId(): string {
  return (
    Date.now().toString(36) +
    "-" +
    Math.random().toString(36).substring(2) +
    "-" +
    Math.random().toString(36).substring(2)
  );
}

export async function getInstallationId():
  Promise<string> {

  const existingId =
    await AsyncStorage.getItem(
      INSTALLATION_ID_KEY
    );

  if (existingId) {
    return existingId;
  }

  const newId =
    createInstallationId();

  await AsyncStorage.setItem(
    INSTALLATION_ID_KEY,
    newId
  );

  return newId;
}

export async function getAuthToken():
  Promise<string | null> {

  return SecureStore.getItemAsync(
    AUTH_TOKEN_KEY
  );
}

export async function saveAuthToken(
  authToken: string
): Promise<void> {

  await SecureStore.setItemAsync(
    AUTH_TOKEN_KEY,
    authToken
  );
}

export async function deleteAuthToken():
  Promise<void> {

  await SecureStore.deleteItemAsync(
    AUTH_TOKEN_KEY
  );
}