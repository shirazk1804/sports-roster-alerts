package sportsalerts;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Service
public class NbaHeadshotService {

    private record CachedTeamPage(
            Document document,
            LocalDateTime fetchedAt) {
    }

    private static final long CACHE_HOURS = 24;

    private static final Map<String, String>
            TEAM_SLUGS = Map.ofEntries(

            Map.entry(
                    "Atlanta Hawks",
                    "hawks"),

            Map.entry(
                    "Boston Celtics",
                    "celtics"),

            Map.entry(
                    "Brooklyn Nets",
                    "nets"),

            Map.entry(
                    "Charlotte Hornets",
                    "hornets"),

            Map.entry(
                    "Chicago Bulls",
                    "bulls"),

            Map.entry(
                    "Cleveland Cavaliers",
                    "cavaliers"),

            Map.entry(
                    "Dallas Mavericks",
                    "mavs"),

            Map.entry(
                    "Denver Nuggets",
                    "nuggets"),

            Map.entry(
                    "Detroit Pistons",
                    "pistons"),

            Map.entry(
                    "Golden State Warriors",
                    "warriors"),

            Map.entry(
                    "Houston Rockets",
                    "rockets"),

            Map.entry(
                    "Indiana Pacers",
                    "pacers"),

            Map.entry(
                    "LA Clippers",
                    "clippers"),

            Map.entry(
                    "Los Angeles Lakers",
                    "lakers"),

            Map.entry(
                    "Memphis Grizzlies",
                    "grizzlies"),

            Map.entry(
                    "Miami Heat",
                    "heat"),

            Map.entry(
                    "Milwaukee Bucks",
                    "bucks"),

            Map.entry(
                    "Minnesota Timberwolves",
                    "timberwolves"),

            Map.entry(
                    "New Orleans Pelicans",
                    "pelicans"),

            Map.entry(
                    "New York Knicks",
                    "knicks"),

            Map.entry(
                    "Oklahoma City Thunder",
                    "thunder"),

            Map.entry(
                    "Orlando Magic",
                    "magic"),

            Map.entry(
                    "Philadelphia 76ers",
                    "sixers"),

            Map.entry(
                    "Phoenix Suns",
                    "suns"),

            Map.entry(
                    "Portland Trail Blazers",
                    "blazers"),

            Map.entry(
                    "Sacramento Kings",
                    "kings"),

            Map.entry(
                    "San Antonio Spurs",
                    "spurs"),

            Map.entry(
                    "Toronto Raptors",
                    "raptors"),

            Map.entry(
                    "Utah Jazz",
                    "jazz"),

            Map.entry(
                    "Washington Wizards",
                    "wizards")
    );

    private final NbaPlayerHeadshotRepository
            headshotRepository;

    /*
     * Avoid downloading the same team page
     * repeatedly while multiple injured players
     * from that team are being resolved.
     */
    private final Map<String, CachedTeamPage>
            teamPageCache =
            new ConcurrentHashMap<>();

    public NbaHeadshotService(
            NbaPlayerHeadshotRepository
                    headshotRepository) {

        this.headshotRepository =
                headshotRepository;
    }

    public String getHeadshotUrl(
            String teamName,
            String nbaPlayerId,
            String playerName) {

        if (nbaPlayerId == null ||
                nbaPlayerId.isBlank()) {

            return null;
        }

        /*
         * Use a recently resolved official
         * team image when available.
         */
        NbaPlayerHeadshot cached =
                headshotRepository
                        .findByNbaPlayerIdAndTeamName(
                                nbaPlayerId,
                                teamName)
                        .orElse(null);

        if (cached != null &&
                cached.getUpdatedAt() != null &&
                cached.getUpdatedAt()
                        .isAfter(
                                LocalDateTime.now(
                                        ZoneOffset.UTC)
                                        .minusHours(
                                                CACHE_HOURS))) {

            return cached.getHeadshotUrl();
        }

        /*
         * Try the current team's official
         * NBA roster page.
         */
        String currentTeamHeadshot =
                resolveCurrentTeamHeadshot(
                        teamName,
                        nbaPlayerId,
                        playerName);

        if (currentTeamHeadshot != null &&
                !currentTeamHeadshot.isBlank()) {

            saveHeadshot(
                    cached,
                    nbaPlayerId,
                    playerName,
                    teamName,
                    currentTeamHeadshot);

            return currentTeamHeadshot;
        }

        /*
         * Reliable fallback.
         *
         * This image may occasionally lag behind
         * a recent trade/signing, but is preferable
         * to displaying no photo at all.
         */
        return buildGenericHeadshotUrl(
                nbaPlayerId);
    }

