package com.example;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface BiasedArticleRepository
        extends MongoRepository<BiasedArticle, String> {

    List<BiasedArticle> findByArticleId(String articleId);
}