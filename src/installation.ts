import AsyncStorage from "@react-native-async-storage/async-storage";

const INSTALLATION_ID_KEY =
  "sports_roster_alerts_installation_id";

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