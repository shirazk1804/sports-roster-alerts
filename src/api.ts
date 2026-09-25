import {
  getAuthToken,
} from "./installation";

const API_URL =
  process.env.EXPO_PUBLIC_API_URL;

async function getAuthHeaders() {
  const authToken =
    await getAuthToken();

  if (!authToken) {
    throw new Error(
      "Authentication token is missing"
    );
  }

  return {
    Authorization:
      `Bearer ${authToken}`,
  };
}

async function getJsonAuthHeaders() {
  return {
    ...(await getAuthHeaders()),
    "Content-Type": "application/json",
  };
}

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
  authToken: string | null;
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

  externalTeamId:
  number | null;

  externalProviderTeamId:
  string | null;

  teamName: string;

  sourceTransactionId:
  number | null;

  sourceProviderEventId:
  string | null;

  playerId:
  number | null;

  playerProviderId:
  string | null;

  playerName: string;
  eventType: string;
  eventDate: string;
  description: string;
  createdAt: string;
};

export type InjuryPracticeReport = {
  reportDate: string;
  practiceStatus: string | null;
};

export type CurrentInjury = {
  playerName: string;

  playerProviderId: string;

  headshotUrl:
  string | null;

  fallbackHeadshotUrl:
  string | null;

  position:
  string | null;

  seasonYear:
  number | null;

  seasonType:
  string | null;

  weekNumber:
  number | null;

  injury:
  string | null;

  secondaryInjury:
  string | null;

  gameStatus:
  string | null;

  statusDate:
  string | null;

  estimatedReturnDate:
  string | null;

  practiceReports:
  InjuryPracticeReport[];

  updatedAt: string;
};

export type MlbLineupPlayer = {
  playerId: number | null;
  playerName: string;
  position: string | null;
  battingOrder: number | null;
};

export type MlbLineup = {
  state:
  | "NO_GAME"
  | "NOT_POSTED"
  | "POSTED";

  gamePk: number | null;
  gameDate: string | null;
  gameStatus: string | null;
  opponentName: string | null;
  homeAway: string | null;

  lineup: MlbLineupPlayer[];

  startingPitcher:
  MlbLineupPlayer | null;
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
    `${API_URL}/api/followed-teams`,
    {
      headers:
        await getAuthHeaders(),
    }
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

      headers:
        await getJsonAuthHeaders(),

      body: JSON.stringify({
        league,
        name,
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

      headers:
        await getAuthHeaders(),
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
    `${API_URL}/api/followed-teams/${teamId}/alert-preferences`,
    {
      headers:
        await getAuthHeaders(),
    }
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

      headers:
        await getJsonAuthHeaders(),

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
    `${API_URL}/api/events`,
    {
      headers:
        await getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw new Error(
      `Could not load alerts (${response.status})`
    );
  }

  return response.json();
}

export async function getCurrentInjuries(
  teamId: number
): Promise<CurrentInjury[]> {

  const response = await fetch(
    `${API_URL}/api/followed-teams/${teamId}/injuries`,
    {
      headers:
        await getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw new Error(
      `Could not load current injuries (${response.status})`
    );
  }

  return response.json();
}

export async function getMlbLineup(
  teamId: number
): Promise<MlbLineup> {

  const response = await fetch(
    `${API_URL}/api/followed-teams/${teamId}/lineup`,
    {
      headers:
        await getAuthHeaders(),
    }
  );

  if (!response.ok) {
    throw new Error(
      `Could not load MLB lineup (${response.status})`
    );
  }

  return response.json();
}

export async function registerPushToken(
  token: string
): Promise<void> {

  const response = await fetch(
    `${API_URL}/api/push-tokens`,
    {
      method: "POST",

      headers:
        await getJsonAuthHeaders(),

      body: JSON.stringify({
        token,
        platform: "ios",
      }),
    }
  );

  if (!response.ok) {
    throw new Error(
      `Could not register push token (${response.status})`
    );
  }
}