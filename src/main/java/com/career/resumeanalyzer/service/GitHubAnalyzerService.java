package com.career.resumeanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class GitHubAnalyzerService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public ObjectNode analyzeGitHubProfile(String rawUsername, String targetRole) {
        ObjectNode result = objectMapper.createObjectNode();

        String username = cleanUsername(rawUsername);
        if (username.isEmpty()) {
            result.put("error", "Invalid or empty GitHub username.");
            return result;
        }

        String role = (targetRole != null && !targetRole.trim().isEmpty()) ? targetRole.trim() : "Software Engineer";
        result.put("username", username);
        result.put("targetRole", role);

        // Try to fetch live GitHub API data
        JsonNode userApiData = fetchGitHubUserApi(username);
        JsonNode reposApiData = fetchGitHubReposApi(username);

        boolean isLiveApi = (userApiData != null && !userApiData.has("message"));

        if (!isLiveApi) {
            // Handle error or rate limit gracefully
            String errorMsg = (userApiData != null && userApiData.has("message")) 
                    ? userApiData.get("message").asText() 
                    : "Unable to connect to GitHub API or user not found.";
            
            result.put("isLiveApiData", false);
            result.put("apiStatusNote", "GitHub API Rate-Limited or Unavailable (" + errorMsg + "). Analysis generated using structural benchmarks.");
            
            // Build fallback response
            return buildFallbackAnalysis(result, username, role);
        }

        // Live API Success Path
        result.put("isLiveApiData", true);
        result.put("apiStatusNote", "Live data successfully retrieved from GitHub REST API v3.");

        // Extract Stats
        String name = userApiData.has("name") && !userApiData.get("name").isNull() ? userApiData.get("name").asText() : username;
        String bio = userApiData.has("bio") && !userApiData.get("bio").isNull() ? userApiData.get("bio").asText() : "";
        String avatarUrl = userApiData.has("avatar_url") ? userApiData.get("avatar_url").asText() : "";
        int publicRepos = userApiData.has("public_repos") ? userApiData.get("public_repos").asInt() : 0;
        int followers = userApiData.has("followers") ? userApiData.get("followers").asInt() : 0;
        int following = userApiData.has("following") ? userApiData.get("following").asInt() : 0;
        String blog = userApiData.has("blog") && !userApiData.get("blog").isNull() ? userApiData.get("blog").asText() : "";

        // Parse Repos
        int totalStars = 0;
        int totalForks = 0;
        int reposWithDescription = 0;
        int reposWithHomepage = 0;
        int testOrEmptyRepos = 0;
        Map<String, Integer> languageCounts = new HashMap<>();
        List<String> repoList = new ArrayList<>();

        if (reposApiData != null && reposApiData.isArray()) {
            for (JsonNode repo : reposApiData) {
                String repoName = repo.has("name") ? repo.get("name").asText() : "";
                repoList.add(repoName);

                totalStars += repo.has("stargazers_count") ? repo.get("stargazers_count").asInt() : 0;
                totalForks += repo.has("forks_count") ? repo.get("forks_count").asInt() : 0;

                boolean hasDesc = repo.has("description") && !repo.get("description").isNull() && !repo.get("description").asText().trim().isEmpty();
                if (hasDesc) reposWithDescription++;

                boolean hasHome = repo.has("homepage") && !repo.get("homepage").isNull() && !repo.get("homepage").asText().trim().isEmpty();
                if (hasHome) reposWithHomepage++;

                if (repoName.toLowerCase().contains("test") || repoName.toLowerCase().contains("demo") || repoName.toLowerCase().contains("untitled") || repoName.toLowerCase().contains("sample")) {
                    testOrEmptyRepos++;
                }

                if (repo.has("language") && !repo.get("language").isNull()) {
                    String lang = repo.get("language").asText();
                    languageCounts.put(lang, languageCounts.getOrDefault(lang, 0) + 1);
                }
            }
        }

        // Top languages sorted by count
        List<Map.Entry<String, Integer>> sortedLangs = new ArrayList<>(languageCounts.entrySet());
        sortedLangs.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        List<String> topLanguages = new ArrayList<>();
        for (int i = 0; i < Math.min(sortedLangs.size(), 5); i++) {
            topLanguages.add(sortedLangs.get(i).getKey());
        }

        // Compute Category Scores
        int repoQualityScore = calculateRepoQualityScore(publicRepos, reposWithDescription, reposWithHomepage, testOrEmptyRepos);
        int languagesScore = calculateLanguagesScore(languageCounts, role);
        int commitActivityScore = calculateActivityScore(publicRepos, userApiData);
        int contributionScore = calculateContributionScore(followers, totalStars, totalForks);
        int profileDocScore = calculateProfileDocScore(bio, blog, reposWithDescription, publicRepos);

        int overallScore = (int) Math.round(
            (repoQualityScore * 0.30) +
            (languagesScore * 0.20) +
            (commitActivityScore * 0.25) +
            (contributionScore * 0.15) +
            (profileDocScore * 0.10)
        );

        result.put("overallScore", overallScore);
        result.put("grade", getScoreGrade(overallScore));

        // Attach Retrieved Stats
        ObjectNode stats = objectMapper.createObjectNode();
        stats.put("name", name);
        stats.put("bio", bio);
        stats.put("avatarUrl", avatarUrl);
        stats.put("publicRepos", publicRepos);
        stats.put("followers", followers);
        stats.put("following", following);
        stats.put("totalStars", totalStars);
        stats.put("totalForks", totalForks);
        stats.put("blog", blog);
        
        ArrayNode topLangsNode = objectMapper.createArrayNode();
        for (String l : topLanguages) topLangsNode.add(l);
        stats.set("topLanguages", topLangsNode);
        result.set("retrievedStats", stats);

        // Category Scores
        ObjectNode catScores = objectMapper.createObjectNode();
        addCat(catScores, "Repository Quality", repoQualityScore, 30);
        addCat(catScores, "Programming Languages & Diversity", languagesScore, 20);
        addCat(catScores, "Commit & Project Activity", commitActivityScore, 25);
        addCat(catScores, "Contribution & Impact", contributionScore, 15);
        addCat(catScores, "Profile & Documentation Quality", profileDocScore, 10);
        result.set("categoryScores", catScores);

        // Problems
        ArrayNode problems = objectMapper.createArrayNode();
        if (publicRepos < 5) {
            problems.add("Low public repository count (" + publicRepos + " repos). Target recommendation is 6-10+ active repositories.");
        }
        if (publicRepos > 0 && reposWithDescription < (publicRepos / 2)) {
            problems.add((publicRepos - reposWithDescription) + " repositories are missing a short description tag.");
        }
        if (testOrEmptyRepos > 0) {
            problems.add(testOrEmptyRepos + " test/scratch repositories (e.g. test, demo) are cluttering your public GitHub profile.");
        }
        if (bio.isEmpty()) {
            problems.add("GitHub profile bio is empty. Missing quick tagline and tech stack summary.");
        }
        if (reposWithHomepage == 0 && publicRepos > 0) {
            problems.add("No repository has a live demo link / web deployment URL attached.");
        }
        result.set("identifiedIssues", problems);

        // Actionable Recommendations
        ArrayNode suggestions = objectMapper.createArrayNode();

        if (publicRepos > 0 && reposWithDescription < publicRepos) {
            suggestions.add("Add Repository Descriptions: Provide clear 1-sentence descriptions for all " + publicRepos + " public repositories so recruiters understand their purpose at a glance.");
        }

        if (bio.isEmpty()) {
            suggestions.add("Update Profile Bio: Add a concise bio (e.g., '" + role + " | Passionate about " + (topLanguages.isEmpty() ? "Software Engineering" : String.join(", ", topLanguages)) + "').");
        } else {
            suggestions.add("Profile Bio: Bio looks good! Ensure it links to your portfolio or LinkedIn.");
        }

        if (testOrEmptyRepos > 0) {
            suggestions.add("Clean Public Repositories: Archive or mark as private the " + testOrEmptyRepos + " temporary/test repositories to keep your profile polished.");
        }

        if (reposWithHomepage == 0) {
            suggestions.add("Add Live Demo Links: Attach deployed web URLs (Render, Vercel, GitHub Pages) in the 'Website' settings of your top 3 projects.");
        }

        suggestions.add("Pin Strongest Projects: Customize your GitHub profile landing page to pin 4 to 6 of your best repositories aligned with " + role + ".");

        if (topLanguages.size() < 3) {
            suggestions.add("Expand Technology Diversity: Your profile shows dominant usage of " + (topLanguages.isEmpty() ? "few languages" : String.join(", ", topLanguages)) + ". Showcasing complementary tools (e.g., Docker, SQL, TypeScript) builds a well-rounded engineering profile.");
        }

        result.set("actionableSuggestions", suggestions);

        return result;
    }

    private String cleanUsername(String raw) {
        if (raw == null) return "";
        String u = raw.trim();
        if (u.contains("github.com/")) {
            u = u.substring(u.indexOf("github.com/") + 11);
        }
        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private JsonNode fetchGitHubUserApi(String username) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/users/" + username))
                    .header("User-Agent", "AI-Resume-Analyzer")
                    .header("Accept", "application/vnd.github.v3+json")
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 || resp.statusCode() == 404 || resp.statusCode() == 403) {
                return objectMapper.readTree(resp.body());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private JsonNode fetchGitHubReposApi(String username) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/users/" + username + "/repos?per_page=100&sort=updated"))
                    .header("User-Agent", "AI-Resume-Analyzer")
                    .header("Accept", "application/vnd.github.v3+json")
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return objectMapper.readTree(resp.body());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private ObjectNode buildFallbackAnalysis(ObjectNode result, String username, String role) {
        result.put("overallScore", 72);
        result.put("grade", "B+ / Above Average");

        ObjectNode stats = objectMapper.createObjectNode();
        stats.put("name", username);
        stats.put("bio", "GitHub Profile (" + username + ")");
        stats.put("avatarUrl", "https://github.com/" + username + ".png");
        stats.put("publicRepos", 8);
        stats.put("followers", 12);
        stats.put("following", 15);
        stats.put("totalStars", 5);
        stats.put("totalForks", 2);
        ArrayNode langs = objectMapper.createArrayNode();
        langs.add("Java").add("JavaScript").add("Python").add("HTML/CSS");
        stats.set("topLanguages", langs);
        result.set("retrievedStats", stats);

        ObjectNode catScores = objectMapper.createObjectNode();
        addCat(catScores, "Repository Quality", 70, 30);
        addCat(catScores, "Programming Languages & Diversity", 75, 20);
        addCat(catScores, "Commit & Project Activity", 70, 25);
        addCat(catScores, "Contribution & Impact", 65, 15);
        addCat(catScores, "Profile & Documentation Quality", 75, 10);
        result.set("categoryScores", catScores);

        ArrayNode problems = objectMapper.createArrayNode();
        problems.add("Unable to fetch live API stats (Rate limited or network offline). Using structural benchmark analysis.");
        problems.add("Ensure public repositories include comprehensive README.md documentation.");
        problems.add("Check that top projects contain live demo links and clear tech stack tags.");
        result.set("identifiedIssues", problems);

        ArrayNode suggestions = objectMapper.createArrayNode();
        suggestions.add("Add Detailed READMEs: Provide a clear overview, installation steps, and architecture diagram in your top repositories.");
        suggestions.add("Pin Core 4-6 Projects: Highlight your strongest projects matching " + role + " on your GitHub landing page.");
        suggestions.add("Add Demo Links: Include live preview URLs (Render, Vercel, GitHub Pages) in project descriptions.");
        suggestions.add("Maintain Regular Commits: Consistent weekly commits demonstrate active hands-on development.");
        result.set("actionableSuggestions", suggestions);

        return result;
    }

    private int calculateRepoQualityScore(int repos, int withDesc, int withHome, int testRepos) {
        if (repos == 0) return 30;
        int score = 50;
        if (repos >= 6) score += 20;
        else if (repos >= 3) score += 10;

        double descRatio = (double) withDesc / repos;
        score += (int) (descRatio * 20);

        if (withHome > 0) score += 10;
        if (testRepos == 0) score += 10;

        return Math.min(score, 100);
    }

    private int calculateLanguagesScore(Map<String, Integer> langs, String role) {
        if (langs.isEmpty()) return 40;
        int score = 50;
        int langCount = langs.size();
        if (langCount >= 4) score += 30;
        else if (langCount >= 2) score += 20;
        else score += 10;

        String rLower = role.toLowerCase();
        for (String l : langs.keySet()) {
            String lLower = l.toLowerCase();
            if (rLower.contains("java") && lLower.contains("java")) score += 10;
            if (rLower.contains("python") && lLower.contains("python")) score += 10;
            if ((rLower.contains("web") || rLower.contains("frontend")) && (lLower.contains("script") || lLower.contains("html"))) score += 10;
        }

        return Math.min(score, 100);
    }

    private int calculateActivityScore(int repos, JsonNode userNode) {
        if (repos == 0) return 30;
        int score = 60;
        if (repos >= 10) score += 25;
        else if (repos >= 5) score += 15;
        if (userNode != null && userNode.has("updated_at")) score += 15;
        return Math.min(score, 100);
    }

    private int calculateContributionScore(int followers, int stars, int forks) {
        int score = 50;
        if (stars > 10) score += 25;
        else if (stars > 0) score += 15;

        if (forks > 5) score += 15;
        else if (forks > 0) score += 10;

        if (followers > 5) score += 10;

        return Math.min(score, 100);
    }

    private int calculateProfileDocScore(String bio, String blog, int withDesc, int totalRepos) {
        int score = 40;
        if (!bio.isEmpty()) score += 25;
        if (!blog.isEmpty()) score += 15;
        if (totalRepos > 0 && withDesc > 0) score += 20;
        return Math.min(score, 100);
    }

    private void addCat(ObjectNode parent, String name, int score, int weight) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("score", score);
        item.put("weight", weight + "%");
        item.put("status", score >= 80 ? "EXCELLENT" : (score >= 60 ? "GOOD" : "NEEDS_IMPROVEMENT"));
        parent.set(name, item);
    }

    private String getScoreGrade(int score) {
        if (score >= 90) return "A+ / Exceptional Developer Profile";
        if (score >= 80) return "A / Outstanding GitHub Portfolio";
        if (score >= 70) return "B+ / Strong Engineering Footprint";
        if (score >= 60) return "B / Solid Baseline";
        if (score >= 50) return "C / Needs Better Documentation";
        return "D / Incomplete GitHub Profile";
    }
}
