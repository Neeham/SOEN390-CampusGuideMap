package com.droidhats.campuscompass.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "IndoorLocation")
data class IndoorLocation(
        @ColumnInfo(name = "lID") val lID: String,
        @PrimaryKey
        @ColumnInfo(name = "location_name") override val name: String,
        @ColumnInfo(name = "floor_num") val floorNum: String,
        @ColumnInfo(name = "floor_map") val floorMap: String,
        @ColumnInfo(name = "location_type") val type: String,
        @ColumnInfo(name = "building_index") val buildingIndex: Int,
        @ColumnInfo(name = "location_first") val latlat: Double,
        @ColumnInfo(name = "location_second") val lnglng: Double
) :Location() {

    @TypeConverter
    override fun getLocation(): LatLng = LatLng(latlat, lnglng)
    override fun getNextDirections() : List<String> {
        return emptyList()
    }

    fun getFloorNumber(): Int {
        return floorNum.replace('s', '-').toInt()
    }
}