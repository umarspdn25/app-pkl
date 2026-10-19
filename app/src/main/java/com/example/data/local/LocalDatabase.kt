package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.data.model.AttendanceRecord
import com.example.data.model.Student
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val nisn: String,
    val name: String,
    val className: String,
    val pklPlace: String,
    val pklLat: Double,
    val pklLng: Double,
    val radiusMeters: Double,
    val accountStatus: String,
    val profilePhotoUrl: String
) {
    fun toDomain(): Student = Student(
        nisn = nisn,
        name = name,
        className = className,
        pklPlace = pklPlace,
        pklLat = pklLat,
        pklLng = pklLng,
        radiusMeters = radiusMeters,
        accountStatus = accountStatus,
        profilePhotoUrl = profilePhotoUrl
    )

    companion object {
        fun fromDomain(student: Student): StudentEntity = StudentEntity(
            nisn = student.nisn,
            name = student.name,
            className = student.className,
            pklPlace = student.pklPlace,
            pklLat = student.pklLat,
            pklLng = student.pklLng,
            radiusMeters = student.radiusMeters,
            accountStatus = student.accountStatus,
            profilePhotoUrl = student.profilePhotoUrl
        )
    }
}

@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val serverTimestamp: String,
    val date: String,
    val time: String,
    val nisn: String,
    val studentName: String,
    val attendanceType: String,
    val attendanceStatus: String,
    val photoUrl: String,
    val latitude: Double,
    val longitude: Double,
    val gpsAccuracy: Double,
    val distanceMeters: Double,
    val notes: String,
    val validationStatus: String,
    val adminNotes: String,
    val isPendingSync: Boolean = false,
    val localPhotoPath: String = ""
) {
    fun toDomain(): AttendanceRecord = AttendanceRecord(
        id = id,
        serverTimestamp = serverTimestamp,
        date = date,
        time = time,
        nisn = nisn,
        studentName = studentName,
        attendanceType = attendanceType,
        attendanceStatus = attendanceStatus,
        photoUrl = if (localPhotoPath.isNotEmpty()) localPhotoPath else photoUrl,
        latitude = latitude,
        longitude = longitude,
        gpsAccuracy = gpsAccuracy,
        distanceMeters = distanceMeters,
        notes = notes,
        validationStatus = validationStatus,
        adminNotes = adminNotes,
        isPendingSync = isPendingSync
    )

    companion object {
        fun fromDomain(record: AttendanceRecord, localPhotoPath: String = ""): AttendanceEntity = AttendanceEntity(
            id = record.id,
            serverTimestamp = record.serverTimestamp,
            date = record.date,
            time = record.time,
            nisn = record.nisn,
            studentName = record.studentName,
            attendanceType = record.attendanceType,
            attendanceStatus = record.attendanceStatus,
            photoUrl = record.photoUrl,
            latitude = record.latitude,
            longitude = record.longitude,
            gpsAccuracy = record.gpsAccuracy,
            distanceMeters = record.distanceMeters,
            notes = record.notes,
            validationStatus = record.validationStatus,
            adminNotes = record.adminNotes,
            isPendingSync = record.isPendingSync,
            localPhotoPath = localPhotoPath
        )
    }
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE nisn = :nisn LIMIT 1")
    suspend fun getStudentByNisn(nisn: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Query("SELECT * FROM students")
    fun getAllStudents(): Flow<List<StudentEntity>>
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE nisn = :nisn ORDER BY serverTimestamp DESC")
    fun getAttendanceByStudent(nisn: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE nisn = :nisn AND date = :date")
    suspend fun getTodayAttendance(nisn: String, date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance_records ORDER BY serverTimestamp DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE isPendingSync = 1")
    suspend fun getPendingSyncRecords(): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceEntity>)

    @Query("UPDATE attendance_records SET isPendingSync = 0, photoUrl = :remotePhotoUrl WHERE id = :id")
    suspend fun markAsSynced(id: String, remotePhotoUrl: String)
}

@Database(entities = [StudentEntity::class, AttendanceEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pkl_tsm_database"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
