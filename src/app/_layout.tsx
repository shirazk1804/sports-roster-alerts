import { Stack } from "expo-router";
import { useEffect } from "react";

import {
  registerInstallation,
  registerPushToken,
} from "../api";

import {
  getExpoPushToken,
} from "../notifications";

import {
  getInstallationId,
} from "../installation";

export default function RootLayout() {

  useEffect(() => {

    async function initializeApp() {

      try {
        const installationId =
          await getInstallationId();

        const user =
          await registerInstallation(
            installationId
          );

        console.log(
          "Installation registered. User ID:",
          user.id
        );

        const pushToken =
          await getExpoPushToken();

        await registerPushToken(
          pushToken,
          installationId
        );

        console.log(
          "Push token registered with backend."
        );

      } catch (error) {

        console.log(
          "Could not initialize app:",
          error
        );
      }
    }

    initializeApp();

  }, []);

  return (
    <Stack
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen name="index" />
      <Stack.Screen name="follow" />
      <Stack.Screen name="team-settings" />
      <Stack.Screen name="alerts" />
    </Stack>
  );
}