    private String resolveCurrentTeamHeadshot(
            String teamName,
            String nbaPlayerId,
            String playerName) {

        String slug =
                TEAM_SLUGS.get(
                        teamName);

        if (slug == null) {
            return null;
        }

        try {

            Document document =
                    getTeamRosterPage(
                            teamName,
                            slug);

            if (document == null) {
                return null;
            }

            /*
             * Strategy 1:
             *
             * Many NBA team pages use links such
             * as:
             *
             * /mavs/player/1641726/dereck-lively-ii
             *
             * NBA ID gives us an exact match.
             */
            for (
                    Element link :
                    document.select(
                            "a[href]")) {

                String href =
                        link.attr(
                                "href");

                if (!href.contains(
                        "/player/"
                                + nbaPlayerId)) {

                    continue;
                }

                String imageUrl =
                        findImageNearElement(
                                link);

                if (isUsableImage(
                        imageUrl)) {

                    return imageUrl;
                }
            }

            /*
             * Strategy 2:
             *
             * Team-specific sites such as the
             * Sixers may not put the NBA player
             * ID in their player URL.
             *
             * Match the player's displayed name
             * to the surrounding roster card.
             */
            String normalizedPlayerName =
                    normalize(
                            playerName);

            for (
                    Element image :
                    document.select(
                            "img")) {

                String imageUrl =
                        getImageUrl(
                                image);

                if (!isUsableImage(
                        imageUrl)) {

                    continue;
                }

                Element container =
                        image;

                for (
                        int level = 0;
                        level < 7 &&
                                container != null;
                        level++) {

                    String containerText =
                            normalize(
                                    container.text());

                    if (!normalizedPlayerName
                            .isBlank() &&
                            containerText.contains(
                                    normalizedPlayerName)) {

                        return imageUrl;
                    }

                    container =
                            container.parent();
                }
            }

        } catch (Exception exception) {

            System.err.println(
                    "Could not resolve current NBA headshot for "
                            + playerName
                            + " on "
                            + teamName
                            + ": "
                            + exception.getMessage());
        }

        return null;
    }

    private Document getTeamRosterPage(
            String teamName,
            String slug) {

        CachedTeamPage cached =
                teamPageCache.get(
                        teamName);

        LocalDateTime now =
                LocalDateTime.now(
                        ZoneOffset.UTC);

        if (cached != null &&
                cached.fetchedAt()
                        .isAfter(
                                now.minusMinutes(
                                        30))) {

            return cached.document();
        }

        try {

            String url =
                    "https://www.nba.com/"
                            + slug
                            + "/roster";

            Document document =
                    Jsoup.connect(
                            url)
                            .userAgent(
                                    "Mozilla/5.0")
                            .referrer(
                                    "https://www.nba.com/")
                            .timeout(
                                    10000)
                            .followRedirects(
                                    true)
                            .get();

            teamPageCache.put(
                    teamName,
                    new CachedTeamPage(
                            document,
                            now));

            return document;

        } catch (Exception exception) {

            System.err.println(
                    "Could not load NBA roster page for "
                            + teamName
                            + ": "
                            + exception.getMessage());

            return null;
        }
    }

    private String findImageNearElement(
            Element element) {

        Element current =
                element;

        for (
                int level = 0;
                level < 7 &&
                        current != null;
                level++) {

            Element image =
                    current.selectFirst(
                            "img");

            if (image != null) {

                String imageUrl =
                        getImageUrl(
                                image);

                if (isUsableImage(
                        imageUrl)) {

                    return imageUrl;
                }
            }

            current =
                    current.parent();
        }

        return null;
    }

    private String getImageUrl(
            Element image) {

        String src =
                image.attr(
                        "src");

        if (!src.isBlank()) {
            return src;
        }

        src =
                image.attr(
                        "data-src");

        if (!src.isBlank()) {
            return src;
        }

        return image.attr(
                "data-lazy-src");
    }

    private boolean isUsableImage(
            String imageUrl) {

        if (imageUrl == null ||
                imageUrl.isBlank()) {

            return false;
        }

        String normalized =
                imageUrl.toLowerCase();

        return normalized.startsWith(
                "https://")
                &&
                (
                    normalized.contains(
                            "cdn.nba.com")
                    ||
                    normalized.contains(
                            "nba.com")
                );
    }

    private void saveHeadshot(
            NbaPlayerHeadshot existing,
            String nbaPlayerId,
            String playerName,
            String teamName,
            String headshotUrl) {

        if (existing == null) {

            headshotRepository.save(
                    new NbaPlayerHeadshot(
                            nbaPlayerId,
                            playerName,
                            teamName,
                            headshotUrl));

            return;
        }

        existing.update(
                playerName,
                teamName,
                headshotUrl);

        headshotRepository.save(
                existing);
    }

    private String buildGenericHeadshotUrl(
            String nbaPlayerId) {

        return "https://cdn.nba.com/headshots/nba/latest/260x190/"
                + nbaPlayerId
                + ".png";
    }

    private String normalize(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .toLowerCase()
                .replaceAll(
                        "[^a-z0-9]",
                        "");
    }
}