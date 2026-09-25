import {
  useCallback,
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
  useFocusEffect,
  useRouter,
} from "expo-router";

import {
  getRosterEvents,
  RosterEvent,
} from "../api";

import TeamLogo from "../components/TeamLogo";

function formatDetectedTime(
  createdAt: string
): string {
  const utcDate =
    new Date(
      createdAt.endsWith("Z")
        ? createdAt
        : `${createdAt}Z`
    );

  return utcDate.toLocaleTimeString(
    undefined,
    {
      hour: "numeric",
      minute: "2-digit",
    }
  );
}

function formatEventDate(
  eventDate: string
): string {
  const [year, month, day] =
    eventDate.split("-").map(Number);

  const date =
    new Date(
      year,
      month - 1,
      day
    );

  return date.toLocaleDateString(
    undefined,
    {
      month: "short",
      day: "numeric",
    }
  );
}

function formatEventDescription(
  event: RosterEvent
): string {
  if (
    event.eventType ===
    "INJURY_STATUS_CHANGE"
  ) {
    const prefix =
      `${event.playerName}: `;

    if (
      event.description.startsWith(
        prefix
      )
    ) {
      return event.description.substring(
        prefix.length
      );
    }
  }

  return event.description;
}

const eventLabels:
  Record<string, string> = {

  IL_PLACEMENT:
    "Placed on Injured List",

  IL_ACTIVATION:
    "Activated from Injured List",

  IL_TRANSFER:
    "Injured List Transfer",

  RECALLED:
    "Recalled",

  OPTIONED:
    "Optioned",

  DESIGNATED_FOR_ASSIGNMENT:
    "Designated for Assignment",

  TRADE:
    "Trade",

  CONTRACT_SELECTED:
    "Contract Selected",

  OUTRIGHTED:
    "Outrighted",

  REHAB_ASSIGNMENT:
    "Rehab Assignment",

  BEREAVEMENT_PLACEMENT:
    "Placed on Bereavement List",

  BEREAVEMENT_ACTIVATION:
    "Activated from Bereavement List",

  PATERNITY_PLACEMENT:
    "Placed on Paternity List",

  PATERNITY_ACTIVATION:
    "Activated from Paternity List",

  RESTRICTED_LIST_PLACEMENT:
    "Placed on Restricted List",

  RESTRICTED_LIST_ACTIVATION:
    "Activated from Restricted List",

  SUSPENDED:
    "Suspended",

  SUSPENSION_REINSTATED:
    "Reinstated from Suspension",

  WAIVER_CLAIM:
    "Claimed Off Waivers",

  WAIVERS:
    "Placed on Waivers",

  RELEASED:
    "Released",

  SIGNED:
    "Signed",

  MINOR_LEAGUE_SIGNING:
    "Minor League Signing",

  RETIRED:
    "Retired",

  ROSTER_ACTIVATION:
    "Activated",

  INJURED_RESERVE:
    "Injured Reserve",

  IR_DESIGNATED_RETURN:
    "Injured Reserve - Designated for Return",

  PUP_PLACEMENT:
    "Placed on PUP",

  NFI_PLACEMENT:
    "Placed on NFI",

  PRACTICE_SQUAD:
    "Practice Squad",

  ACTIVATED:
    "Activated",

  INJURY_STATUS_CHANGE:
    "Injury Status Update",

  WAIVED:
    "Waived",

  G_LEAGUE_ASSIGNMENT:
    "Assigned to G League",

  G_LEAGUE_RECALL:
    "Recalled from G League",

  INACTIVE:
    "Inactive",
};

export default function AlertsScreen() {
  const router =
    useRouter();

  const [events, setEvents] =
    useState<RosterEvent[]>([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  useFocusEffect(
    useCallback(() => {
      loadEvents();
    }, [])
  );

  async function loadEvents() {
    try {
      setLoading(true);
      setError("");

      const savedEvents =
        await getRosterEvents();

      const visibleEvents =
        savedEvents.filter(
          event =>
            event.eventType !==
            "STARTING_LINEUP_POSTED"
        );

      setEvents(
        visibleEvents
      );

    } catch (error) {

      const message =
        error instanceof Error
          ? error.message
          : "Could not load alerts.";

      setError(message);

    } finally {
      setLoading(false);
    }
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

        <Text
          style={styles.eyebrow}
        >
          SPORTS ROSTER ALERTS
        </Text>

        <Text
          style={styles.title}
        >
          Recent Alerts
        </Text>

        <Text
          style={styles.subtitle}
        >
          Recent roster and player
          availability changes.
        </Text>

        {loading && (
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
              Loading alerts...
            </Text>
          </View>
        )}

        {error !== "" && (
          <Text
            style={styles.error}
          >
            {error}
          </Text>
        )}

        {!loading &&
          error === "" &&
          events.length === 0 && (
            <Text
              style={styles.empty}
            >
              No alerts yet.
            </Text>
          )}

        {events.map(
          (event) => (
            <View
              key={event.id}
              style={
                styles.alertCard
              }
            >
              <View
                style={
                  styles.alertHeader
                }
              >
                <TeamLogo
                  league={
                    event.league
                  }
                  teamName={
                    event.teamName
                  }
                />

                <View
                  style={
                    styles.headerText
                  }
                >
                  <Text
                    style={
                      styles.team
                    }
                  >
                    {
                      event.teamName
                    }
                  </Text>

                  <Text
                    style={
                      styles.date
                    }
                  >
                    {formatEventDate(
                      event.eventDate
                    )}
                    {" · "}
                    {formatDetectedTime(
                      event.createdAt
                    )}
                  </Text>
                </View>
              </View>

              <Text
                style={
                  styles.eventType
                }
              >
                {
                  eventLabels[
                  event.eventType
                  ] ||
                  event.eventType
                    .replace(
                      /_/g,
                      " "
                    )
                }
              </Text>

              <Text
                style={
                  styles.playerName
                }
              >
                {event.playerName}
              </Text>

              <Text
                style={
                  styles.description
                }
              >
                {formatEventDescription(
                  event
                )}
              </Text>
            </View>
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
      backgroundColor:
        "#F5F7FA",
    },

    content: {
      paddingHorizontal: 22,
      paddingTop: 20,
      paddingBottom: 40,
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
      marginBottom: 24,
    },

    loadingContainer: {
      alignItems: "center",
      paddingVertical: 40,
    },

    loadingText: {
      marginTop: 10,
      fontSize: 14,
      color: "#64748B",
    },

    error: {
      color: "#DC2626",
      fontSize: 15,
    },

    empty: {
      fontSize: 15,
      color: "#64748B",
    },

    alertCard: {
      backgroundColor:
        "#FFFFFF",
      borderRadius: 18,
      borderWidth: 1,
      borderColor:
        "#E2E8F0",
      padding: 18,
      marginBottom: 14,
    },

    alertHeader: {
      flexDirection: "row",
      alignItems: "center",
      marginBottom: 14,
    },

    headerText: {
      flex: 1,
    },

    team: {
      fontSize: 15,
      fontWeight: "700",
      color: "#0F172A",
    },

    date: {
      fontSize: 12,
      color: "#64748B",
      marginTop: 2,
    },

    eventType: {
      fontSize: 12,
      fontWeight: "800",
      color: "#475569",
      textTransform:
        "uppercase",
      marginBottom: 6,
    },

    playerName: {
      fontSize: 19,
      fontWeight: "800",
      color: "#0F172A",
      marginBottom: 6,
    },

    description: {
      fontSize: 14,
      lineHeight: 21,
      color: "#475569",
    },
  });