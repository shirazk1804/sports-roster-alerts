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
  Switch,
  Text,
  View,
} from "react-native";

import {
  useLocalSearchParams,
  useRouter,
} from "expo-router";

import {
  AlertPreferences,
  deleteFollowedTeam,
  getAlertPreferences,
  saveAlertPreferences,
} from "../api";

const leagueSettings = {
  NFL: [
    "Injury status changes",
    "IR / PUP moves",
    "Activations",
    "Signings and releases",
    "Trades",
    "Practice squad moves",
    "Suspensions",
  ],

  MLB: [
    "Injury status changes",
    "IL placements",
    "IL activations",
    "Call-ups",
    "Options to minors",
    "Designated for assignment",
    "Trades",
    "Signings and releases",
  ],

  NBA: [
    "Injury status changes",
    "Inactive / Out",
    "G League assignments",
    "G League recalls",
    "Signings and waives",
    "Trades",
    "Suspensions",
  ],
};

export default function TeamSettingsScreen() {
  const router = useRouter();

  const params = useLocalSearchParams<{
    id: string;
    league: string;
    name: string;
    emoji: string;
  }>();

  const teamId = Number(params.id);
  const league = params.league || "";
  const teamName = params.name || "";
  const emoji = params.emoji || "";

  const availableSettings =
    leagueSettings[
      league as keyof typeof leagueSettings
    ] || [];

  const [settings, setSettings] =
    useState<AlertPreferences>({});

  const [loading, setLoading] =
    useState(true);

  const [saving, setSaving] =
    useState(false);

  useEffect(() => {
    loadSettings();
  }, []);

  async function loadSettings() {
    try {
      setLoading(true);

      const savedSettings =
        await getAlertPreferences(teamId);

      if (
        Object.keys(savedSettings).length > 0
      ) {
        setSettings(savedSettings);
        return;
      }

      const defaultSettings:
        AlertPreferences = {};

      availableSettings.forEach(
        (setting) => {
          defaultSettings[setting] = true;
        }
      );

      setSettings(defaultSettings);
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Could not load alert settings.";

      Alert.alert(
        "Unable to Load Settings",
        message
      );
    } finally {
      setLoading(false);
    }
  }

  function toggleSetting(
    setting: string
  ) {
    setSettings(
      (currentSettings) => ({
        ...currentSettings,
        [setting]:
          !currentSettings[setting],
      })
    );
  }

  function openCurrentInjuries() {
    router.push({
      pathname: "/team-injuries",

      params: {
        id: String(teamId),
        name: teamName,
        emoji,
      },
    });
  }

  async function saveSettings() {
    try {
      setSaving(true);

      await saveAlertPreferences(
        teamId,
        settings
      );

      router.back();
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Could not save alert settings.";

      Alert.alert(
        "Unable to Save Settings",
        message
      );
    } finally {
      setSaving(false);
    }
  }

  function confirmUnfollow() {
    Alert.alert(
      "Unfollow Team",
      `Stop following ${teamName}?`,
      [
        {
          text: "Cancel",
          style: "cancel",
        },
        {
          text: "Unfollow",
          style: "destructive",
          onPress: unfollowTeam,
        },
      ]
    );
  }

  async function unfollowTeam() {
    try {
      await deleteFollowedTeam(
        teamId
      );

      router.back();
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Could not unfollow team.";

      Alert.alert(
        "Unable to Unfollow Team",
        message
      );
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
          <Text style={styles.backText}>
            ‹ Back
          </Text>
        </Pressable>

        <View style={styles.teamHeader}>
          <Text style={styles.emoji}>
            {emoji}
          </Text>

          <View>
            <Text style={styles.league}>
              {league}
            </Text>

            <Text
              style={styles.teamName}
            >
              {teamName}
            </Text>
          </View>
        </View>

        {league === "NFL" && (
          <Pressable
            onPress={
              openCurrentInjuries
            }
            style={({ pressed }) => [
              styles.injuriesButton,

              pressed &&
                styles.injuriesButtonPressed,
            ]}
          >
            <View>
              <Text
                style={
                  styles.injuriesButtonTitle
                }
              >
                Current Injuries
              </Text>

              <Text
                style={
                  styles.injuriesButtonSubtitle
                }
              >
                View player injury and
                availability information
              </Text>
            </View>

            <Text
              style={
                styles.injuriesChevron
              }
            >
              ›
            </Text>
          </Pressable>
        )}

        <Text
          style={styles.sectionTitle}
        >
          Notify Me About
        </Text>

        <Text
          style={styles.description}
        >
          Choose which roster and player
          availability updates you want to
          receive.
        </Text>

        {loading ? (
          <View
            style={styles.loadingContainer}
          >
            <ActivityIndicator
              size="large"
            />

            <Text
              style={styles.loadingText}
            >
              Loading settings...
            </Text>
          </View>
        ) : (
          <View
            style={styles.settingsCard}
          >
            {availableSettings.map(
              (setting, index) => (
                <View
                  key={setting}
                  style={[
                    styles.settingRow,

                    index !==
                      availableSettings.length -
                        1 &&
                      styles.settingBorder,
                  ]}
                >
                  <Text
                    style={
                      styles.settingText
                    }
                  >
                    {setting}
                  </Text>

                  <Switch
                    value={
                      settings[
                        setting
                      ] ?? true
                    }
                    onValueChange={() =>
                      toggleSetting(
                        setting
                      )
                    }
                  />
                </View>
              )
            )}
          </View>
        )}

        <Pressable
          onPress={confirmUnfollow}
          style={
            styles.unfollowButton
          }
        >
          <Text
            style={styles.unfollowText}
          >
            Unfollow {teamName}
          </Text>
        </Pressable>
      </ScrollView>

      <View
        style={styles.bottomAction}
      >
        <Pressable
          onPress={saveSettings}
          disabled={
            loading || saving
          }
          style={({ pressed }) => [
            styles.saveButton,

            pressed &&
              styles.saveButtonPressed,

            (loading || saving) &&
              styles.saveButtonDisabled,
          ]}
        >
          <Text
            style={styles.saveButtonText}
          >
            {saving
              ? "Saving..."
              : "Save Alert Settings"}
          </Text>
        </Pressable>
      </View>
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
    paddingTop: 20,
    paddingBottom: 30,
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
    marginBottom: 26,
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

  injuriesButton: {
    minHeight: 82,
    backgroundColor: "#FFFFFF",
    borderRadius: 18,
    borderWidth: 1,
    borderColor: "#E2E8F0",
    paddingHorizontal: 18,
    paddingVertical: 16,
    marginBottom: 30,
    flexDirection: "row",
    alignItems: "center",
    justifyContent:
      "space-between",
  },

  injuriesButtonPressed: {
    opacity: 0.7,
  },

  injuriesButtonTitle: {
    fontSize: 16,
    fontWeight: "800",
    color: "#0F172A",
    marginBottom: 4,
  },

  injuriesButtonSubtitle: {
    fontSize: 13,
    color: "#64748B",
    maxWidth: 260,
  },

  injuriesChevron: {
    fontSize: 30,
    color: "#94A3B8",
    marginLeft: 15,
  },

  sectionTitle: {
    fontSize: 20,
    fontWeight: "800",
    color: "#0F172A",
  },

  description: {
    fontSize: 15,
    lineHeight: 21,
    color: "#64748B",
    marginTop: 6,
    marginBottom: 18,
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

  settingsCard: {
    backgroundColor: "#FFFFFF",
    borderRadius: 18,
    borderWidth: 1,
    borderColor: "#E2E8F0",
    paddingHorizontal: 17,
  },

  settingRow: {
    minHeight: 58,
    flexDirection: "row",
    alignItems: "center",
  },

  settingBorder: {
    borderBottomWidth: 1,
    borderBottomColor: "#E2E8F0",
  },

  settingText: {
    flex: 1,
    fontSize: 15,
    fontWeight: "600",
    color: "#334155",
    paddingRight: 15,
  },

  unfollowButton: {
    marginTop: 28,
    paddingVertical: 16,
    alignItems: "center",
  },

  unfollowText: {
    fontSize: 15,
    fontWeight: "700",
    color: "#DC2626",
  },

  bottomAction: {
    backgroundColor: "#F5F7FA",
    paddingHorizontal: 22,
    paddingTop: 12,
    paddingBottom: 12,
    borderTopWidth: 1,
    borderTopColor: "#E2E8F0",
  },

  saveButton: {
    backgroundColor: "#0F172A",
    paddingVertical: 17,
    borderRadius: 16,
    alignItems: "center",
  },

  saveButtonPressed: {
    opacity: 0.8,
  },

  saveButtonDisabled: {
    opacity: 0.5,
  },

  saveButtonText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "700",
  },
});