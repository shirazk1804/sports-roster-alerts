import {
  useEffect,
} from "react";

import {
  Stack,
} from "expo-router";

import {
  registerPushToken,
} from "../api";

import {
  getExpoPushToken,
} from "../notifications";

export default function RootLayout() {

  useEffect(() => {
    async function registerForPush() {
      try {
        const token =
          await getExpoPushToken();

        await registerPushToken(
          token
        );

        console.log(
          "Push token registered with backend."
        );
      } catch (error) {
        console.log(
          "Could not register push token:",
          error
        );
      }
    }

    registerForPush();
  }, []);

  return (
    <Stack
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen name="index" />
      <Stack.Screen name="follow" />
      <Stack.Screen
        name="team-settings"
      />
      <Stack.Screen name="alerts" />
    </Stack>
  );
}