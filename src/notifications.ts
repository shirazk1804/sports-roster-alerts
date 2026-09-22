import Constants from "expo-constants";
import * as Notifications from "expo-notifications";

import type {
    RosterEvent,
} from "./api";

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

async function makeSureNotificationsAreAllowed() {
  const currentPermission =
    await Notifications.getPermissionsAsync();

  if (
    currentPermission.status === "granted"
  ) {
    return;
  }

  const requestedPermission =
    await Notifications.requestPermissionsAsync();

  if (
    requestedPermission.status !== "granted"
  ) {
    throw new Error(
      "Notification permission was not granted."
    );
  }
}

export async function getExpoPushToken() {
  await makeSureNotificationsAreAllowed();

  const projectId =
    Constants.expoConfig?.extra?.eas?.projectId ??
    Constants.easConfig?.projectId;

  if (!projectId) {
    throw new Error(
      "EAS project ID could not be found."
    );
  }

  const token =
    await Notifications.getExpoPushTokenAsync({
      projectId,
    });

  return token.data;
}

export async function sendTestNotification() {
  await makeSureNotificationsAreAllowed();

  await Notifications.scheduleNotificationAsync({
    content: {
      title: "Sports Roster Alerts",
      body: "Test notification: roster alerts are working.",
    },
    trigger: null,
  });
}

export async function sendRosterEventNotification(
  event: RosterEvent
) {
  await makeSureNotificationsAreAllowed();

  await Notifications.scheduleNotificationAsync({
    content: {
      title:
        `${event.teamName}: ${event.playerName}`,

      body: event.description,

      data: {
        rosterEventId: event.id,
        teamName: event.teamName,
        eventType: event.eventType,
      },
    },

    trigger: null,
  });
}