package net.vaultcity.splitp2p

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
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

@Entity(tableName = "comments_user")
data class Comment(
    @PrimaryKey val id: String,
    val belongs_to: String,      // ID der Expense oder des Settlements
    val group_id: String,        // Optional, aber hilfreich für Performance
    val author_pubkey: String,
    val comment: String,
    val timestamp: Long,
    val is_deleted: Long = 0,
    val lamport_clock: Long,
    val signature: String
)

@Entity(tableName = "attachments")
data class Attachment(
    @PrimaryKey val id: String,
    val belongs_to: String,
    val group_id: String,
    val author_pubkey: String,
    val mime: String,
    val size: Long,
    val sha256: String,
    val filename: String,
    val timestamp: Long,
    val lamport_clock: Long,
    val is_stored: Long = 0,
    val signature: String
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

    @Query("SELECT * FROM group_info WHERE group_id = :groupId")
    fun getGroupByIdFlow(groupId: String): Flow<GroupInfo?>

    @Query("SELECT * FROM expenses WHERE group_id = :groupId ORDER BY timestamp DESC")
    fun getExpensesForGroup(groupId: String): Flow<List<Expense>>

    @Query("SELECT * FROM settlements WHERE group_id = :groupId ORDER BY timestamp DESC")
    fun getSettlementsForGroup(groupId: String): Flow<List<Settlement>>

    @Query("SELECT * FROM users WHERE group_id = :groupId")
    fun getUsersForGroupFlow(groupId: String): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplit(split: Split)

    @Query("SELECT * FROM users WHERE group_id = :groupId")
    suspend fun getUsersInGroup(groupId: String): List<User>

    // Höchster Lamport für Ausgaben in DIESER Gruppe
    @Query("SELECT MAX(lamport_clock) FROM expenses WHERE group_id = :groupId")
    suspend fun getMaxLamportExpenses(groupId: String): Long?

    // Höchster Lamport für Mitglieder-Updates in DIESER Gruppe
    @Query("SELECT MAX(lamport_clock) FROM users WHERE group_id = :groupId")
    suspend fun getMaxLamportUsers(groupId: String): Long?

    // Höchster Lamport für Abrechnungen in DIESER Gruppe
    @Query("SELECT MAX(lamport_clock) FROM settlements WHERE group_id = :groupId")
    suspend fun getMaxLamportSettlements(groupId: String): Long?

    @Query("""
    SELECT MAX(c.lamport_clock)
    FROM comments_user c
    LEFT JOIN expenses e ON c.belongs_to = e.id 
    LEFT JOIN settlements s ON c.belongs_to = s.id 
    WHERE e.group_id = :groupId OR s.group_id = :groupId
    """)
    suspend fun getMaxCommentLamport(groupId: String): Long?

    @Query("""
    SELECT MAX(a.lamport_clock)
    FROM attachments a
    LEFT JOIN expenses e ON a.belongs_to = e.id 
    LEFT JOIN settlements s ON a.belongs_to = s.id 
    WHERE e.group_id = :groupId OR s.group_id = :groupId
    """)
    suspend fun getMaxLamportAttachments(groupId: String): Long?

    @Query("""
        SELECT MAX(s.lamport_clock) 
        FROM split s
        JOIN expenses e ON s.belongs_to = e.id
        WHERE e.group_id = :groupId
    """)
    suspend fun getMaxLamportSplits(groupId: String): Long?

    @androidx.room.Transaction
    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseWithSplitsAndComments(expenseId: String): ExpenseDetailData?
}

data class ExpenseDetailData(
    @Embedded val expense: Expense,
    @Relation(
        parentColumn = "id",
        entityColumn = "belongs_to"
    )
    val splits: List<Split>,
    @Relation(
        parentColumn = "id",
        entityColumn = "belongs_to"
    )
    val comments: List<Comment>
)