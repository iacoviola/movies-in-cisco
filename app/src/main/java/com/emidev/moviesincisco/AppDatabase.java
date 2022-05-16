package com.emidev.moviesincisco;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {MovieLocation.class},
        version = 1
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MovieLocationDAO movieLocationDAO();
}
