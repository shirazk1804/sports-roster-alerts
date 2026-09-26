import {
  useCallback,
  useState,
} from "react";

import {
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";

import {
  useFocusEffect,
  useRouter,
} from "expo-router";

import {
  FollowedTeam,
  getFollowedTeams,
} from "../api";

import TeamLogo from "../components/TeamLogo";

export default function HomeScreen() {
  const router = useRouter();

  const [teams, setTeams] =
    useState<FollowedTeam[]>([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  async function loadFollowedTeams() {
    try {
      setLoading(true);
      setError("");

      const backendTeams =
        await getFollowedTeams();

      setTeams(backendTeams);
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Could not load followed teams.";

      setError(message);
    } finally {
      setLoading(false);
    }
  }

  useFocusEffect(
    useCallback(() => {
      loadFollowedTeams();
    }, [])
  );

  function openTeam(team: FollowedTeam) {
    router.push({
      pathname: "/team-settings",

      params: {
        id: String(team.id),
        league: team.league,
        name: team.name,
      },
    });
  }

  function followTeam() {
    router.push("/follow");
  }

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView
        contentContainerStyle={styles.content}
      >
        <Text style={styles.eyebrow}>
          SPORTS ROSTER ALERTS
        </Text>

        <Text style={styles.title}>
          Following
        </Text>

        <Text style={styles.subtitle}>
          Get notified when player availability
          or roster status changes.
        </Text>

        <Pressable
          onPress={() =>
            router.push("/alerts")
          }
          style={styles.alertsButton}
        >
          <Text
            style={styles.alertsButtonText}
          >
            View Recent Alerts
          </Text>
        </Pressable>

        {loading && (
          <Text style={styles.message}>
            Loading teams...
          </Text>
        )}

        {error !== "" && (
          <Text style={styles.error}>
            {error}
          </Text>
        )}

        {!loading &&
          error === "" &&
          teams.length === 0 && (
            <Text style={styles.message}>
              You aren't following any teams yet.
            </Text>
          )}

        {teams.map((team) => (
          <Pressable
            key={team.id}
            onPress={() => openTeam(team)}
            style={({ pressed }) => [
              styles.teamCard,
              pressed &&
              styles.teamCardPressed,
            ]}
          >
            <View style={styles.teamHeader}>
              <View style={styles.logoWrapper}>
                <TeamLogo
                  league={team.league}
                  teamName={team.name}
                  size={52}
                />
              </View>

              <View style={styles.teamText}>
                <Text style={styles.league}>
                  {team.league}
                </Text>

                <Text style={styles.teamName}>
                  {team.name}
                </Text>
              </View>

              <Text style={styles.arrow}>
                ›
              </Text>
            </View>

          </Pressable>
        ))}

      </ScrollView>
      <Pressable
        onPress={() =>
          router.push("/follow")
        }
        style={({ pressed }) => [
          styles.followButton,
          pressed &&
          styles.followButtonPressed,
        ]}
      >
        <Text style={styles.followButtonText}>
          + Follow Team
        </Text>
      </Pressable>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F5F7FA",
  },

  content: {
    paddingHorizontal: 22,
    paddingTop: 40,
    paddingBottom: 100,
  },

  eyebrow: {
    fontSize: 12,
    fontWeight: "700",
    letterSpacing: 1.5,
    color: "#64748B",
    marginBottom: 8,
  },

  title: {
    fontSize: 34,
    fontWeight: "800",
    color: "#0F172A",
  },

  subtitle: {
    fontSize: 16,
    lineHeight: 23,
    color: "#64748B",
    marginTop: 8,
    marginBottom: 20,
  },

  alertsButton: {
    backgroundColor: "#E2E8F0",
    paddingVertical: 13,
    borderRadius: 14,
    alignItems: "center",
    marginBottom: 22,
  },

  alertsButtonText: {
    fontSize: 15,
    fontWeight: "700",
    color: "#0F172A",
  },

  message: {
    fontSize: 15,
    color: "#64748B",
    marginBottom: 18,
  },

  error: {
    fontSize: 15,
    color: "#DC2626",
    marginBottom: 18,
  },

  teamCard: {
    backgroundColor: "#FFFFFF",
    borderRadius: 18,
    padding: 18,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: "#E2E8F0",
  },

  teamCardPressed: {
    opacity: 0.7,
    transform: [
      {
        scale: 0.99,
      },
    ],
  },

  teamHeader: {
    flexDirection: "row",
    alignItems: "center",
  },

  teamText: {
    flex: 1,
  },

  logoWrapper: {
    marginRight: 14,
  },

  league: {
    fontSize: 12,
    fontWeight: "700",
    color: "#64748B",
    marginBottom: 3,
  },

  teamName: {
    fontSize: 18,
    fontWeight: "700",
    color: "#0F172A",
  },

  arrow: {
    fontSize: 32,
    color: "#94A3B8",
  },

  followButton: {
    position: "absolute",
    left: 20,
    right: 20,
    bottom: 20,
    backgroundColor: "#0F172A",
    paddingVertical: 15,
    borderRadius: 14,
    alignItems: "center",
    justifyContent: "center",

    shadowColor: "#000",
    shadowOffset: {
      width: 0,
      height: 3,
    },
    shadowOpacity: 0.18,
    shadowRadius: 6,

    elevation: 6,

    zIndex: 10,
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