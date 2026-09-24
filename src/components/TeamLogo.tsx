import {
    Image,
    StyleSheet,
    Text,
    View,
} from "react-native";

import {
    useState,
} from "react";

const nflTeamCodes:
  Record<string, string> = {
  "Arizona Cardinals": "ari",
  "Atlanta Falcons": "atl",
  "Baltimore Ravens": "bal",
  "Buffalo Bills": "buf",
  "Carolina Panthers": "car",
  "Chicago Bears": "chi",
  "Cincinnati Bengals": "cin",
  "Cleveland Browns": "cle",
  "Dallas Cowboys": "dal",
  "Denver Broncos": "den",
  "Detroit Lions": "det",
  "Green Bay Packers": "gb",
  "Houston Texans": "hou",
  "Indianapolis Colts": "ind",
  "Jacksonville Jaguars": "jax",
  "Kansas City Chiefs": "kc",
  "Las Vegas Raiders": "lv",
  "Los Angeles Chargers": "lac",
  "Los Angeles Rams": "lar",
  "Miami Dolphins": "mia",
  "Minnesota Vikings": "min",
  "New England Patriots": "ne",
  "New Orleans Saints": "no",
  "New York Giants": "nyg",
  "New York Jets": "nyj",
  "Philadelphia Eagles": "phi",
  "Pittsburgh Steelers": "pit",
  "San Francisco 49ers": "sf",
  "Seattle Seahawks": "sea",
  "Tampa Bay Buccaneers": "tb",
  "Tennessee Titans": "ten",
  "Washington Commanders": "wsh",
};

const mlbTeamCodes:
  Record<string, string> = {
  "Arizona Diamondbacks": "ari",
  "Athletics": "oak",
  "Oakland Athletics": "oak",
  "Atlanta Braves": "atl",
  "Baltimore Orioles": "bal",
  "Boston Red Sox": "bos",
  "Chicago Cubs": "chc",
  "Chicago White Sox": "chw",
  "Cincinnati Reds": "cin",
  "Cleveland Guardians": "cle",
  "Colorado Rockies": "col",
  "Detroit Tigers": "det",
  "Houston Astros": "hou",
  "Kansas City Royals": "kc",
  "Los Angeles Angels": "laa",
  "Los Angeles Dodgers": "lad",
  "Miami Marlins": "mia",
  "Milwaukee Brewers": "mil",
  "Minnesota Twins": "min",
  "New York Mets": "nym",
  "New York Yankees": "nyy",
  "Philadelphia Phillies": "phi",
  "Pittsburgh Pirates": "pit",
  "San Diego Padres": "sd",
  "San Francisco Giants": "sf",
  "Seattle Mariners": "sea",
  "St. Louis Cardinals": "stl",
  "Tampa Bay Rays": "tb",
  "Texas Rangers": "tex",
  "Toronto Blue Jays": "tor",
  "Washington Nationals": "wsh",
};

function getLeagueEmoji(
  league: string
): string {
  switch (league.toUpperCase()) {
    case "NFL":
      return "🏈";

    case "NBA":
      return "🏀";

    case "MLB":
      return "⚾";

    default:
      return "🏟️";
  }
}

function getTeamLogoUrl(
  league: string,
  teamName: string
): string | null {

  const normalizedLeague =
    league.toUpperCase();

  let code: string | undefined;

  if (normalizedLeague === "NFL") {
    code =
      nflTeamCodes[teamName];
  }

  if (normalizedLeague === "MLB") {
    code =
      mlbTeamCodes[teamName];
  }

  if (!code) {
    return null;
  }

  return (
    "https://a.espncdn.com/i/teamlogos/"
    + normalizedLeague.toLowerCase()
    + "/500/"
    + code
    + ".png"
  );
}

type TeamLogoProps = {
  league: string;
  teamName: string;
  size?: number;
};

export default function TeamLogo({
  league,
  teamName,
  size = 48,
}: TeamLogoProps) {

  const [failed, setFailed] =
    useState(false);

  const logoUrl =
    getTeamLogoUrl(
      league,
      teamName
    );

  const imageSize =
    size * 0.81;

  if (
    !logoUrl ||
    failed
  ) {
    return (
      <View
        style={[
          styles.container,
          {
            width: size,
            height: size,
            borderRadius:
              size * 0.29,
          },
        ]}
      >
        <Text
          style={{
            fontSize:
              size * 0.52,
          }}
        >
          {getLeagueEmoji(
            league
          )}
        </Text>
      </View>
    );
  }

  return (
    <View
      style={[
        styles.container,
        {
          width: size,
          height: size,
          borderRadius:
            size * 0.29,
        },
      ]}
    >
      <Image
        source={{
          uri: logoUrl,
        }}
        style={{
          width: imageSize,
          height: imageSize,
        }}
        resizeMode="contain"
        onError={() =>
          setFailed(true)
        }
      />
    </View>
  );
}

const styles =
  StyleSheet.create({
    container: {
      backgroundColor:
        "#F8FAFC",
      borderWidth: 1,
      borderColor:
        "#E2E8F0",
      alignItems: "center",
      justifyContent:
        "center",
    },
  });