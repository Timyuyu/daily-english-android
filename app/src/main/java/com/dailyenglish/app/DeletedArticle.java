package com.dailyenglish.app;

import androidx.annotation.NonNull;
import androidx.room.*;

@Entity(tableName="deleted_articles")
public class DeletedArticle {
    @PrimaryKey @NonNull public String id="";
}
