package com.dailyenglish.app;

import androidx.annotation.NonNull;
import androidx.room.*;

@Entity(tableName = "articles", indices = {@Index(value = {"date", "period"}, unique = true)})
public class Article {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String date = "";
    @NonNull public String period = "";
    @NonNull public String title = "";
    @NonNull public String english = "";
    @NonNull public String chinese = "";
    @NonNull public String expressions = "[]";
    @NonNull public String source = "";
    public boolean favorite;
    public long createdAt;
}
