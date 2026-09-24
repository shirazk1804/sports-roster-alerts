const isDevelopment =
  process.env.APP_VARIANT === "development";

export default {
  expo: {
    name: isDevelopment
      ? "Sports Roster Alerts Dev"
      : "Sports Roster Alerts",

    slug: "sports-roster-alerts",

    version: "0.1.0",

    orientation: "portrait",

    icon: "./assets/images/icon.png",

    scheme: isDevelopment
      ? "sportsrosteralerts-dev"
      : "sportsrosteralerts",

    userInterfaceStyle: "automatic",

    ios: {
      icon: "./assets/images/icon.png",

      bundleIdentifier: isDevelopment
        ? "com.shirazk.sportsrosteralerts.dev"
        : "com.shirazk.sportsrosteralerts",

      infoPlist: {
        ITSAppUsesNonExemptEncryption: false,
      },
    },

    android: {
      package: isDevelopment
        ? "com.shirazk.sportsrosteralerts.dev"
        : "com.shirazk.sportsrosteralerts",

      adaptiveIcon: {
        backgroundColor: "#E6F4FE",
        foregroundImage:
          "./assets/images/android-icon-foreground.png",
        backgroundImage:
          "./assets/images/android-icon-background.png",
        monochromeImage:
          "./assets/images/android-icon-monochrome.png",
      },

      predictiveBackGestureEnabled: false,
    },

    web: {
      output: "static",
      favicon: "./assets/images/icon.png",
    },

    plugins: [
      "expo-router",
      [
        "expo-splash-screen",
        {
          backgroundColor: "#208AEF",
          image: "./assets/images/splash-icon.png",
          imageWidth: 76,
        },
      ],
      "expo-secure-store",
    ],

    experiments: {
      typedRoutes: true,
      reactCompiler: true,
    },

    extra: {
      router: {},

      eas: {
        projectId:
          "2a9ca2e0-7ea4-4f55-bd68-2610e1e1c07e",
      },
    },
  },
};