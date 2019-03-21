package com.jetbridge.testapp.yevhen

import com.google.gson.annotations.SerializedName

class TeamMembersRepository {

}

class ResponsePage<T : Any>(
    val items: List<T>,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val page: Int)


data class ManagerId(
    val id: Int,

    @SerializedName("first_name")
    val firstName: String,

    @SerializedName("last_name")
    val lastName: String)

data class ProjectEntity(
    val id: Int,

    @SerializedName("project_name")
    val projectName: String)

data class WorkingHoursEntity(
    val timezone: String,

    @SerializedName("start")
    val startLocalIsoTime: String,

    @SerializedName("end")
    val endLocalIsoTime: String)

data class TeamMemberEntity(
    val id: Int,

    val skills: List<String>,

    @SerializedName("first_name")
    val firstName: String,

    @SerializedName("last_name")
    val lastName: String,

    @SerializedName("manager_id")
    val managerId: ManagerId?,

    @SerializedName("on_holidays_till")
    val onHolidaysTillIsoDate: String?, // consider holidays include this date

    @SerializedName("free_since")
    val freeSinceIsoDate: String?,

    @SerializedName("current_project")
    val currentProject: ProjectEntity?,

    @SerializedName("working_hours")
    val workingHours: WorkingHoursEntity) {

    fun convertToManagerId() = ManagerId(id, firstName, lastName)
}

enum class BooleanOp { AND, OR }
