package com.example;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "NewsArticles")
public class NewsArticle {

    @Id
    private String id;

    private String headline;
    private String description;
    private String article;
    private String imageUrl;
    private String date;
    private String category;
    private Float centerBias;
    private Float leftBias;
    private Float rightBias;
    private String source;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getArticle() { return article; }
    public void setArticle(String article) { this.article = article; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Float getCenterBias() { return centerBias; }
    public void setCenterBias(Float centerBias) { this.centerBias = centerBias; }

    public Float getLeftBias() { return leftBias; }
    public void setLeftBias(Float leftBias) { this.leftBias = leftBias; }

    public Float getRightBias() { return rightBias; }
    public void setRightBias(Float rightBias) { this.rightBias = rightBias; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}