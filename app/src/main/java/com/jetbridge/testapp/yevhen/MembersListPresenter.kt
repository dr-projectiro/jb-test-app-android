package com.jetbridge.testapp.yevhen

import android.annotation.SuppressLint
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("CheckResult")
class MembersListPresenter(val view: MembersListView) {

    val repository = MainApp.repository

    fun start() {
        loadDataForFilter(TeamMemberFilter())
    }

    fun loadDataForFilter(filter: TeamMemberFilter) {
        val loadedItems = mutableListOf<TeamMemberEntity>()
        val loadedChunkCount = AtomicInteger(0)
        view.displayLoadingProgress(0f)
        repository.getAllTeamMembers(filter)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapCompletable { dataChunk ->
                loadedItems.addAll(dataChunk)
                loadedChunkCount.incrementAndGet()
                return@flatMapCompletable view
                    .displayLoadingProgress(calculateProgressPercentage(loadedChunkCount.get()))
            }.andThen(view.displayLoadingProgress(1.0f))
            .delay(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { view.displayTeamMemberItems(loadedItems) },
                { TODO("handle errors") })
    }

    private fun calculateProgressPercentage(loadedChunkCount: Int) =
        if (loadedChunkCount < 6) loadedChunkCount * 0.1f // tick 10% for 1-5 chunks
        else if (loadedChunkCount < 20) 0.5f + (loadedChunkCount - 5) * 0.02f // tick 2% for 6-19 chunks
        else 0.8f // stay on 80% until loading is complete
}

interface MembersListView {

    fun displayLoadingProgress(percentage: Float): Completable

    fun displayTeamMemberItems(teamMembers: List<TeamMemberEntity>)

    fun displayFilter(filter: TeamMemberFilter,
                      skills: List<String>,
                      projects: List<ProjectEntity>): Completable
}