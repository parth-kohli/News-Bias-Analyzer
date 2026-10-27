
package com.example;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class NewsAPI {

    private final MongoTemplate mongoTemplate;

    public NewsAPI(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // GET /api/news
    // Fetch all main news articles.
    @GetMapping("/news")
    public List<NewsArticle> getAllNewsArticles() {
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "date"));

        return mongoTemplate.find(
                query, NewsArticle.class, "NewsArticles");
    }

    // GET /api/news/{id}/biased
    // Fetch related articles belonging to a main article's MongoDB ID.
    @GetMapping("/news/{id}/biased")
    public List<BiasedArticle> getBiasedArticles(
            @PathVariable String id) {

        Query query = new Query(
                Criteria.where("articleId").is(id));

        query.with(Sort.by(Sort.Direction.DESC, "date"));

        return mongoTemplate.find(
                query, BiasedArticle.class, "BiasedArticles");
    }

    // GET /api/news/search?query=election
    // Search headline, description, and article content.
    @GetMapping("/news/search")
    public List<NewsArticle> searchArticles(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (query == null || query.isBlank()) {
            return List.of();
        }

        // Prevent invalid pagination values.
        page = Math.max(page, 0);
        size = Math.max(1, Math.min(size, 100));

        Pattern pattern = Pattern.compile(
                Pattern.quote(query.trim()),
                Pattern.CASE_INSENSITIVE);

        Criteria keywordCriteria = new Criteria().orOperator(
                Criteria.where("headline").regex(pattern),
                Criteria.where("description").regex(pattern),
                Criteria.where("article").regex(pattern)
        );

        Query searchQuery = new Query(keywordCriteria);
        searchQuery.with(Sort.by(Sort.Direction.DESC, "date"));
        searchQuery.skip((long) page * size);
        searchQuery.limit(size);

        return mongoTemplate.find(
                searchQuery, NewsArticle.class, "NewsArticles");
    }

    // GET /api/news/category?name=Politics
    // Fetch up to 20 articles in a category.
    @GetMapping("/news/category")
    public List<NewsArticle> getArticlesByCategory(
            @RequestParam String name) {

        if (name == null || name.isBlank()) {
            return List.of();
        }

        Query query = new Query(
                Criteria.where("category")
                        .regex("^" + Pattern.quote(name.trim()) + "$",
                                "i"));

        query.with(Sort.by(Sort.Direction.DESC, "date"));
        query.limit(20);

        return mongoTemplate.find(
                query, NewsArticle.class, "NewsArticles");
    }

    // GET /api/news/today
    // Preserves the original behavior of selecting yesterday's news.
    @GetMapping("/news/today")
    public List<NewsArticle> getTodaysNews() {
        String targetDate = LocalDate.now()
                .minusDays(1)
                .toString();

        // Dates are saved as ISO strings, e.g.
        // 2026-10-26T12:30:00Z.
        Query query = new Query(
                Criteria.where("date")
                        .regex("^" + Pattern.quote(targetDate)));

        query.with(Sort.by(Sort.Direction.DESC, "date"));

        return mongoTemplate.find(
                query, NewsArticle.class, "NewsArticles");
    }
}
