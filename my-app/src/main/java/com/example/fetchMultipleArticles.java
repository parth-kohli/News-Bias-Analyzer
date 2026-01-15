package com.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class fetchMultipleArticles {
    private static final String API_KEY = "e8efcdd3703b4e58b9b3f207e4591d95"; // Use your own API key
    private static final String CACHE_FILE = "cached_articles"+LocalDate.now().toString()+".json";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Set<String> STOP_WORDS = Set.of("a", "an", "the", "in", "on", "at", "for", "with", "is", "are", "was", "were", "to", "of", "and", "says", "posts", "after", "by");
    //Mapping from each news source to its id
    private static final Map<String, String> DOMAIN_TO_SOURCE_ID = Map.ofEntries(
            Map.entry("apnews.com", "associated-press"), Map.entry("reuters.com", "reuters"),
            Map.entry("bbc.com", "bbc-news"), Map.entry("wsj.com", "the-wall-street-journal"),
            Map.entry("theguardian.com", "the-guardian-uk"), Map.entry("msnbc.com", "msnbc"),
            Map.entry("foxnews.com", "fox-news"), Map.entry("huffpost.com", "the-huffington-post"),
            Map.entry("vox.com", "vox"), Map.entry("breitbart.com", "breitbart-news"),
            Map.entry("newsweek.com", "newsweek"), Map.entry("cbsnews.com", "cbs-news"),
            Map.entry("theblaze.com", "the-blaze")
    );
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.ofEntries(
            Map.entry("POLITICS", List.of("/politics", "politics.", "election", "government")),
            Map.entry("BUSINESS", List.of("/business", "business.", "/finance", "/economy", "markets")),
            Map.entry("TECHNOLOGY", List.of("/tech", "tech.", "/technology", "gadgets", "/science")),
            Map.entry("SPORTS", List.of("/sports", "sports.", "nfl", "nba", "mlb", "soccer", "olympics")),
            Map.entry("ENTERTAINMENT", List.of("/entertainment", "entertainment.", "/movies", "/music", "celebrity")),
            Map.entry("HEALTH", List.of("/health", "health.", "medical", "wellness")),
            Map.entry("WORLD", List.of("/world", "world.", "/international", "global")),
            Map.entry("OPINION", List.of("/opinion", "opinion.", "/editorial", "/commentary"))
    );
    static TfidfVectorizer vectorizer;
    static MultinomialNaiveBayes model;

    public static List<List<Article>> fetchAndClusterArticles(ModelTrainer.TrainedModels trainedModels) {
        //Vectorizer and training model for Naive Bayes is Initialized
        fetchMultipleArticles.vectorizer = trainedModels.vectorizer;
        fetchMultipleArticles.model = trainedModels.classifier;
        List<String> domainsToSearch = Arrays.asList("apnews.com", "msnbc.com", "foxnews.com", "reuters.com", "huffpost.com", "nypost.com", "bbc.com", "theguardian.com", "wsj.com", "cbsnews.com");
        System.out.println("Checking for cached articles at '" + CACHE_FILE + "'...");
        //If articles are present in cache it gets the values
        List<Article> allArticles = loadArticlesFromCache();
        if (allArticles.isEmpty()) {
            System.out.println("Cache not found or empty. Fetching from API...");
            allArticles = fetchAllArticles(domainsToSearch, 100, "");
            saveArticlesToCache(allArticles);
            System.out.println("Found " + allArticles.size() + " articles and saved to cache.");
        } else {
            System.out.println("Loaded " + allArticles.size() + " articles from cache.");
        }
        if (allArticles.isEmpty()) {
            System.out.println("Could not fetch or load any valid articles.");
            return Collections.emptyList();
        }
        System.out.println("\nClustering articles to find specific events...");
        //Clustering similar articles together
        List<List<Article>> clusters = clusterArticles(allArticles);
        System.out.println("Processing " + clusters.size() + " clusters to concatenate categories...");
        //Groups all the categories in the cluster and puts it in the main article i.e. the first element
        for (List<Article> cluster : clusters) {
            if (cluster == null || cluster.isEmpty()) {
                continue;
            }
            Set<String> categoriesInCluster = new LinkedHashSet<>();
            for (Article article : cluster) {
                if (article.category != null && !article.category.isBlank()) {
                    String[] individualCategories = article.category.split(", ");
                    for (String cat : individualCategories) {
                        if (cat != null && !cat.isBlank()) {
                            categoriesInCluster.add(cat.trim());
                        }
                    }
                }
            }
            String concatenatedCategories = String.join(", ", categoriesInCluster);
            cluster.get(0).category = concatenatedCategories;
        }
        System.out.println("Sorting " + clusters.size() + " clusters using custom Merge Sort...");
        List<List<Article>> sortedClusters = mergeSort(clusters);
        //Returns a list of 10 clusters with atleast 1 biased article
        return sortedClusters.stream()
                .filter(cluster -> cluster.size() > 1)
                .limit(10)
                .collect(Collectors.toList());
    }
    //Scrapes article from the url of the news
    private static String scrapeArticle(String url) {
        try {
            Document doc = Jsoup.connect(url).timeout(10000).get(); 
            Element articleBody = doc.selectFirst("article, div.article-body, div.article-content, main, [role=main]");
            if (articleBody != null) {
                return articleBody.text().replaceAll("\\s+", " "); 
            }
            return null;
        } catch (Exception e) {
            System.err.println("   ! Scrape failed for " + url + ": " + e.getMessage());
            return null;
        }
    }
    //Calculates and maps the bias scores to a Hash Map and returns the value
    public static Map<String, Double> calculateBiasScores(String text) {
        if (vectorizer == null || model == null || text == null || text.isBlank()) {
            return Map.of("Left", 0.33, "Center", 0.34, "Right", 0.33);
        }
        double[] vector = vectorizer.transformToVector(text);
        Map<String, Double> probs = model.predict_proba(vector);
        probs.putIfAbsent("left", 0.0);
        probs.putIfAbsent("center", 0.0);
        probs.putIfAbsent("right", 0.0);
        
        return Map.of(
            "Left", probs.get("left"),
            "Center", probs.get("center"),
            "Right", probs.get("right")
        );
    }

    //Gets the response from the api
    private static List<Article> fetchAllArticles(List<String> domains, int countPerSource, String topicQuery) {
        Set<Article> articleSet = new LinkedHashSet<>();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String yesterdayStr = yesterday.format(DateTimeFormatter.ISO_LOCAL_DATE);

        for (String domain : domains) {
            try {
                //Api call to fetch the JSON response
                String queryParam = DOMAIN_TO_SOURCE_ID.containsKey(domain) ? "&sources=" + DOMAIN_TO_SOURCE_ID.get(domain) : "&domains=" + domain;
                String url = "https://newsapi.org/v2/everything?q=" + ""
                        + "&from=" + yesterdayStr + "&to=" + yesterdayStr
                        + queryParam + "&pageSize=" + countPerSource + "&language=en&apiKey=" + API_KEY;

                String responseBody = sendRequest(url);
                JSONObject json = new JSONObject(responseBody);

                if (!"ok".equals(json.optString("status"))) continue;

                JSONArray articles = json.getJSONArray("articles");
                //Maps JSON values to the article
                for (int i = 0; i < articles.length(); i++) {
                    JSONObject articleJson = articles.getJSONObject(i);
                    String title = articleJson.optString("title");
                    if (title == null || title.isBlank() || "[Removed]".equals(title) || title.length() < 25) {
                        continue;
                    }

                    String description = articleJson.optString("description");
                    String imageUrl = articleJson.optString("urlToImage");
                    String publishedAt = articleJson.getString("publishedAt");
                    String sourceName = articleJson.getJSONObject("source").getString("name");
                    String articleUrl = articleJson.getString("url");
                    System.out.println("   -> Scraping: " + title);
                    //Scrapes article at url to get body
                    String fullArticleText = scrapeArticle(articleUrl);
                    String category = categorizeNewsUrl(url);
                    
                    String textToAnalyze;
                    String contentForDB;
                    //Forms the text whose score is calculated by Naive Bayes
                    if (fullArticleText != null && !fullArticleText.isBlank()) {
                        textToAnalyze = title + " " + description + " " + fullArticleText;
                        contentForDB = fullArticleText;
                    } else {
                        System.out.println("   Scrape failed, falling back to snippet for: " + title);
                        String apiSnippet = articleJson.optString("content");
                        textToAnalyze = title + " " + description + " " + apiSnippet;
                        contentForDB = (apiSnippet != null && !apiSnippet.isBlank()) ? apiSnippet : "No content available.";
                    }
                    // Finds the bias score for each article
                    Map<String, Double> biasScores = calculateBiasScores(textToAnalyze);
                    articleSet.add(new Article(
                            title, articleUrl, sourceName, publishedAt,
                            description, imageUrl, 
                            contentForDB, category,
                            biasScores.getOrDefault("Left", 0.0).floatValue(),
                            biasScores.getOrDefault("Center", 0.0).floatValue(),
                            biasScores.getOrDefault("Right", 0.0).floatValue()
                    ));
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch from " + domain + ". Error: " + e.getMessage());
            }
        }
        return new ArrayList<>(articleSet);
    }

    //Function to convert articles to JSON file
    private static void saveArticlesToCache(List<Article> articles) {
        try {
            Gson gson = new Gson();
            String json = gson.toJson(articles);
            Files.writeString(Paths.get(CACHE_FILE), json);
        } catch (IOException e) {
            System.err.println("Error saving articles to cache: " + e.getMessage());
        }
    }
    // Function to load articles from cache, returns empty list if no cache
    private static List<Article> loadArticlesFromCache() {
        try {
            Path path = Paths.get(CACHE_FILE);
            if (!Files.exists(path)) return Collections.emptyList();
            String json = Files.readString(path);
            if (json == null || json.isBlank()) return Collections.emptyList();
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Article>>() {}.getType();
            return gson.fromJson(json, type);
        } catch (IOException e) {
            System.err.println("Error loading articles from cache: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    //Clusters article based on similarity score bw 2 articles
    public static List<List<Article>> clusterArticles(List<Article> articles) {
        List<List<Article>> clusters = new ArrayList<>();
        Set<Article> clusteredArticles = new HashSet<>();
        //Loops through every article to form cluster
        for (Article articleA : articles) {
            if (clusteredArticles.contains(articleA)) continue;
            List<Article> newCluster = new ArrayList<>();
            newCluster.add(articleA);
            clusteredArticles.add(articleA);
            //Compares similarity with all other articles and if they are same and they don't already exist in a cluster they are added to a cluster
            for (Article articleB : articles) {
                if (!articleA.equals(articleB) && !clusteredArticles.contains(articleB) && isTitleSimilar(articleA.title, articleB.title)) {
                    newCluster.add(articleB);
                    clusteredArticles.add(articleB);
                }
            }
            clusters.add(sortCluster(newCluster));
        }
        return clusters;
    }
    //Sorts cluster so that max center bias score is at the first element
    private  static List<Article> sortCluster( List<Article> cluster){
        if (cluster.size()<=1) return cluster;
        int maxIndex =0;
        Article maxArticle;
        float maxVal = cluster.get(0).centerBias;
        for (int i =0; i<cluster.size(); i++){
            if (cluster.get(i).centerBias>maxVal){
                maxVal= cluster.get(i).centerBias;
                maxIndex = i;
            }
        }
        maxArticle=cluster.get(maxIndex);
        cluster.remove(maxIndex);
        cluster.add(maxIndex,cluster.get(0));
        cluster.remove(0);
        cluster.add(0, maxArticle);
        return cluster;

    }
    //Uses string comparison to find similarity between 2 articles
    private static boolean isTitleSimilar(String title1, String title2) {
        //Defines weightage of proper and common keywords
        final double ENTITY_KEYWORD_THRESHOLD = 0.20;
        final double PURE_KEYWORD_THRESHOLD = 0.4;
        //Finds 2 word proper nouns eg: Joe Biden by finding title case using regex
        Set<String> multiWordEntities1 = getEntities(title1, "\\b[A-Z][a-z]+(?:\\s[A-Z][a-z]+){1,2}\\b");
        Set<String> multiWordEntities2 = getEntities(title2, "\\b[A-Z][a-z]+(?:\\s[A-Z][a-z]+){1,2}\\b");
        //Finds 1 word proper nouns eg: Biden by finding title case using regex
        Set<String> singleWordEntities1 = getEntities(title1, "\\b[A-Z][a-z]{3,}\\b");
        Set<String> singleWordEntities2 = getEntities(title2, "\\b[A-Z][a-z]{3,}\\b");
        //Converts title to normal case
        String normTitle1 = normalizeTitle(title1);
        String normTitle2 = normalizeTitle(title2);

        Set<String> keywords1 = getKeywords(normTitle1);
        Set<String> keywords2 = getKeywords(normTitle2);
        //checks for common elements between the proper nouns of title 1 & 2
        boolean entitiesMatch = !Collections.disjoint(multiWordEntities1, multiWordEntities2) ||
                !Collections.disjoint(singleWordEntities1, singleWordEntities2);
        // Uses Jaccard similarity to calculate and compare the score to threshold
        double keywordSimilarity = calculateJaccardSimilarity(keywords1, keywords2);
        if (entitiesMatch && keywordSimilarity >= ENTITY_KEYWORD_THRESHOLD) return true;
        return keywordSimilarity >= PURE_KEYWORD_THRESHOLD;
    }
    //Makes string to lower case and replaces word numbers by symbols
    private static String normalizeTitle(String title) {
        return title.toLowerCase()
                .replace("one", "1").replace("two", "2").replace("three", "3")
                .replace("four", "4").replace("five", "5").replace("six", "6")
                .replace("seven", "7").replace("eight", "8").replace("nine", "9")
                .replace("ten", "10");
    }
    //gets keywords by getting a word whose length > 2 and which is not in the list of stop words used
    private static Set<String> getKeywords(String normalizedText) {
        return Arrays.stream(normalizedText.replaceAll("[^a-z0-9\\s]", "").split("\\s+"))
                .filter(word -> word.length() > 2 && !STOP_WORDS.contains(word))
                .collect(Collectors.toSet());
    }
    //Returns lowercase list of proper nouns after comparing regex
    private static Set<String> getEntities(String originalCaseTitle, String regex) {
        return Pattern.compile(regex)
                .matcher(originalCaseTitle)
                .results()
                .map(mr -> mr.group().toLowerCase())
                .collect(Collectors.toSet());
    }
    //Uses jaccard similarity defined by J(A, B) = |A ∩ B| / |A ∪ B|
    private static double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        return (double) intersection.size() / union.size();
    }
    private static String sendRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
    //gets the number of unique sources in each cluster
    private static int getSourceDiversity(List<Article> cluster) {
        if (cluster == null || cluster.isEmpty()) return 0;
        return (int) cluster.stream().map(a -> a.sourceName).distinct().count();
    }
    //Categorizes news article by running pattern matching on the url
    public static String categorizeNewsUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "UNKNOWN";
        }
        String normalizedUrl = url.toLowerCase();
        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            String category = entry.getKey();
            List<String> keywords = entry.getValue();
            for (String keyword : keywords) {
                if (stringContains(normalizedUrl, keyword)) {
                    return category;
                }
            }
        }
        return "";
    }
    //Pattern matching of string by brute force method
    private static boolean stringContains(String text, String keyword) {
        if (keyword == null || text == null) {
            return false;
        }
        int textLen = text.length();
        int keywordLen = keyword.length();
        if (keywordLen == 0) {
            return true;
        }
        if (textLen < keywordLen) {
            return false;
        }
        for (int i = 0; i <= textLen - keywordLen; i++) {
            boolean matchFound = true;
            for (int j = 0; j < keywordLen; j++) {
                if (text.charAt(i + j) != keyword.charAt(j)) {
                    matchFound = false;
                    break;
                }
            }
            if (matchFound) {
                return true;
            }
        }
        return false;
    }
    public static List<List<Article>> mergeSort(List<List<Article>> list) {
        if (list.size() <= 1) return list;
        int middle = list.size() / 2;
        List<List<Article>> leftHalf = new ArrayList<>(list.subList(0, middle));
        List<List<Article>> rightHalf = new ArrayList<>(list.subList(middle, list.size()));
        List<List<Article>> sortedLeft = mergeSort(leftHalf);
        List<List<Article>> sortedRight = mergeSort(rightHalf);
        return merge(sortedLeft, sortedRight);
    }

    private static List<List<Article>> merge(List<List<Article>> left, List<List<Article>> right) {
        List<List<Article>> result = new ArrayList<>();
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.size() && rightIndex < right.size()) {
            List<Article> leftCluster = left.get(leftIndex);
            List<Article> rightCluster = right.get(rightIndex);
            int leftDiversity = getSourceDiversity(leftCluster);
            int rightDiversity = getSourceDiversity(rightCluster);
            if (leftDiversity > rightDiversity) {
                result.add(leftCluster);
                leftIndex++;
            } else if (leftDiversity < rightDiversity) {
                result.add(rightCluster);
                rightIndex++;
            } else {
                if (leftCluster.size() >= rightCluster.size()) {
                    result.add(leftCluster);
                    leftIndex++;
                } else {
                    result.add(rightCluster);
                    rightIndex++;
                }
            }
        }
        while (leftIndex < left.size()) {
            result.add(left.get(leftIndex));
            leftIndex++;
        }
        while (rightIndex < right.size()) {
            result.add(right.get(rightIndex));
            rightIndex++;
        }
        return result;
    }
}

/*Above two functions is for the implementation of merge sort, which is modified slightly so as to sort the clusters first based on
 the no. of sources and if there is a tie, it is broken using the cluster with most size*/





