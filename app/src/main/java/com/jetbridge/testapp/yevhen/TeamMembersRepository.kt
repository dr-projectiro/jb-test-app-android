package com.jetbridge.testapp.yevhen

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.lang.IllegalStateException

// safety catch threshold if backend feeds app with unlimited data
const val MAX_PAGE_LIMIT = 15000

class TeamMembersRepository(apiBaseUrl: String) {

    val backendApi = createBackendApi(apiBaseUrl)

    fun getAllProjects() = mergePages { page -> backendApi.getProjectsPage(page).execute() }

    fun getAllTeamMembers(filter: TeamMemberFilter = TeamMemberFilter()) =
        mergePages { page ->
            backendApi.getTeamMembersPage(
                combineSkillsForHttpRequest(filter.skills, filter.skillCombinationOperator),
                filter.projectId, filter.onHolidaysNow, filter.workingNow, page).execute()
        }

    private fun changeTeamMemberProject(teamMemberId: Int, projectId: Int?) =
        Single.just(true)

    private fun createBackendApi(apiBaseUrl: String) = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .baseUrl(apiBaseUrl)
        .client(buildHttpClient())
        .build()
        .create(BackendApi::class.java)

    private fun combineSkillsForHttpRequest(listOfSkills: List<String>?,
                                            skillCombinationOp: BooleanOp?) =
        if (listOfSkills == null)
            emptyList()
        else when (skillCombinationOp) {
            null -> emptyList()
            BooleanOp.AND -> listOfSkills // to get request like /team?skill=a&skill=b&skill=c
            BooleanOp.OR -> listOf(Gson().toJson(listOfSkills)) // to get /team?skill=[a, b, c]
        }

    private fun <T> mergePages(pageProvider: (Int) -> Response<out DataPage<T>>) =
        Observable.create<List<T>> { emitter ->
            var currentPageNumber = 1
            do {
                val pageHttpResponse = pageProvider.invoke(currentPageNumber++)
                if (pageHttpResponse.isSuccessful) {
                    val pageData = pageHttpResponse.body()?.items
                        ?: throw IllegalStateException("Backend provided null data for page: $currentPageNumber")
                    emitter.onNext(pageData)

                    if (currentPageNumber > MAX_PAGE_LIMIT) {
                        throw IllegalStateException("Too much pages: $currentPageNumber")
                    }
                }
            } while (pageHttpResponse.body()?.hasNext == true)
            emitter.onComplete()
        }.subscribeOn(Schedulers.io())

    private fun buildHttpClient() = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor()
            .apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()
}

interface BackendApi {
    @GET("/projects?page")
    fun getProjectsPage(@Query("page") page: Int? = null)
        : Call<ProjectsDataPage>

    @GET("/team")
    fun getTeamMembersPage(
        @Query("skill") skills: List<String> = emptyList(),
        @Query("project") projectId: Int? = null,
        @Query("holidays") onHolidays: Boolean? = null,
        @Query("holidays") isWorkingNow: Boolean? = null,
        @Query("page") page: Int? = null)
        : Call<TeamMembersDataPage>

    @PUT("/team/{teamMemberId}/project")
    fun changeTeamMemberProject(
        @Path("teamMemberId") teamMemberId: Int,
        @Body projectId: Int): Single<Boolean>
}

data class TeamMemberFilter(
    val onHolidaysNow: Boolean? = null,
    val workingNow: Boolean? = null,
    val projectId: Int? = null,
    val skillCombinationOperator: BooleanOp? = null,
    val skills: List<String>? = null)

interface DataPage<T> {
    val items: List<T> get() = throw NotImplementedError()
    val hasPrevious: Boolean get() = throw NotImplementedError()
    val hasNext: Boolean get() = throw NotImplementedError()
    val page: Int get() = throw NotImplementedError()
}

class ProjectsDataPage(
    override val items: List<ProjectEntity>,
    override val hasPrevious: Boolean,
    override val hasNext: Boolean,
    override val page: Int) : DataPage<ProjectEntity>

class TeamMembersDataPage(
    override val items: List<TeamMemberEntity>,
    override val hasPrevious: Boolean,
    override val hasNext: Boolean,
    override val page: Int) : DataPage<TeamMemberEntity>


data class ManagerId(
    val id: Int,

    @SerializedName("first_name")
    val firstName: String,

    @SerializedName("last_name")
    val lastName: String)

data class ProjectEntity(
    val id: Int,

    @SerializedName("project_name")
    val projectName: String) : ReadableEntity {

    override val readableText: String get() = projectName
}

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
