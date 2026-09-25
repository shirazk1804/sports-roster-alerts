import { Stack } from "expo-router";
import { useEffect } from "react";
import { Platform } from "react-native";

import {
  registerInstallation,
  registerPushToken,
} from "../api";

import {
  getAuthToken,
  getInstallationId,
  saveAuthToken,
} from "../installation";

export default function RootLayout() {

  useEffect(() => {

    async function initializeApp() {

      try {
        const installationId =
          await getInstallationId();

        let authToken =
          await getAuthToken();

        const user =
          await registerInstallation(
            installationId
          );

        console.log(
          "Installation registered. User ID:",
          user.id
        );

        /*
         * During the authentication migration,
         * User 1 will receive an auth token the
         * first time the new backend sees this
         * installation.
         */
        if (
          !authToken &&
          user.authToken
        ) {
          await saveAuthToken(
            user.authToken
          );

          authToken =
            user.authToken;

          console.log(
            "Authentication token saved securely."
          );
        }

        if (Platform.OS !== "web") {

          const {
            getExpoPushToken,
          } = await import(
            "../notifications"
          );

          const pushToken =
            await getExpoPushToken();

          await registerPushToken(
            pushToken
          );

          console.log(
            "Push token registered with backend."
          );
        }

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