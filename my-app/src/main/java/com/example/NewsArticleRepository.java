package com.example;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NewsArticleRepository
        extends MongoRepository<NewsArticle, String> {

    List<NewsArticle> findTop20ByCategoryIgnoreCaseOrderByDateDesc(
            String category
    );

    List<NewsArticle> findByDateStartingWith(String date);
}