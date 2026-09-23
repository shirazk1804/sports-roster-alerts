import {
    useEffect,
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
    useLocalSearchParams,
    useRouter,
} from "expo-router";

import {
    CurrentInjury,
    getCurrentInjuries,
} from "../api";

export default function TeamInjuriesScreen() {
  const router = useRouter();

  const params = useLocalSearchParams<{
    id: string;
    name: string;
    emoji: string;
  }>();

  const teamId =
    Number(params.id);

  const teamName =
    params.name || "";

  const emoji =
    params.emoji || "🏈";

  const [injuries, setInjuries] =
    useState<CurrentInjury[]>([]);

  const [loading, setLoading] =
    useState(true);

  useEffect(() => {
    loadInjuries();
  }, []);

  async function loadInjuries() {
    try {
      setLoading(true);

      const currentInjuries =
        await getCurrentInjuries(
          teamId
        );

      setInjuries(
        currentInjuries
      );
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Could not load current injuries.";

      Alert.alert(
        "Unable to Load Injuries",
        message
      );
    } finally {
      setLoading(false);
    }
  }

  /*
   * Sportradar's status_date is the
   * provider's injury-status timestamp.
   *
   * Some provider timestamps use midnight
   * as a date marker. In that case we show
   * only the date rather than pretending
   * midnight is a meaningful update time.
   */
  function formatStatusDate(
    value: string | null
  ) {
    if (!value) {
      return "";
    }

    const dateOnlyMatch =
      value.match(
        /^(\d{4})-(\d{2})-(\d{2})/
      );

    if (!dateOnlyMatch) {
      return value;
    }

    const [
      ,
      year,
      month,
      day,
    ] = dateOnlyMatch;

    const hasMidnightTime =
      value.includes(
        "T00:00:00"
      );

    if (
      hasMidnightTime ||
      !value.includes("T")
    ) {
      const date =
        new Date(
          Number(year),
          Number(month) - 1,
          Number(day)
        );

      return date.toLocaleDateString(
        undefined,
        {
          month: "short",
          day: "numeric",
          year: "numeric",
        }
      );
    }

    const parsed =
      new Date(value);

    if (
      Number.isNaN(
        parsed.getTime()
      )
    ) {
      return value;
    }

    const dateText =
      parsed.toLocaleDateString(
        undefined,
        {
          month: "short",
          day: "numeric",
          year: "numeric",
        }
      );

    const timeText =
      parsed.toLocaleTimeString(
        undefined,
        {
          hour: "numeric",
          minute: "2-digit",
        }
      );

    return `${dateText} · ${timeText}`;
  }

  /*
   * updatedAt comes from our backend.
   *
   * This means when Sports Roster Alerts
   * last stored/checked this snapshot,
   * NOT when the team officially reported it.
   */
  function formatCheckedAt(
    value: string
  ) {
    if (!value) {
      return "";
    }

    const utcValue =
      value.endsWith("Z")
        ? value
        : `${value}Z`;

    const date =
      new Date(utcValue);

    if (
      Number.isNaN(
        date.getTime()
      )
    ) {
      return "";
    }

    const dateText =
      date.toLocaleDateString(
        undefined,
        {
          month: "short",
          day: "numeric",
        }
      );

    const timeText =
      date.toLocaleTimeString(
        undefined,
        {
          hour: "numeric",
          minute: "2-digit",
        }
      );

    return `${dateText} · ${timeText}`;
  }

  function detail(
    label: string,
    value: string | null
  ) {
    if (!value) {
      return null;
    }

    return (
      <Text
        style={styles.detail}
      >
        <Text
          style={styles.detailLabel}
        >
          {label}:{" "}
        </Text>

        {value}
      </Text>
    );
  }

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
          <Text
            style={styles.backText}
          >
            ‹ Back
          </Text>
        </Pressable>

        <View
          style={styles.teamHeader}
        >
          <Text
            style={styles.emoji}
          >
            {emoji}
          </Text>

          <View>
            <Text
              style={styles.league}
            >
              NFL
            </Text>

            <Text
              style={styles.teamName}
            >
              {teamName}
            </Text>
          </View>
        </View>

        <Text
          style={styles.title}
        >
          Current Injuries
        </Text>

        <Text
          style={styles.description}
        >
          Current injury and player
          availability information.
        </Text>

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
              style={styles.loadingText}
            >
              Loading injuries...
            </Text>
          </View>
        ) : injuries.length === 0 ? (
          <View
            style={styles.emptyCard}
          >
            <Text
              style={styles.emptyText}
            >
              No current injury report
              available yet.
            </Text>
          </View>
        ) : (
          injuries.map(
            (injury, index) => {
              const statusUpdated =
                formatStatusDate(
                  injury.statusDate
                );

              const lastChecked =
                formatCheckedAt(
                  injury.updatedAt
                );

              return (
                <View
                  key={`${injury.playerName}-${index}`}
                  style={
                    styles.injuryCard
                  }
                >
                  <Text
                    style={
                      styles.playerName
                    }
                  >
                    {injury.playerName}
                  </Text>

                  {detail(
                    "Injury",
                    injury.injury
                  )}

                  {detail(
                    "Secondary",
                    injury.secondaryInjury
                  )}

                  {detail(
                    "Game status",
                    injury.gameStatus
                  )}

                  {detail(
                    "Practice",
                    injury.practiceStatus
                  )}

                  {detail(
                    "Estimated return",
                    injury.estimatedReturnDate
                  )}

                  <View
                    style={
                      styles.timestampSection
                    }
                  >
                    {statusUpdated && (
                      <Text
                        style={
                          styles.timestamp
                        }
                      >
                        Status updated{" "}
                        {statusUpdated}
                      </Text>
                    )}

                    {lastChecked && (
                      <Text
                        style={
                          styles.checkedTimestamp
                        }
                      >
                        Last checked{" "}
                        {lastChecked}
                      </Text>
                    )}
                  </View>
                </View>
              );
            }
          )
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles =
  StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: "#F5F7FA",
    },

    content: {
      paddingHorizontal: 22,
      paddingTop: 20,
      paddingBottom: 40,
    },

    backButton: {
      marginBottom: 22,
    },

    backText: {
      fontSize: 17,
      fontWeight: "600",
      color: "#475569",
    },

    teamHeader: {
      flexDirection: "row",
      alignItems: "center",
      marginBottom: 30,
    },

    emoji: {
      fontSize: 42,
      marginRight: 15,
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

    title: {
      fontSize: 22,
      fontWeight: "800",
      color: "#0F172A",
    },

    description: {
      fontSize: 15,
      lineHeight: 21,
      color: "#64748B",
      marginTop: 6,
      marginBottom: 20,
    },

    loadingContainer: {
      alignItems: "center",
      paddingVertical: 40,
    },

    loadingText: {
      marginTop: 10,
      color: "#64748B",
    },

    injuryCard: {
      backgroundColor: "#FFFFFF",
      borderRadius: 18,
      borderWidth: 1,
      borderColor: "#E2E8F0",
      padding: 18,
      marginBottom: 12,
    },

    playerName: {
      fontSize: 18,
      fontWeight: "800",
      color: "#0F172A",
      marginBottom: 9,
    },

    detail: {
      fontSize: 15,
      lineHeight: 22,
      color: "#475569",
    },

    detailLabel: {
      fontWeight: "700",
      color: "#334155",
    },

    timestampSection: {
      marginTop: 12,
      paddingTop: 10,
      borderTopWidth: 1,
      borderTopColor: "#E2E8F0",
    },

    timestamp: {
      fontSize: 13,
      fontWeight: "600",
      color: "#64748B",
    },

    checkedTimestamp: {
      fontSize: 12,
      color: "#94A3B8",
      marginTop: 3,
    },

    emptyCard: {
      backgroundColor: "#FFFFFF",
      borderRadius: 18,
      borderWidth: 1,
      borderColor: "#E2E8F0",
      padding: 20,
    },

    emptyText: {
      color: "#64748B",
      fontSize: 15,
    },
  });