package com.terinit.rhythmicreader.app

import android.content.Context
import androidx.room.Room
import com.terinit.rhythmicreader.data.db.ReaderDatabase
import com.terinit.rhythmicreader.data.repository.AndroidPdfDocumentRepository
import com.terinit.rhythmicreader.data.repository.BookRepository
import com.terinit.rhythmicreader.data.repository.DefaultBookRepository
import com.terinit.rhythmicreader.data.repository.PdfDocumentRepository

import com.terinit.rhythmicreader.data.repository.DefaultRecoveryRepository
import com.terinit.rhythmicreader.data.repository.DailyReadingEvidenceRepository
import com.terinit.rhythmicreader.data.repository.DefaultDailyReadingEvidenceRepository
import com.terinit.rhythmicreader.data.repository.RecoveryRepository
import com.terinit.rhythmicreader.data.system.ScreenStateReader
import com.terinit.rhythmicreader.domain.recovery.ActiveReadingTracker
import com.terinit.rhythmicreader.domain.recovery.PageQualificationEngine
import com.terinit.rhythmicreader.domain.recovery.RecoveryCoordinator
import com.terinit.rhythmicreader.domain.time.AndroidMonotonicClock
import com.terinit.rhythmicreader.domain.time.DeviceLocalDateClock
import com.terinit.rhythmicreader.domain.time.MonotonicClock
import com.terinit.rhythmicreader.domain.time.SystemWallClock
import com.terinit.rhythmicreader.domain.time.WallClock
import com.terinit.rhythmicreader.domain.recovery.DailyEvidenceRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(
    context: Context
) {
    val database: ReaderDatabase =
        Room.databaseBuilder(
            context,
            ReaderDatabase::class.java,
            "rhythmic-reader.db"
        ).addMigrations(ReaderDatabase.MIGRATION_2_3, ReaderDatabase.MIGRATION_3_4).build()

    val pdfDocumentRepository: PdfDocumentRepository =
        AndroidPdfDocumentRepository(
            context = context
        )

    val bookRepository: BookRepository =
        DefaultBookRepository(
            bookDao = database.bookDao(),
            contentResolver = context.contentResolver,
            pdfDocumentRepository = pdfDocumentRepository
        )

    val monotonicClock: MonotonicClock = AndroidMonotonicClock
    val wallClock: WallClock = SystemWallClock
    val localDateClock = DeviceLocalDateClock(wallClock)

    val screenStateReader: ScreenStateReader = ScreenStateReader(context)

    val recoveryRepository: RecoveryRepository =
        DefaultRecoveryRepository(
            recoveryDao = database.recoveryDao(),
            contentResolver = context.contentResolver
        )

    val dailyReadingEvidenceRepository: DailyReadingEvidenceRepository =
        DefaultDailyReadingEvidenceRepository(database.dailyReadingEvidenceDao())

    val dailyEvidenceRecorder = DailyEvidenceRecorder(
        repository = dailyReadingEvidenceRepository,
        localDateClock = localDateClock,
        wallClock = wallClock
    )

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val recoveryCoordinator: RecoveryCoordinator =
        RecoveryCoordinator(
            repository = recoveryRepository,
            activeReadingTracker = ActiveReadingTracker(monotonicClock, wallClock),
            pageQualificationEngine = PageQualificationEngine(monotonicClock),
            dailyEvidenceRecorder = dailyEvidenceRecorder,
            localDateClock = localDateClock,
            scope = appScope
        )
}
