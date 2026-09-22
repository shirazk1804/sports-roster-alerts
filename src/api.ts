const API_URL =
  process.env.EXPO_PUBLIC_API_URL;

export type ApiTeam = {
  id: number;
  league: string;
  name: string;
  abbreviation: string;
  externalTeamId: number | null;
};

export type AppUser = {
  id: number;
  installationId: string;
  createdAt: string;
};

export type FollowedTeam = {
  id: number;
  league: string;
  name: string;
  alertType: string;
};

export type AlertPreferences = {
  [key: string]: boolean;
};

export type RosterEvent = {
  id: number;
  league: string;
  externalTeamId: number;
  teamName: string;
  sourceTransactionId: number;
  playerId: number;
  playerName: string;
  eventType: string;
  eventDate: string;
  description: string;
  createdAt: string;
};

export async function registerInstallation(
  installationId: string
): Promise<AppUser> {

  const response = await fetch(
    `${API_URL}/api/users/register`,
    {
      method: "POST",

      headers: {
        "Content-Type": "application/json",
      },

      body: JSON.stringify({
        installationId,
      }),
    }
  );

  if (!response.ok) {
    throw new Error(
      `Could not register installation (${response.status})`
    );
  }

  return response.json();
}

export async function getTeams():
  Promise<ApiTeam[]> {

  const response = await fetch(
    `${API_URL}/api/teams`
  );

  if (!response.ok) {
    throw new Error(
      `Could not load teams (${response.status})`
    );
  }

  return response.json();
}

export async function getFollowedTeams():
  Promise<FollowedTeam[]> {

  const response = await fetch(
    `${API_URL}/api/followed-teams`
  );

  if (!response.ok) {
    throw new Error(
      `Could not load followed teams (${response.status})`
    );
  }

  return response.json();
}

export async function followTeam(
  league: string,
  name: string
): Promise<FollowedTeam> {

  const response = await fetch(
    `${API_URL}/api/followed-teams`,
    {
      method: "POST",

      headers: {
        "Content-Type":
          "application/json",
      },

      body: JSON.stringify({
        league,
        name,
        alertType:
          "All roster transactions",
      }),
    }
  );

  const data =
    await response.json();

  if (!response.ok) {
    throw new Error(
      data.message ||
      `Could not follow team (${response.status})`
    );
  }

  return data;
}

export async function deleteFollowedTeam(
  id: number
): Promise<void> {

  const response = await fetch(
    `${API_URL}/api/followed-teams/${id}`,
    {
      method: "DELETE",
    }
  );

  if (!response.ok) {
    throw new Error(
      `Could not unfollow team (${response.status})`
    );
  }
}

export async function getAlertPreferences(
  teamId: number
): Promise<AlertPreferences> {

  const response = await fetch(
    `${API_URL}/api/followed-teams/${teamId}/alert-preferences`
  );

  if (!response.ok) {
    throw new Error(
      `Could not load alert preferences (${response.status})`
    );
  }

  return response.json();
}

export async function saveAlertPreferences(
  teamId: number,
  settings: AlertPreferences
): Promise<AlertPreferences> {

  const response = await fetch(
    `${API_URL}/api/followed-teams/${teamId}/alert-preferences`,
    {
      method: "PUT",

      headers: {
        "Content-Type":
          "application/json",
      },

      body: JSON.stringify(
        settings
      ),
    }
  );

  if (!response.ok) {
    throw new Error(
      `Could not save alert preferences (${response.status})`
    );
  }

  return response.json();
}

export async function getRosterEvents():
  Promise<RosterEvent[]> {

  const response = await fetch(
    `${API_URL}/api/events`
  );

  if (!response.ok) {
    throw new Error(
      `Could not load alerts (${response.status})`
    );
  }

  return response.json();
}

export async function registerPushToken(
  token: string,
  installationId: string
): Promise<void> {

  const response = await fetch(
    `${API_URL}/api/push-tokens`,
    {
      method: "POST",

      headers: {
        "Content-Type":
          "application/json",
      },

      body: JSON.stringify({
        token,
        platform: "ios",
        installationId,
      }),
    }
  );

  if (!response.ok) {
    throw new Error(
      `Could not register push token (${response.status})`
    );
  }
}