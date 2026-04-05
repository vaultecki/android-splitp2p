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
@Database(
    entities = [
        UserProfile::class,
        GroupInfo::class,   // Neu
        User::class,        // Neu
        Expense::class,     // Neu
        Split::class,       // Neu
        Settlement::class   // Neu
    ],
    version = 4 // WICHTIG: Version auf 2 erhöhen!
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun groupDao(): GroupDao // Die abstrakte Methode für das neue DAO
}
