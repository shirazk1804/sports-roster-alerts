import {
    useEffect,
    useState,
} from "react";

import {
    ActivityIndicator,
    Alert,
    Image,
    Pressable,
    RefreshControl,
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
    getNbaCurrentInjuries,
} from "../api";

import TeamLogo from "../components/TeamLogo";

function PlayerHeadshot({
    name,
    url,
    fallbackUrl,
}: {
    name: string;
    url: string | null;
    fallbackUrl: string | null;
}) {
    const [activeUrl, setActiveUrl] =
        useState<string | null>(
            url ?? fallbackUrl ?? null
        );

    useEffect(() => {
        setActiveUrl(
            url ?? fallbackUrl ?? null
        );
    }, [url, fallbackUrl]);

    const initials =
        name
            .split(" ")
            .filter(Boolean)
            .map(part => part[0])
            .slice(0, 2)
            .join("")
            .toUpperCase();

    if (!activeUrl) {
        return (
            <View style={styles.headshotFallback}>
                <Text
                    style={
                        styles.headshotFallbackText
                    }
                >
                    {initials || "NFL"}
                </Text>
            </View>
        );
    }

    return (
        <View style={styles.headshotContainer}>
            <Image
                source={{ uri: activeUrl }}
                style={styles.headshot}
                resizeMode="cover"
                onError={() => {
                    if (
                        fallbackUrl &&
                        fallbackUrl !== activeUrl
                    ) {
                        setActiveUrl(
                            fallbackUrl
                        );
                    } else {
                        setActiveUrl(null);
                    }
                }}
            />
        </View>
    );
}

