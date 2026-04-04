package net.vaultcity.splitp2p

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Die Tabelle
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 0,
    val name: String,
    val publicKeyHex: String
)

// 2. Die Zugriffsrechte
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 0")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)
}

// 3. Die Datenbank-Zentrale
@Database(entities = [UserProfile::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
}
