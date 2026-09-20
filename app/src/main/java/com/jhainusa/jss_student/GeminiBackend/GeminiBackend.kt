package com.jhainusa.jss_student.GeminiBackend

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.jhainusa.jss_student.RoomDatabase.DaySchedule
import com.jhainusa.jss_student.RoomDatabase.MainVIewModel
import com.jhainusa.jss_student.RoomDatabase.Schedule
import com.jhainusa.jss_student.assignColor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

fun sendImageToSupabase(
    context: Context,
    uri: Uri,
    userIdStr: String,
    username : String,
    viewModel: MainVIewModel,
    onResult: (Boolean) -> Unit
) {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return onResult(false)
    val bytes = inputStream.readBytes()

    val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
    val imagePart = MultipartBody.Part.createFormData("image", "timetable.jpg", requestBody)

    SupabaseClient.api.sendTimetable(userIdStr, username,imagePart).enqueue(object : Callback<String> {
        override fun onResponse(call: Call<String>, response: Response<String>) {
            if (response.isSuccessful && response.body() != null) {
                try {
                    val jsonArray = JSONArray(response.body())
                    val subjectsMap = mutableMapOf<String, MutableList<DaySchedule>>()
                    val teachersMap = mutableMapOf<String, String>()
                    val roomsMap = mutableMapOf<String, String>()

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val day = item.getString("day")
                        val time = item.getString("time")
                        val subject = item.getString("subject")
                        val teacher = item.optString("teacher", "")
                        val roomNo = item.optString("roomNo", "")

                        if (!subjectsMap.containsKey(subject)) {
                            subjectsMap[subject] = mutableListOf()
                            teachersMap[subject] = teacher
                            roomsMap[subject] = roomNo
                        }
                        subjectsMap[subject]?.add(DaySchedule(day, time))
                    }

                    subjectsMap.forEach { (name, schedules) ->
                        viewModel.insertSchedule(
                            Schedule(
                                subject = name,
                                teacher = teachersMap[name] ?: "",
                                roomNo = roomsMap[name] ?: "",
                                scheduleday = schedules,
                                color = assignColor(name).value.toLong()
                            )
                        )
                    }
                    onResult(true)
                } catch (e: Exception) {
                    onResult(false)
                }
            } else if (response.code() == 429) {
                // Handle the 3-request limit reached
                Toast.makeText(context, "Daily limit reached (2 request per day).Try next day", Toast.LENGTH_LONG).show()
                onResult(false)
            } else {
                onResult(false)
            }
        }

        override fun onFailure(call: Call<String>, t: Throwable) {
            onResult(false)
        }
    })
}
