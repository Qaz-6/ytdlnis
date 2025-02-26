package com.deniscerri.ytdlnis.database

import androidx.room.TypeConverter
import com.deniscerri.ytdlnis.database.models.AudioPreferences
import com.deniscerri.ytdlnis.database.models.ChapterItem
import com.deniscerri.ytdlnis.database.models.Format
import com.deniscerri.ytdlnis.database.models.VideoPreferences
import com.deniscerri.ytdlnis.database.viewmodel.DownloadViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class Converters {
    @TypeConverter
    fun stringToListOfFormats(value: String) = Gson().fromJson(value, Array<Format>::class.java).toMutableList()
    @TypeConverter
    fun listOfFormatsToString(list: List<Format?>?) = Gson().toJson(list).toString()

    @TypeConverter
    fun formatToString(format: Format): String = Gson().toJson(format)

    @TypeConverter
    fun stringToFormat(string: String): Format = Gson().fromJson(string, Format::class.java)


    @TypeConverter
    fun typeToString(type: DownloadViewModel.Type) : String = type.toString()
    @TypeConverter
    fun stringToType(string: String) : DownloadViewModel.Type {
        return when(string){
            "audio" -> DownloadViewModel.Type.audio
            "video" -> DownloadViewModel.Type.video
            else -> DownloadViewModel.Type.command
        }
    }

    @TypeConverter
    fun audioPreferencesToString(audioPreferences: AudioPreferences): String = Gson().toJson(audioPreferences)
    @TypeConverter
    fun stringToAudioPreferences(string: String): AudioPreferences = Gson().fromJson(string, AudioPreferences::class.java)

    @TypeConverter
    fun videoPreferencesToString(videoPreferences: VideoPreferences): String = Gson().toJson(videoPreferences)
    @TypeConverter
    fun stringToVideoPreferences(string: String): VideoPreferences = Gson().fromJson(string, VideoPreferences::class.java)

    @TypeConverter
    fun stringToListOfChapters(value: String?) = Gson().fromJson(value, Array<ChapterItem>::class.java).toMutableList()

    @TypeConverter
    fun listOfChaptersToString(list: List<ChapterItem?>?): String {
        val gson = Gson()
        return gson.toJson(list)
    }
}