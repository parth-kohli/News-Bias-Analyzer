
package com.example;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InsertClustersIntoDB {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context =
                     new SpringApplicationBuilder(NewsAPIApplication.class)
                             .web(WebApplicationType.NONE)
                             .run(args)) {

            MongoTemplate mongoTemplate = context.getBean(MongoTemplate.class);

            System.out.println("Training bias classification models...");
            ModelTrainer.TrainedModels trainedModels = ModelTrainer.trainModels();

            System.out.println("Fetching and clustering articles...");
            List<List<Article>> clusters =
                    fetchMultipleArticles.fetchAndClusterArticles(trainedModels);

            insertClusters(mongoTemplate, clusters);

            System.out.println("Finished saving article clusters to MongoDB.");

        } catch (Exception e) {
            System.err.println("Article pipeline failed.");
            e.printStackTrace();
        }
    }

    private static void insertClusters(
            MongoTemplate mongoTemplate,
            List<List<Article>> clusters) {

        int eventCounter = 1;

        for (List<Article> cluster : clusters) {
            if (cluster == null || cluster.isEmpty()) {
                continue;
            }

            Article mainArticle = cluster.get(0);

            // Convert the main article into the MongoDB document model.
            NewsArticle newsArticle = createNewsArticle(mainArticle);

            // Save it first so MongoDB generates its ID.
            mongoTemplate.save(newsArticle, "NewsArticles");

            String mainArticleId = newsArticle.getId();

            if (mainArticleId == null || mainArticleId.isBlank()) {
                throw new IllegalStateException(
                        "MongoDB did not generate an ID for the main article.");
            }

            System.out.printf(
                    "%nSaved Event #%d: '%s'%n",
                    eventCounter++, mainArticle.title);

            System.out.printf(
                    "   -> Scores: [L:%.2f, C:%.2f, R:%.2f]%n",
                    mainArticle.leftBias,
                    mainArticle.centerBias,
                    mainArticle.rightBias);

            int savedRelatedArticles = 0;

            // Save the remaining articles as related/biased articles.
            for (int i = 1; i < cluster.size(); i++) {
                Article article = cluster.get(i);

                BiasedArticle biasedArticle = createBiasedArticle(article);

                // Link this article to its main article using the MongoDB ID.
                biasedArticle.setArticleId(mainArticleId);

                mongoTemplate.save(biasedArticle, "BiasedArticles");
                savedRelatedArticles++;
            }

            System.out.println(
                    "   -> Saved " + savedRelatedArticles
                            + " related (biased) articles.");
        }
    }

    private static NewsArticle createNewsArticle(Article article) {
        NewsArticle document = new NewsArticle();

        document.setHeadline(clean(article.title));
        document.setDescription(clean(article.description));
        document.setArticle(getContent(article));
        document.setImageUrl(clean(article.imageUrl));
        document.setDate(normalizeDate(article.publishedAt));
        document.setCategory(getCategory(article.category));
        document.setCenterBias(article.centerBias);
        document.setLeftBias(article.leftBias);
        document.setRightBias(article.rightBias);
        document.setSource(clean(article.sourceName));

        return document;
    }

    private static BiasedArticle createBiasedArticle(Article article) {
        BiasedArticle document = new BiasedArticle();

        document.setHeadline(clean(article.title));
        document.setArticle(getContent(article));
        document.setDescription(clean(article.description));
        document.setImageUrl(clean(article.imageUrl));
        document.setDate(normalizeDate(article.publishedAt));
        document.setCategory(getCategory(article.category));
        document.setCenterBias(article.centerBias);
        document.setLeftBias(article.leftBias);
        document.setRightBias(article.rightBias);
        document.setSource(clean(article.sourceName));

        return document;
    }

    private static String getContent(Article article) {
        String content = clean(article.content);

        return content == null
                ? "No content snippet available."
                : content;
    }

    private static String getCategory(String category) {
        String value = clean(category);
        return value == null ? "General" : value;
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()
                || value.trim().equalsIgnoreCase("null")) {
            return null;
        }

        return value.trim();
    }

    private static String normalizeDate(String publishedAt) {
        if (publishedAt == null || publishedAt.isBlank()
                || publishedAt.trim().equalsIgnoreCase("null")) {
            return null;
        }

        try {
            // NewsAPI normally supplies an ISO date-time with an offset.
            return OffsetDateTime.parse(
                    publishedAt,
                    DateTimeFormatter.ISO_DATE_TIME
            ).toInstant().toString();

        } catch (Exception ignored) {
            try {
                return Instant.parse(publishedAt).toString();
            } catch (Exception e) {
                System.err.println(
                        "Could not parse article date: " + publishedAt);
                return publishedAt;
            }
        }
    }
}
