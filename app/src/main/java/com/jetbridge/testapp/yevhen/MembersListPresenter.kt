package com.jetbridge.testapp.yevhen

import android.annotation.SuppressLint
import android.util.Log
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("CheckResult")
class MembersListPresenter(val view: MembersListView) {

    private val repository = MainApp.repository
    private var currentFilter = TeamMemberFilter()
    private var dataIsLoading = false
    private var filterOpen = false
    private var members: List<TeamMemberEntity> = emptyList()
    private var skills: List<String> = emptyList()
    var projects: List<ProjectEntity> = emptyList()
        private set

    fun restart() {
        loadDataForFilter(currentFilter)
    }

    private fun loadDataForFilter(filter: TeamMemberFilter) {
        dataIsLoading = true
        view.setActionBarButtonsEnabled(false)
        val loadedMembersData = mutableListOf<TeamMemberEntity>()
        val loadedChunkCount = AtomicInteger(0)

        view.displayLoadingProgress(0f)
            .andThen(repository.getAllTeamMembers(filter)
                .subscribeOn(Schedulers.io()) // execute io operations on io thread, while obtaining result in GUI thread
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable { dataChunk -> // collect downloaded data and display progress
                    loadedMembersData.addAll(dataChunk)
                    loadedChunkCount.incrementAndGet()
                    return@flatMapCompletable view
                        .displayLoadingProgress(calculateProgressPercentage(loadedChunkCount.get()))
                }.andThen(view.displayLoadingProgress(1.0f)) // display 100% progress (~1.0f)
                .andThen(Single.fromCallable {
                    // it is convenient to parse a bit data fetched from backend:
                    // 1. group projects and user skills
                    // 2. keep team members from the same project grouped together
                    extractAllDataFromMembersData(loadedMembersData)
                }))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ collectedData ->
                dataIsLoading = false
                // trying to keep side effects in terminal monad operator only
                val (members, skills, projects) = collectedData
                this.members = members
                // project list merge is required as long as there is no separate 'GET projects' endpoint
                // if projects are not included in the filter, they won't be available during the next try
                this.projects = this.projects.plus(projects).distinct()
                // the same thing for skills
                this.skills = this.skills.plus(skills).distinct()
                view.setActionBarButtonsEnabled(true)
                view.displayTeamMemberItems(members, currentFilter)
            }, {
                dataIsLoading = false
                view.displayDataFailure()
            })
    }

    private fun extractAllDataFromMembersData(newMembersData: List<TeamMemberEntity>) =
        Triple(
            formatMembersData(newMembersData),
            getSkillsData(newMembersData),
            getProjectsData(newMembersData))

    private fun getProjectsData(newMembersData: List<TeamMemberEntity>) = newMembersData
        .map { it.currentProject }
        .distinct()
        .filter { it != null }
        .map { it!! }

    private fun getSkillsData(newMembersData: List<TeamMemberEntity>) =
        if (newMembersData.isEmpty()) emptyList()
        else newMembersData
            .map { it.skills }
            .reduce { list1, list2 -> list1.plus(list2) }
            .distinct()

    private fun formatMembersData(newMembersData: List<TeamMemberEntity>) = newMembersData
        .sortedBy { it.currentProject?.id ?: Int.MAX_VALUE }

    private fun calculateProgressPercentage(loadedChunkCount: Int) =
        if (loadedChunkCount < 6) loadedChunkCount * 0.1f // tick 10% for 1-5 chunks
        else if (loadedChunkCount < 20) 0.5f + (loadedChunkCount - 5) * 0.02f // tick 2% for 6-19 chunks
        else 0.8f // stay on 80% until loading is complete

    fun onFilterAcceptClick(newFilter: TeamMemberFilter) {
        filterOpen = false
        view.hideFilter()
        currentFilter = newFilter
        loadDataForFilter(currentFilter)
    }

    fun onFilterClick() {
        filterOpen = !filterOpen
        if (filterOpen) view.displayFilter(currentFilter, skills, projects)
        else view.hideFilter()
    }

    fun retryLoadData() {
        if (!dataIsLoading) {
            loadDataForFilter(currentFilter)
            view.hideDataFailure()
        }
    }
}

/**
 * Interface of View visible to Presenter
 */
interface MembersListView {

    fun displayLoadingProgress(percentage: Float): Completable

    fun displayTeamMemberItems(teamMembers: List<TeamMemberEntity>, filter: TeamMemberFilter)

    fun displayFilter(filter: TeamMemberFilter,
                      skills: List<String>,
                      projects: List<ProjectEntity>)

    fun hideFilter()

    fun setActionBarButtonsEnabled(enabled: Boolean)

    fun displayDataFailure()
    fun hideDataFailure()
}
