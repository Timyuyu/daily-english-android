package com.dailyenglish.app;

import androidx.room.*;
import androidx.lifecycle.LiveData;
import java.util.List;

@Dao
public interface ArticleDao {
    @Query("SELECT COUNT(*) FROM articles") int count();
    @Query("SELECT * FROM articles ORDER BY createdAt DESC, id DESC") List<Article> all();
    @Query("SELECT * FROM articles ORDER BY createdAt DESC, id DESC") LiveData<List<Article>> observeAll();
    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1") Article get(String id);
    @Query("SELECT title FROM articles ORDER BY createdAt DESC LIMIT 20") List<String> recentTitles();
    @Query("SELECT EXISTS(SELECT 1 FROM deleted_articles WHERE id = :id)") boolean deleted(String id);
    @Insert(onConflict = OnConflictStrategy.IGNORE) void markDeleted(DeletedArticle article);
    @Query("DELETE FROM articles WHERE id = :id") void deleteRow(String id);
    @Insert(onConflict = OnConflictStrategy.IGNORE) long insertRow(Article article);
    @Transaction default long insert(Article article) { return deleted(article.id)?-1:insertRow(article); }
    @Transaction default void remove(String id) {
        DeletedArticle marker=new DeletedArticle();marker.id=id;markDeleted(marker);deleteRow(id);
    }
    @Query("UPDATE articles SET favorite = :value WHERE id = :id") void favorite(String id, boolean value);
    @Query("UPDATE articles SET period = :period WHERE id = :id AND id LIKE 'archive-%'") void classifyImported(String id, String period);
}
