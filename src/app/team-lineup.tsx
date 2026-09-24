import {
    useEffect,
    useState,
} from "react";

import {
    ActivityIndicator,
    Pressable,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text,
    View,
} from "react-native";

import {
    useLocalSearchParams,
    useRouter,
} from "expo-router";

import {
    getMlbLineup,
    MlbLineup,
} from "../api";

import TeamLogo from "../components/TeamLogo";

export default function TeamLineupScreen() {
  const router = useRouter();

  const params = useLocalSearchParams<{
    id: string;
    name: string;
  }>();

  const teamId =
    Number(params.id);

  const teamName =
    params.name || "";

  const [lineup, setLineup] =
    useState<MlbLineup | null>(
      null
    );

  const [loading, setLoading] =
    useState(true);

  const [refreshing, setRefreshing] =
    useState(false);

  const [error, setError] =
    useState("");

  useEffect(() => {
    loadLineup();
  }, []);

  async function loadLineup(
    showLoading = true
  ) {
    try {
      if (showLoading) {
        setLoading(true);
      }

      setError("");

      const currentLineup =
        await getMlbLineup(
          teamId
        );

      setLineup(
        currentLineup
      );

    } catch (loadError) {

      const message =
        loadError instanceof Error
          ? loadError.message
          : "Could not load today's lineup.";

      setError(message);

    } finally {

      if (showLoading) {
        setLoading(false);
      }
    }
  }

  async function refreshLineup() {
    try {
      setRefreshing(true);

      await loadLineup(
        false
      );

    } finally {
      setRefreshing(false);
    }
  }

  function formatGameTime(
    value: string | null
  ) {
    if (!value) {
      return "";
    }

    const date =
      new Date(value);

    if (
      Number.isNaN(
        date.getTime()
      )
    ) {
      return "";
    }

    return date.toLocaleTimeString(
      undefined,
      {
        hour: "numeric",
        minute: "2-digit",
      }
    );
  }

  const gameTime =
    formatGameTime(
      lineup?.gameDate ?? null
    );

  return (
    <SafeAreaView
      style={styles.container}
    >
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
          <Text style={styles.backText}>
            ‹ Back
          </Text>
        </Pressable>

        <View style={styles.teamHeader}>
          <View style={styles.logoWrapper}>
            <TeamLogo
              league="MLB"
              teamName={teamName}
              size={62}
            />
          </View>

          <View style={styles.teamHeaderText}>
            <Text style={styles.league}>
              MLB
            </Text>

            <Text style={styles.teamName}>
              {teamName}
            </Text>
          </View>
        </View>

        <View style={styles.titleRow}>
          <View style={styles.titleContainer}>
            <Text style={styles.title}>
              Today's Lineup
            </Text>

            <Text style={styles.description}>
              Batting order and starting
              pitcher for today's game.
            </Text>
          </View>

          <Pressable
            onPress={
              refreshLineup
            }
            disabled={refreshing}
            style={({ pressed }) => [
              styles.refreshButton,

              pressed &&
              styles.refreshButtonPressed,
            ]}
          >
            <Text
              style={
                styles.refreshText
              }
            >
              {refreshing
                ? "..."
                : "Refresh"}
            </Text>
          </Pressable>
        </View>

        {loading ? (
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
              Loading today's lineup...
            </Text>
          </View>

        ) : Boolean(error) ? (

          <View style={styles.messageCard}>
            <Text style={styles.messageTitle}>
              Unable to Load Lineup
            </Text>

            <Text style={styles.messageText}>
              {error}
            </Text>
          </View>

        ) : lineup?.state ===
          "NO_GAME" ? (

          <View style={styles.messageCard}>
            <Text style={styles.messageTitle}>
              No Game Today
            </Text>

            <Text style={styles.messageText}>
              {teamName} does not have a
              game scheduled today.
            </Text>
          </View>

        ) : (

          <>
            <View style={styles.gameCard}>
              <Text style={styles.gameLabel}>
                {lineup?.homeAway ===
                "HOME"
                  ? "VS"
                  : "AT"}
              </Text>

              <Text
                style={
                  styles.opponentName
                }
              >
                {lineup?.opponentName}
              </Text>

              {Boolean(gameTime) && (
                <Text
                  style={
                    styles.gameTime
                  }
                >
                  {gameTime}
                </Text>
              )}

              {Boolean(
                lineup?.gameStatus
              ) && (
                <Text
                  style={
                    styles.gameStatus
                  }
                >
                  {lineup?.gameStatus}
                </Text>
              )}
            </View>

            {lineup?.state ===
            "NOT_POSTED" ? (

              <View
                style={
                  styles.messageCard
                }
              >
                <Text
                  style={
                    styles.messageTitle
                  }
                >
                  Lineup Not Posted Yet
                </Text>

                <Text
                  style={
                    styles.messageText
                  }
                >
                  The official starting
                  lineup has not been
                  posted yet.
                </Text>
              </View>

            ) : (

              <View
                style={
                  styles.lineupCard
                }
              >
                <Text
                  style={
                    styles.sectionTitle
                  }
                >
                  Batting Order
                </Text>

                {lineup?.lineup.map(
                  player => (
                    <View
                      key={
                        player.playerId ??
                        `${player.playerName}-${player.battingOrder}`
                      }
                      style={
                        styles.playerRow
                      }
                    >
                      <View
                        style={
                          styles.orderCircle
                        }
                      >
                        <Text
                          style={
                            styles.orderText
                          }
                        >
                          {
                            player.battingOrder
                          }
                        </Text>
                      </View>

                      <Text
                        style={
                          styles.playerName
                        }
                      >
                        {
                          player.playerName
                        }
                      </Text>

                      <View
                        style={
                          styles.positionBadge
                        }
                      >
                        <Text
                          style={
                            styles.positionText
                          }
                        >
                          {
                            player.position ||
                            "—"
                          }
                        </Text>
                      </View>
                    </View>
                  )
                )}
              </View>
            )}

            {lineup?.startingPitcher && (
              <View
                style={
                  styles.pitcherCard
                }
              >
                <Text
                  style={
                    styles.pitcherLabel
                  }
                >
                  {lineup.state ===
                  "POSTED"
                    ? "Starting Pitcher"
                    : "Probable Pitcher"}
                </Text>

                <View
                  style={
                    styles.pitcherRow
                  }
                >
                  <Text
                    style={
                      styles.pitcherName
                    }
                  >
                    {
                      lineup
                        .startingPitcher
                        .playerName
                    }
                  </Text>

                  <View
                    style={
                      styles.positionBadge
                    }
                  >
                    <Text
                      style={
                        styles.positionText
                      }
                    >
                      SP
                    </Text>
                  </View>
                </View>
              </View>
            )}
          </>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F5F7FA",
  },

  content: {
    paddingHorizontal: 20,
    paddingTop: 18,
    paddingBottom: 40,
  },

  backButton: {
    marginBottom: 20,
  },

  backText: {
    fontSize: 17,
    fontWeight: "600",
    color: "#475569",
  },

  teamHeader: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 24,
  },

  logoWrapper: {
    marginRight: 14,
  },

  teamHeaderText: {
    flex: 1,
  },

  league: {
    fontSize: 13,
    fontWeight: "700",
    color: "#64748B",
    marginBottom: 3,
  },

  teamName: {
    fontSize: 25,
    fontWeight: "800",
    color: "#0F172A",
  },

  titleRow: {
    flexDirection: "row",
    alignItems: "flex-start",
    justifyContent:
      "space-between",
    marginBottom: 18,
  },

  titleContainer: {
    flex: 1,
    paddingRight: 12,
  },

  title: {
    fontSize: 24,
    fontWeight: "800",
    color: "#0F172A",
  },

  description: {
    fontSize: 14,
    lineHeight: 20,
    color: "#64748B",
    marginTop: 5,
  },

  refreshButton: {
    backgroundColor: "#E2E8F0",
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 10,
  },

  refreshButtonPressed: {
    opacity: 0.7,
  },

  refreshText: {
    fontSize: 12,
    fontWeight: "700",
    color: "#334155",
  },

  loadingContainer: {
    alignItems: "center",
    paddingVertical: 50,
  },

  loadingText: {
    marginTop: 10,
    color: "#64748B",
  },

  gameCard: {
    backgroundColor: "#FFFFFF",
    borderRadius: 16,
    borderWidth: 1,
    borderColor: "#E2E8F0",
    padding: 16,
    marginBottom: 12,
  },

  gameLabel: {
    fontSize: 11,
    fontWeight: "800",
    color: "#94A3B8",
    marginBottom: 4,
  },

  opponentName: {
    fontSize: 19,
    fontWeight: "800",
    color: "#0F172A",
  },

  gameTime: {
    fontSize: 14,
    color: "#475569",
    marginTop: 5,
  },

  gameStatus: {
    fontSize: 12,
    fontWeight: "700",
    color: "#64748B",
    marginTop: 4,
  },

  messageCard: {
    backgroundColor: "#FFFFFF",
    borderRadius: 16,
    borderWidth: 1,
    borderColor: "#E2E8F0",
    padding: 20,
    marginBottom: 12,
  },

  messageTitle: {
    fontSize: 17,
    fontWeight: "800",
    color: "#0F172A",
    marginBottom: 5,
  },

  messageText: {
    fontSize: 14,
    lineHeight: 20,
    color: "#64748B",
  },

  lineupCard: {
    backgroundColor: "#FFFFFF",
    borderRadius: 16,
    borderWidth: 1,
    borderColor: "#E2E8F0",
    padding: 16,
    marginBottom: 12,
  },

  sectionTitle: {
    fontSize: 16,
    fontWeight: "800",
    color: "#0F172A",
    marginBottom: 12,
  },

  playerRow: {
    minHeight: 52,
    flexDirection: "row",
    alignItems: "center",
    borderTopWidth: 1,
    borderTopColor: "#F1F5F9",
  },

  orderCircle: {
    width: 30,
    height: 30,
    borderRadius: 15,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#F1F5F9",
    marginRight: 12,
  },

  orderText: {
    fontSize: 13,
    fontWeight: "800",
    color: "#334155",
  },

  playerName: {
    flex: 1,
    fontSize: 15,
    fontWeight: "700",
    color: "#0F172A",
    paddingRight: 10,
  },

  positionBadge: {
    minWidth: 40,
    alignItems: "center",
    backgroundColor: "#F1F5F9",
    borderRadius: 8,
    paddingHorizontal: 8,
    paddingVertical: 5,
  },

  positionText: {
    fontSize: 12,
    fontWeight: "800",
    color: "#475569",
  },

  pitcherCard: {
    backgroundColor: "#FFFFFF",
    borderRadius: 16,
    borderWidth: 1,
    borderColor: "#E2E8F0",
    padding: 16,
  },

  pitcherLabel: {
    fontSize: 12,
    fontWeight: "800",
    color: "#64748B",
    marginBottom: 8,
  },

  pitcherRow: {
    flexDirection: "row",
    alignItems: "center",
  },

  pitcherName: {
    flex: 1,
    fontSize: 16,
    fontWeight: "800",
    color: "#0F172A",
  },
});