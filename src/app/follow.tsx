import {
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    ActivityIndicator,
    Alert,
    Pressable,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text,
    View,
} from "react-native";

import {
    useRouter,
} from "expo-router";

import {
    ApiTeam,
    followTeam,
    getTeams,
} from "../api";

const nflTeams = [
  "Arizona Cardinals",
  "Atlanta Falcons",
  "Baltimore Ravens",
  "Buffalo Bills",
  "Carolina Panthers",
  "Chicago Bears",
  "Cincinnati Bengals",
  "Cleveland Browns",
  "Dallas Cowboys",
  "Denver Broncos",
  "Detroit Lions",
  "Green Bay Packers",
  "Houston Texans",
  "Indianapolis Colts",
  "Jacksonville Jaguars",
  "Kansas City Chiefs",
  "Las Vegas Raiders",
  "Los Angeles Chargers",
  "Los Angeles Rams",
  "Miami Dolphins",
  "Minnesota Vikings",
  "New England Patriots",
  "New Orleans Saints",
  "New York Giants",
  "New York Jets",
  "Philadelphia Eagles",
  "Pittsburgh Steelers",
  "San Francisco 49ers",
  "Seattle Seahawks",
  "Tampa Bay Buccaneers",
  "Tennessee Titans",
  "Washington Commanders",
];

const nbaTeams = [
  "Atlanta Hawks",
  "Boston Celtics",
  "Brooklyn Nets",
  "Charlotte Hornets",
  "Chicago Bulls",
  "Cleveland Cavaliers",
  "Dallas Mavericks",
  "Denver Nuggets",
  "Detroit Pistons",
  "Golden State Warriors",
  "Houston Rockets",
  "Indiana Pacers",
  "LA Clippers",
  "Los Angeles Lakers",
  "Memphis Grizzlies",
  "Miami Heat",
  "Milwaukee Bucks",
  "Minnesota Timberwolves",
  "New Orleans Pelicans",
  "New York Knicks",
  "Oklahoma City Thunder",
  "Orlando Magic",
  "Philadelphia 76ers",
  "Phoenix Suns",
  "Portland Trail Blazers",
  "Sacramento Kings",
  "San Antonio Spurs",
  "Toronto Raptors",
  "Utah Jazz",
  "Washington Wizards",
];

type League =
  | "NFL"
  | "MLB"
  | "NBA";

