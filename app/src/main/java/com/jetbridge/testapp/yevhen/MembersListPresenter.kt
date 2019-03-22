package com.jetbridge.testapp.yevhen

import android.annotation.SuppressLint
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("CheckResult")
class MembersListPresenter(val view: MembersListView) {

    private val repository = MainApp.repository
    private var currentFilter = TeamMemberFilter()
    private var filterOpen = false
    private var dataIsLoading = false
    private var members: List<TeamMemberEntity> = emptyList()
    private var skills: List<String> = emptyList()
    var projects: List<ProjectEntity> = emptyList()
        private set

    fun start() {
        loadDataForFilter(currentFilter)
    }

    fun loadDataForFilter(filter: TeamMemberFilter) {
        view.setActionBarButtonsEnabled(false)
        val loadedMembersData = mutableListOf<TeamMemberEntity>()
        val loadedChunkCount = AtomicInteger(0)
        view.displayLoadingProgress(0f)
        repository.getAllTeamMembers(filter)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapCompletable { dataChunk ->
                loadedMembersData.addAll(dataChunk)
                loadedChunkCount.incrementAndGet()
                return@flatMapCompletable view
                    .displayLoadingProgress(calculateProgressPercentage(loadedChunkCount.get()))
            }.andThen(view.displayLoadingProgress(1.0f))
            .delay(550, TimeUnit.MILLISECONDS)
            .andThen(Single.fromCallable { extractAllDataFromMembersData(loadedMembersData) })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ collectedData ->
                // trying to keep side effects in terminal monad operator only
                val (members, skills, projects) = collectedData
                this.members = members
                this.projects = projects
                this.skills = skills
                view.setActionBarButtonsEnabled(true)
                view.displayTeamMemberItems(members)
            },
                { TODO("handle errors") })
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

    private fun getSkillsData(newMembersData: List<TeamMemberEntity>) = newMembersData
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
}

interface MembersListView {

    fun displayLoadingProgress(percentage: Float): Completable

    fun displayTeamMemberItems(teamMembers: List<TeamMemberEntity>)

    fun displayFilter(filter: TeamMemberFilter,
                      skills: List<String>,
                      projects: List<ProjectEntity>)

    fun hideFilter()

    fun setActionBarButtonsEnabled(enabled: Boolean)
}