export default function TeamInjuriesScreen() {
    const router = useRouter();

    const params = useLocalSearchParams<{
        id: string;
        name: string;
        league?: string;
    }>();

    const teamId =
        Number(params.id);

    const teamName =
        params.name || "";

    const league =
        (params.league || "NFL")
            .toUpperCase();

    const [injuries, setInjuries] =
        useState<CurrentInjury[]>([]);

    const [loading, setLoading] =
        useState(true);

    const [refreshing, setRefreshing] =
        useState(false);

    useEffect(() => {
        loadInjuries();
    }, []);

    async function loadInjuries(
        showLoading = true
    ) {
        try {
            if (showLoading) {
                setLoading(true);
            }

            if (league === "NBA") {

                const nbaInjuries =
                    await getNbaCurrentInjuries(
                        teamId
                    );

                /*
                 * Normalize NBA injuries into the
                 * same shape the existing injury
                 * screen already understands.
                 */
                const normalizedInjuries:
                    CurrentInjury[] =
                    nbaInjuries.map(
                        injury => ({
                            playerName:
                                injury.playerName,

                            playerProviderId:
                                injury.playerProviderId,

                            headshotUrl: null,

                            fallbackHeadshotUrl: null,

                            position:
                                injury.position,

                            seasonYear: null,

                            seasonType: null,

                            weekNumber: null,

                            injury:
                                injury.injury,

                            secondaryInjury: null,

                            gameStatus:
                                injury.status,

                            statusDate:
                                injury.updateDate ??
                                injury.startDate,

                            estimatedReturnDate: null,

                            practiceReports: [],

                            comment:
                                injury.comment,

                            updatedAt:
                                injury.updatedAt,
                        })
                    );

                setInjuries(
                    normalizedInjuries
                );

            } else {

                const currentInjuries =
                    await getCurrentInjuries(
                        teamId
                    );

                setInjuries(
                    currentInjuries
                );
            }
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
            if (showLoading) {
                setLoading(false);
            }
        }
    }

    async function refreshInjuries() {
        try {
            setRefreshing(true);

            await loadInjuries(
                false
            );
        } finally {
            setRefreshing(false);
        }
    }

    function getPracticeDates() {
        const dates =
            new Set<string>();

        injuries.forEach(
            injury => {
                (
                    injury.practiceReports ??
                    []
                ).forEach(
                    report => {
                        if (report.reportDate) {
                            dates.add(
                                report.reportDate
                            );
                        }
                    }
                );
            }
        );

        return Array.from(
            dates
        ).sort();
    }

    function getPracticeStatus(
        injury: CurrentInjury,
        reportDate: string
    ) {
        const report =
            (
                injury.practiceReports ??
                []
            ).find(
                item =>
                    item.reportDate ===
                    reportDate
            );

        return formatPracticeStatus(
            report?.practiceStatus ??
            null
        );
    }

    function formatPracticeStatus(
        value: string | null
    ) {
        if (!value) {
            return "—";
        }

        const normalized =
            value
                .trim()
                .toLowerCase();

        if (
            normalized.includes(
                "did not participate"
            ) ||
            normalized === "dnp"
        ) {
            return "DNP";
        }

        if (
            normalized.includes(
                "limited"
            ) ||
            normalized === "lp"
        ) {
            return "LP";
        }

        if (
            normalized.includes(
                "full"
            ) ||
            normalized === "fp"
        ) {
            return "FP";
        }

        return value;
    }

    function formatDay(
        value: string
    ) {
        const date =
            new Date(
                `${value}T12:00:00`
            );

        if (
            Number.isNaN(
                date.getTime()
            )
        ) {
            return value;
        }

        return date
            .toLocaleDateString(
                undefined,
                {
                    weekday: "short",
                }
            )
            .toUpperCase();
    }

    function formatShortDate(
        value: string
    ) {
        const date =
            new Date(
                `${value}T12:00:00`
            );

        if (
            Number.isNaN(
                date.getTime()
            )
        ) {
            return "";
        }

        return date
            .toLocaleDateString(
                undefined,
                {
                    month: "numeric",
                    day: "numeric",
                }
            );
    }

    function formatLastChecked(
        value: string | null
    ) {
        if (!value) {
            return "";
        }

        const utcValue =
            value.endsWith("Z")
                ? value
                : `${value}Z`;

        const date =
            new Date(
                utcValue
            );

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

    function getLatestChecked() {
        let latestTimestamp:
            number | null = null;

        injuries.forEach(
            injury => {
                if (!injury.updatedAt) {
                    return;
                }

                const utcValue =
                    injury.updatedAt.endsWith(
                        "Z"
                    )
                        ? injury.updatedAt
                        : `${injury.updatedAt}Z`;

                const timestamp =
                    new Date(
                        utcValue
                    ).getTime();

                if (
                    Number.isNaN(
                        timestamp
                    )
                ) {
                    return;
                }

                if (
                    latestTimestamp === null ||
                    timestamp >
                    latestTimestamp
                ) {
                    latestTimestamp =
                        timestamp;
                }
            }
        );

        if (
            latestTimestamp === null
        ) {
            return "";
        }

        return formatLastChecked(
            new Date(
                latestTimestamp
            ).toISOString()
        );
    }

    function getInjuryDescription(
        injury: CurrentInjury
    ) {
        const parts =
            [
                injury.injury,
                injury.secondaryInjury,
            ].filter(
                value =>
                    value &&
                    value.trim()
            );

        if (
            parts.length === 0
        ) {
            return "Injury not specified";
        }

        return parts.join(
            " / "
        );
    }

    const practiceDates =
        getPracticeDates();

    const weekNumber =
        injuries.length > 0
            ? injuries[0].weekNumber
            : null;

    const latestChecked =
        getLatestChecked();

    return (
        <SafeAreaView
            style={styles.container}
        >
            <ScrollView
                contentContainerStyle={
                    styles.content
                }
                refreshControl={
                    <RefreshControl
                        refreshing={
                            refreshing
                        }
                        onRefresh={
                            refreshInjuries
                        }
                    />
                }
            >
                <Pressable
                    onPress={() =>
                        router.back()
                    }
                    style={
                        styles.backButton
                    }
                >
                    <Text
                        style={
                            styles.backText
                        }
                    >
                        ‹ Back
                    </Text>
                </Pressable>

                <View style={styles.teamHeader}>
                    <View style={styles.logoWrapper}>
                        <TeamLogo
                            league={league}
                            teamName={teamName}
                            size={62}
                        />
                    </View>

                    <View style={styles.teamHeaderText}>
                        <Text style={styles.league}>
                            {league}
                        </Text>

                        <Text style={styles.teamName}>
                            {teamName}
                        </Text>
                    </View>
                </View>

                <View
                    style={
                        styles.titleRow
                    }
                >
                    <View
                        style={
                            styles.titleContainer
                        }
                    >
                        <Text
                            style={styles.title}
                        >
                            Injury Report
                        </Text>

                        <Text
                            style={
                                styles.description
                            }
                        >
                            {league === "NBA"
                                ? "Current player injury status and details."
                                : "Practice participation and game availability."}
                        </Text>
                    </View>

                    {league === "NFL" &&
                        Boolean(weekNumber) && (
                            <View
                                style={
                                    styles.weekBadge
                                }
                            >
                                <Text
                                    style={
                                        styles.weekBadgeText
                                    }
                                >
                                    WEEK {weekNumber}
                                </Text>
                            </View>
                        )}
                </View>

                {Boolean(latestChecked) && (
                    <Text
                        style={
                            styles.lastUpdated
                        }
                    >
                        Last checked{" "}
                        {latestChecked}
                    </Text>
                )}

                {league === "NFL" &&
                    practiceDates.length > 0 && (
                        <View
                            style={
                                styles.legend
                            }
                        >
                            <Text
                                style={
                                    styles.legendText
                                }
                            >
                                DNP = Did Not Participate
                                {"  "}•{"  "}
                                LP = Limited
                                {"  "}•{"  "}
                                FP = Full
                            </Text>
                        </View>
                    )}

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
                            Loading injuries...
                        </Text>
                    </View>
                ) : injuries.length === 0 ? (
                    <View
                        style={
                            styles.emptyCard
                        }
                    >
                        <Text
                            style={
                                styles.emptyTitle
                            }
                        >
                            No injury report yet
                        </Text>

                        <Text
                            style={
                                styles.emptyText
                            }
                        >
                            {league === "NBA"
                                ? "No current injuries are available for this team."
                                : "The current week's injury report has not been published yet."}
                        </Text>
                    </View>
                ) : (
                    injuries.map(
                        (
                            injury,
                            index
                        ) => (
                            <View
                                key={
                                    `${injury.playerName}-${index}`
                                }
                                style={
                                    styles.injuryCard
                                }
                            >
                                <View style={styles.playerHeader}>
                                    <PlayerHeadshot
                                        name={injury.playerName}
                                        url={injury.headshotUrl}
                                        fallbackUrl={
                                            injury.fallbackHeadshotUrl
                                        }
                                    />

                                    <View style={styles.playerInfo}>
                                        <Text style={styles.playerName}>
                                            {injury.playerName}
                                        </Text>

                                        <Text style={styles.injuryText}>
                                            {getInjuryDescription(
                                                injury
                                            )}
                                        </Text>
                                    </View>

                                    {Boolean(injury.position) && (
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
                                                {injury.position}
                                            </Text>
                                        </View>
                                    )}
                                </View>

                                <View
                                    style={
                                        styles.statusDivider
                                    }
                                />

                                {league === "NBA" ? (

                                    <View
                                        style={
                                            styles.nbaStatusSection
                                        }
                                    >
                                        <View
                                            style={
                                                styles.nbaStatusRow
                                            }
                                        >
                                            <Text
                                                style={
                                                    styles.statusLabel
                                                }
                                            >
                                                STATUS
                                            </Text>

                                            <View
                                                style={
                                                    styles.gameStatusBox
                                                }
                                            >
                                                <Text
                                                    style={
                                                        styles.gameStatusText
                                                    }
                                                >
                                                    {injury.gameStatus ||
                                                        "—"}
                                                </Text>
                                            </View>
                                        </View>

                                        {Boolean(injury.comment) && (
                                            <Text
                                                style={
                                                    styles.nbaComment
                                                }
                                            >
                                                {injury.comment}
                                            </Text>
                                        )}

                                        {Boolean(injury.statusDate) && (
                                            <Text
                                                style={
                                                    styles.returnText
                                                }
                                            >
                                                Updated:{" "}
                                                {injury.statusDate}
                                            </Text>
                                        )}
                                    </View>

                                ) : (

                                    <ScrollView
                                        horizontal
                                        showsHorizontalScrollIndicator={
                                            false
                                        }
                                        contentContainerStyle={
                                            styles.statusRow
                                        }
                                    >
                                        {practiceDates.map(
                                            reportDate => (
                                                <View
                                                    key={
                                                        reportDate
                                                    }
                                                    style={
                                                        styles.statusColumn
                                                    }
                                                >
                                                    <Text
                                                        style={
                                                            styles.statusLabel
                                                        }
                                                    >
                                                        {
                                                            formatDay(
                                                                reportDate
                                                            )
                                                        }
                                                    </Text>

                                                    <Text
                                                        style={
                                                            styles.statusDate
                                                        }
                                                    >
                                                        {
                                                            formatShortDate(
                                                                reportDate
                                                            )
                                                        }
                                                    </Text>

                                                    <View
                                                        style={
                                                            styles.statusValueBox
                                                        }
                                                    >
                                                        <Text
                                                            style={
                                                                styles.statusValue
                                                            }
                                                        >
                                                            {
                                                                getPracticeStatus(
                                                                    injury,
                                                                    reportDate
                                                                )
                                                            }
                                                        </Text>
                                                    </View>
                                                </View>
                                            )
                                        )}

                                        <View
                                            style={
                                                styles.gameColumn
                                            }
                                        >
                                            <Text
                                                style={
                                                    styles.statusLabel
                                                }
                                            >
                                                GAME
                                            </Text>

                                            <Text
                                                style={
                                                    styles.statusDate
                                                }
                                            >
                                                STATUS
                                            </Text>

                                            <View
                                                style={
                                                    styles.gameStatusBox
                                                }
                                            >
                                                <Text
                                                    style={
                                                        styles.gameStatusText
                                                    }
                                                >
                                                    {
                                                        injury.gameStatus ||
                                                        "—"
                                                    }
                                                </Text>
                                            </View>
                                        </View>
                                    </ScrollView>

                                )}

                                {Boolean(injury.estimatedReturnDate) && (
                                    <Text
                                        style={
                                            styles.returnText
                                        }
                                    >
                                        Estimated return:{" "}
                                        {
                                            injury.estimatedReturnDate
                                        }
                                    </Text>
                                )}
                            </View>
                        )
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

        teamHeaderText: {
            flex: 1,
        },

        logoWrapper: {
            marginRight: 14,
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
            justifyContent:
                "space-between",
            alignItems: "flex-start",
            marginBottom: 8,
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

        weekBadge: {
            backgroundColor:
                "#E2E8F0",
            borderRadius: 10,
            paddingHorizontal: 10,
            paddingVertical: 7,
        },

        weekBadgeText: {
            fontSize: 12,
            fontWeight: "800",
            color: "#334155",
        },

        lastUpdated: {
            fontSize: 12,
            color: "#94A3B8",
            marginBottom: 14,
        },

        legend: {
            backgroundColor:
                "#EEF2F7",
            borderRadius: 10,
            paddingHorizontal: 10,
            paddingVertical: 8,
            marginBottom: 14,
        },

        legendText: {
            fontSize: 11,
            lineHeight: 16,
            fontWeight: "600",
            color: "#64748B",
        },

        loadingContainer: {
            alignItems: "center",
            paddingVertical: 50,
        },

        loadingText: {
            marginTop: 10,
            color: "#64748B",
        },

        emptyCard: {
            backgroundColor:
                "#FFFFFF",
            borderRadius: 18,
            borderWidth: 1,
            borderColor: "#E2E8F0",
            padding: 22,
        },

        emptyTitle: {
            fontSize: 17,
            fontWeight: "800",
            color: "#0F172A",
            marginBottom: 5,
        },

        emptyText: {
            color: "#64748B",
            fontSize: 14,
            lineHeight: 20,
        },

        injuryCard: {
            backgroundColor:
                "#FFFFFF",
            borderRadius: 16,
            borderWidth: 1,
            borderColor: "#E2E8F0",
            padding: 16,
            marginBottom: 12,
        },

        playerHeader: {
            flexDirection: "row",
            alignItems: "flex-start",
            justifyContent:
                "space-between",
        },

        playerInfo: {
            flex: 1,
            paddingRight: 12,
        },

        playerName: {
            fontSize: 17,
            fontWeight: "800",
            color: "#0F172A",
        },

        headshotContainer: {
            width: 52,
            height: 52,
            borderRadius: 26,
            overflow: "hidden",
            backgroundColor: "#F1F5F9",
            borderWidth: 1,
            borderColor: "#E2E8F0",
            marginRight: 12,
        },

        headshot: {
            width: "100%",
            height: "100%",
        },

        headshotFallback: {
            width: 52,
            height: 52,
            borderRadius: 26,
            backgroundColor: "#E2E8F0",
            borderWidth: 1,
            borderColor: "#CBD5E1",
            alignItems: "center",
            justifyContent: "center",
            marginRight: 12,
        },

        headshotFallbackText: {
            fontSize: 14,
            fontWeight: "800",
            color: "#64748B",
        },

        injuryText: {
            fontSize: 14,
            color: "#64748B",
            marginTop: 3,
        },

        positionBadge: {
            backgroundColor:
                "#F1F5F9",
            borderRadius: 8,
            paddingHorizontal: 9,
            paddingVertical: 5,
        },

        positionText: {
            fontSize: 12,
            fontWeight: "800",
            color: "#475569",
        },

        statusDivider: {
            height: 1,
            backgroundColor:
                "#E2E8F0",
            marginTop: 14,
            marginBottom: 12,
        },

        statusRow: {
            alignItems: "stretch",
            paddingRight: 4,
        },

        statusColumn: {
            width: 70,
            marginRight: 8,
            alignItems: "center",
        },

        gameColumn: {
            minWidth: 110,
            alignItems: "center",
        },

        statusLabel: {
            fontSize: 11,
            fontWeight: "800",
            color: "#475569",
        },

        statusDate: {
            fontSize: 10,
            color: "#94A3B8",
            marginTop: 2,
            marginBottom: 7,
        },

        statusValueBox: {
            minWidth: 58,
            backgroundColor:
                "#F1F5F9",
            borderRadius: 9,
            paddingVertical: 8,
            paddingHorizontal: 8,
            alignItems: "center",
        },

        statusValue: {
            fontSize: 13,
            fontWeight: "800",
            color: "#0F172A",
        },

        gameStatusBox: {
            minWidth: 100,
            backgroundColor:
                "#F1F5F9",
            borderRadius: 9,
            paddingVertical: 8,
            paddingHorizontal: 9,
            alignItems: "center",
        },

        gameStatusText: {
            fontSize: 12,
            fontWeight: "800",
            color: "#0F172A",
            textAlign: "center",
        },

        returnText: {
            marginTop: 12,
            fontSize: 12,
            color: "#64748B",
        },

        nbaStatusSection: {
            marginTop: 2,
        },

        nbaStatusRow: {
            flexDirection: "row",
            alignItems: "center",
            justifyContent: "space-between",
        },

        nbaComment: {
            marginTop: 12,
            fontSize: 13,
            lineHeight: 19,
            color: "#475569",
        },
    });