export default function FollowScreen() {
  const router = useRouter();

  const [league, setLeague] =
    useState<League>("NFL");

  const [selectedTeam, setSelectedTeam] =
    useState("");

  const [backendTeams, setBackendTeams] =
    useState<ApiTeam[]>([]);

  const [loadingMlb, setLoadingMlb] =
    useState(true);

  useEffect(() => {
    loadTeams();
  }, []);

  async function loadTeams() {
    try {
      setLoadingMlb(true);

      const teams =
        await getTeams();

      setBackendTeams(teams);
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Could not load teams.";

      Alert.alert(
        "Unable to Load Teams",
        message
      );
    } finally {
      setLoadingMlb(false);
    }
  }

  const mlbTeams =
    useMemo(
      () =>
        backendTeams
          .filter(
            (team) =>
              team.league === "MLB"
          )
          .sort(
            (a, b) =>
              a.name.localeCompare(
                b.name
              )
          )
          .map(
            (team) => team.name
          ),
      [backendTeams]
    );

  const visibleTeams =
    league === "NFL"
      ? nflTeams
      : league === "MLB"
        ? mlbTeams
        : nbaTeams;

  function selectLeague(
    newLeague: League
  ) {
    setLeague(newLeague);
    setSelectedTeam("");
  }

  async function saveTeam() {
    if (!selectedTeam) {
      return;
    }

    try {
      await followTeam(
        league,
        selectedTeam
      );

      router.back();
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Could not follow team.";

      Alert.alert(
        "Unable to Follow Team",
        message
      );
    }
  }

  return (
    <SafeAreaView
      style={styles.container}
    >
      <View style={styles.screen}>
        <ScrollView
          contentContainerStyle={
            styles.content
          }
        >
          <Pressable
            onPress={() =>
              router.back()
            }
            style={styles.backButton}
          >
            <Text
              style={styles.backText}
            >
              ‹ Back
            </Text>
          </Pressable>

          <Text style={styles.eyebrow}>
            SPORTS ROSTER ALERTS
          </Text>

          <Text style={styles.title}>
            Follow Team
          </Text>

          <Text style={styles.subtitle}>
            Choose a league and team to
            receive roster and player
            availability alerts.
          </Text>

          <Text
            style={styles.sectionTitle}
          >
            League
          </Text>

          <View
            style={styles.leagueRow}
          >
            {(
              [
                "NFL",
                "MLB",
                "NBA",
              ] as League[]
            ).map(
              (leagueOption) => (
                <Pressable
                  key={leagueOption}
                  onPress={() =>
                    selectLeague(
                      leagueOption
                    )
                  }
                  style={[
                    styles.leagueButton,

                    league ===
                      leagueOption &&
                      styles.leagueButtonSelected,
                  ]}
                >
                  <Text
                    style={[
                      styles.leagueButtonText,

                      league ===
                        leagueOption &&
                        styles.leagueButtonTextSelected,
                    ]}
                  >
                    {leagueOption}
                  </Text>
                </Pressable>
              )
            )}
          </View>

          <Text
            style={styles.sectionTitle}
          >
            Team
          </Text>

          {league === "MLB" &&
          loadingMlb ? (
            <View
              style={
                styles.loadingContainer
              }
            >
              <ActivityIndicator
                size="large"
              />

              <Text
                style={
                  styles.loadingText
                }
              >
                Loading MLB teams...
              </Text>
            </View>
          ) : (
            <View
              style={styles.teamList}
            >
              {visibleTeams.map(
                (team) => (
                  <Pressable
                    key={team}
                    onPress={() =>
                      setSelectedTeam(
                        team
                      )
                    }
                    style={[
                      styles.teamButton,

                      selectedTeam ===
                        team &&
                        styles.teamButtonSelected,
                    ]}
                  >
                    <Text
                      style={[
                        styles.teamButtonText,

                        selectedTeam ===
                          team &&
                          styles.teamButtonTextSelected,
                      ]}
                    >
                      {team}
                    </Text>
                  </Pressable>
                )
              )}
            </View>
          )}
        </ScrollView>

        {selectedTeam !== "" && (
          <View
            style={styles.bottomAction}
          >
            <Pressable
              onPress={saveTeam}
              style={({ pressed }) => [
                styles.followButton,

                pressed &&
                  styles.followButtonPressed,
              ]}
            >
              <Text
                style={
                  styles.followButtonText
                }
              >
                Follow {selectedTeam}
              </Text>
            </Pressable>
          </View>
        )}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F5F7FA",
  },

  screen: {
    flex: 1,
  },

  content: {
    paddingHorizontal: 22,
    paddingTop: 20,
    paddingBottom: 120,
  },

  backButton: {
    marginBottom: 24,
  },

  backText: {
    fontSize: 17,
    fontWeight: "600",
    color: "#475569",
  },

  eyebrow: {
    fontSize: 12,
    fontWeight: "700",
    letterSpacing: 1.5,
    color: "#64748B",
    marginBottom: 8,
  },

  title: {
    fontSize: 32,
    fontWeight: "800",
    color: "#0F172A",
  },

  subtitle: {
    fontSize: 15,
    lineHeight: 22,
    color: "#64748B",
    marginTop: 7,
    marginBottom: 28,
  },

  sectionTitle: {
    fontSize: 18,
    fontWeight: "800",
    color: "#0F172A",
    marginBottom: 12,
  },

  leagueRow: {
    flexDirection: "row",
    gap: 10,
    marginBottom: 28,
  },

  leagueButton: {
    flex: 1,
    paddingVertical: 13,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: "#CBD5E1",
    alignItems: "center",
    backgroundColor: "#FFFFFF",
  },

  leagueButtonSelected: {
    backgroundColor: "#0F172A",
    borderColor: "#0F172A",
  },

  leagueButtonText: {
    fontSize: 15,
    fontWeight: "700",
    color: "#475569",
  },

  leagueButtonTextSelected: {
    color: "#FFFFFF",
  },

  teamList: {
    gap: 10,
  },

  teamButton: {
    backgroundColor: "#FFFFFF",
    borderRadius: 14,
    paddingVertical: 15,
    paddingHorizontal: 17,
    borderWidth: 1,
    borderColor: "#E2E8F0",
  },

  teamButtonSelected: {
    backgroundColor: "#E2E8F0",
    borderColor: "#0F172A",
  },

  teamButtonText: {
    fontSize: 15,
    fontWeight: "600",
    color: "#334155",
  },

  teamButtonTextSelected: {
    fontWeight: "800",
    color: "#0F172A",
  },

  loadingContainer: {
    alignItems: "center",
    paddingVertical: 30,
  },

  loadingText: {
    marginTop: 10,
    fontSize: 14,
    color: "#64748B",
  },

  bottomAction: {
    backgroundColor: "#F5F7FA",
    paddingHorizontal: 22,
    paddingTop: 12,
    paddingBottom: 12,
    borderTopWidth: 1,
    borderTopColor: "#E2E8F0",
  },

  followButton: {
    backgroundColor: "#0F172A",
    paddingVertical: 17,
    borderRadius: 16,
    alignItems: "center",
  },

  followButtonPressed: {
    opacity: 0.8,
  },

  followButtonText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "700",
  },
});