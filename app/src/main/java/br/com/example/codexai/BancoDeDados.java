package br.com.example.codexai;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                Conversa.class,
                MensagemEntity.class
        },
        version = 1
)
public abstract class BancoDeDados extends RoomDatabase {
    public abstract ConversaDao conversaDao();
    public abstract MensagemDao mensagemDao();
    private static volatile BancoDeDados INSTANCE;
    public static BancoDeDados getDatabase(Context context){
        if(INSTANCE == null){
            synchronized (BancoDeDados.class){
                if(INSTANCE == null){
                    INSTANCE =
                            Room.databaseBuilder(
                                            context.getApplicationContext(),
                                            BancoDeDados.class,
                                            "codexai_db"
                                    )
                                    .fallbackToDestructiveMigration()
                                    .allowMainThreadQueries()
                                    .build();
                }
            }
        }
        return INSTANCE;
    }
}