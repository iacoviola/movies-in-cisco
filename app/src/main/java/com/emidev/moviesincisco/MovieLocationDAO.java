package com.emidev.moviesincisco;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MovieLocationDAO {
    @Query("SELECT * FROM movie_location")
    List<MovieLocation> getAll();

    @Query("SELECT * FROM movie_location WHERE id IN (:movieIds)")
    List<MovieLocation> loadAllByIds(int[] movieIds);

    @Query("SELECT * FROM movie_location WHERE title LIKE :title LIMIT 1")
    MovieLocation findByName(String title);

    @Insert
    void insert(MovieLocation movieLocations);

    @Update
    void update(MovieLocation movieLocations);

    @Delete
    void delete(MovieLocation movieLocations);
}
