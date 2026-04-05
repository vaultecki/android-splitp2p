package net.vaultcity.splitp2p

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "users",
    primaryKeys = ["public_key", "group_id"] // Hier definierst du den zusammengesetzten Key
)
data class User(
    val public_key: String,
    val group_id: String,
    val name: String,
    val timestamp: Long,
    val lamport_clock: Long,
    val signature: String
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String,
    val group_id: String,
    val timestamp: Long,
    val expense_date: Long,
    val lamport_clock: Long,
    val author_pubkey: String,
    val is_deleted: Long = 0,
    val amount: Long, // In Python als INTEGER gespeichert (meist Cents)
    val description: String?,
    val category: String?,
    val original_amount: Long?,
    val original_currency: String?,
    val signature: String
)

@Entity(tableName = "split")
data class Split(
    @PrimaryKey val id: String,
    val belongs_to: String, // Fremdschlüssel zu Expense.id
    val timestamp: Long,
    val lamport_clock: Long,
    val author_pubkey: String,
    val payer_key: String,
    val debtor_key: String,
    val amount: Long,
    val signature: String
)

@Entity(tableName = "settlements")
data class Settlement(
    @PrimaryKey val id: String,
    val group_id: String,
    val timestamp: Long,
    val lamport_clock: Long,
    val author_pubkey: String,
    val is_deleted: Long = 0,
    val from_key: String,
    val to_key: String,
    val amount: Long,
    val signature: String
)

@Entity(tableName = "group_info")
data class GroupInfo(
    @PrimaryKey val group_id: String,
    val name: String,
    val currency: String,
    val group_key: String // Hex-String aus Python
)

@Dao
interface GroupDao {
    @Query("SELECT * FROM group_info ORDER BY name ASC")
    fun getAllGroupsFlow(): Flow<List<GroupInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("DELETE FROM group_info WHERE group_id = :groupId")
    suspend fun deleteGroupById(groupId: String)
}
