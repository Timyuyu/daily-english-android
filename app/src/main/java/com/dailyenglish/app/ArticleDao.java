package com.dailyenglish.app;

import androidx.room.*;
import androidx.lifecycle.LiveData;
import java.util.List;

@Dao
public interface ArticleDao {
    @Query("SELECT COUNT(*) FROM articles") int count();
    @Query("SELECT * FROM articles ORDER BY createdAt DESC, id DESC") LiveData<List<Article>> observeAll();
    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1") Article get(String id);
    @Query("SELECT title FROM articles ORDER BY createdAt DESC LIMIT 20") List<String> recentTitles();
    @Insert(onConflict = OnConflictStrategy.IGNORE) long insert(Article article);
    @Query("UPDATE articles SET favorite = :value WHERE id = :id") void favorite(String id, boolean value);
    @Query("UPDATE articles SET period = :period WHERE id = :id AND id LIKE 'archive-%'") void classifyImported(String id, String period);
}
