package com.droidhats.campuscompass.roomdb

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

/**
 * This class will create the ShuttleBus_SGW_Entity table
 * @param ID
 * @param SHUTTLE_DAY
 * @param SHUTTLE_TIME
 */
@Entity()
data class ShuttleBus_SGW_Entity(

    @PrimaryKey
    @NotNull
    @ColumnInfo(name = "ID")
    var ID: Int,

    @ColumnInfo(name ="SHUTTLE_DAY")
    @NotNull
    var shuttle_day: String= "",

    @ColumnInfo(name ="SHUTTLE_TIME")
    @NotNull
    var shuttle_time: String= ""
) {}