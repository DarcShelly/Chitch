package com.chitch.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Local backup/restore via the Storage Access Framework — the caller launches
 * ACTION_CREATE_DOCUMENT/ACTION_OPEN_DOCUMENT and hands us an already-permissioned [Uri], so no
 * FileProvider or manifest changes are needed. Copies the raw SQLite file rather than exporting
 * JSON: the schema has no shipped users yet, so there's no compatibility surface to protect, and
 * a byte-for-byte copy needs no per-entity (de)serialization code.
 */
object BackupManager {

    suspend fun exportBackup(context: Context, targetUri: Uri) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        // Flush the WAL into the main file so the copy is complete and consistent. This PRAGMA
        // returns a row (busy, log, checkpointed), so it must go through query(), not execSQL()
        // — execSQL() rejects any statement that returns results.
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }

        val dbFile = context.getDatabasePath(DB_NAME)
        context.contentResolver.openOutputStream(targetUri)?.use { out ->
            FileInputStream(dbFile).use { input -> input.copyTo(out) }
        } ?: error("Could not open the chosen file for writing.")
    }

    /** Overwrites the local database with [sourceUri]'s contents, then restarts the app so Room reopens it. */
    suspend fun importBackup(context: Context, sourceUri: Uri) = withContext(Dispatchers.IO) {
        AppDatabase.closeInstance()

        val dbFile = context.getDatabasePath(DB_NAME)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(dbFile).use { out -> input.copyTo(out) }
        } ?: error("Could not open the chosen file for reading.")

        // Stale WAL/SHM sidecars from the old file would otherwise get replayed against the
        // restored one on next open.
        File("${dbFile.path}-wal").delete()
        File("${dbFile.path}-shm").delete()

        restartApp(context)
    }

    private fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Process.killProcess(Process.myPid())
    }

    private const val DB_NAME = "chitch.db"
}
