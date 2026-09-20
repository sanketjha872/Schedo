package com.jhainusa.jss_student

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jhainusa.jss_student.RoomDatabase.MainVIewModel
import com.jhainusa.jss_student.RoomDatabase.Schedule
import com.jhainusa.jss_student.UserPref.NameViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MoreOptionsViewModel(application: Application) : AndroidViewModel(application) {

    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog = _showFeedbackDialog.asStateFlow()

    private val _showAttendanceDialog = MutableStateFlow(false)
    val showAttendanceDialog = _showAttendanceDialog.asStateFlow()

    private val _showNameDialog = MutableStateFlow(false)
    val showNameDialog = _showNameDialog.asStateFlow()

    private val _feedbackType = MutableStateFlow("")
    val feedbackType = _feedbackType.asStateFlow()

    fun showFeedbackDialog(type: String) {
        _feedbackType.value = type
        _showFeedbackDialog.value = true
    }

    fun dismissFeedbackDialog() {
        _showFeedbackDialog.value = false
    }

    fun showAttendanceDialog() {
        _showAttendanceDialog.value = true
    }

    fun dismissAttendanceDialog() {
        _showAttendanceDialog.value = false
    }

    fun showNameDialog() {
        _showNameDialog.value = true
    }

    fun dismissNameDialog() {
        _showNameDialog.value = false
    }

    fun toggleDarkMode(nameViewModel: NameViewModel, enabled: Boolean) {
        nameViewModel.setDarkMode(enabled)
    }

    fun toggleNotifications(nameViewModel: NameViewModel, enabled: Boolean) {
        nameViewModel.setNotificationsEnabled(enabled)
    }

    fun saveDesiredAttendance(nameViewModel: NameViewModel, attendance: Float) {
        nameViewModel.saveDesiredAttendance(attendance)
        dismissAttendanceDialog()
    }

    fun saveName(nameViewModel: NameViewModel, name: String) {
        nameViewModel.saveName(name)
        dismissNameDialog()
    }

    fun submitFeedback(
        context: Context,
        nameViewModel: NameViewModel,
        type: String,
        message: String
    ) {
        nameViewModel.sendFeedback(
            type = type,
            message = message,
            onSuccess = {
                Toast.makeText(context, "Feedback sent! Thank you.", Toast.LENGTH_SHORT).show()
                dismissFeedbackDialog()
            },
            onError = { error ->
                Toast.makeText(context, "Failed to send: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    fun openInstagram(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/schedo010/"))
        context.startActivity(intent)
    }

    fun openPlayStore(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.jhainusa.jss_student"))
        context.startActivity(intent)
    }

    fun openPrivacyPolicy(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sanketjha872.github.io/schedo-privacy-policy/"))
        context.startActivity(intent)
    }

    fun shareApp(context: Context) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out Schedo, an amazing app for managing your attendance! https://play.google.com/store/apps/details?id=com.jhainusa.jss_student")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Schedo via"))
    }

    fun exportSchedule(
        context: Context,
        subjectsList: List<Schedule>,
        userName: String?
    ) {
        if (subjectsList.isEmpty()) {
            Toast.makeText(context, "No subjects found to share", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            try {
                val json = Gson().toJson(subjectsList)
                val fileName = "${userName?.trim()?.ifEmpty { "My" } ?: "My"} Schedule.schedo"
                val cacheFile = File(context.cacheDir, fileName)
                FileOutputStream(cacheFile).use { it.write(json.toByteArray()) }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    cacheFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Schedule File"))
            } catch (e: Exception) {
                Toast.makeText(context, "Error sharing schedule", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun importSchedule(
        context: Context,
        uri: Uri,
        mainViewModel: MainVIewModel
    ) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val json = inputStream.bufferedReader().use { reader -> reader.readText() }
                    val listType = object : TypeToken<List<Schedule>>() {}.type
                    val importedList: List<Schedule> = Gson().fromJson(json, listType)

                    if (importedList.isNullOrEmpty()) {
                        Toast.makeText(context, "The file is empty", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    importedList.forEach { importedSub ->
                        mainViewModel.insertSchedule(
                            Schedule(
                                subject = importedSub.subject,
                                teacher = importedSub.teacher,
                                scheduleday = importedSub.scheduleday,
                                color = importedSub.color,
                                totalClasses = importedSub.totalClasses
                            )
                        )
                    }
                    Toast.makeText(context, "Schedule imported successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Invalid or corrupted schedule file", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
