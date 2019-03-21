package com.jetbridge.testapp.yevhen

import android.arch.lifecycle.Observer
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.jetbridge.testapp.yevhen.databinding.ActivityMembersListBinding

class MembersListActivity : AppCompatActivity() {

    lateinit var binding: ActivityMembersListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_members_list)

        val adapter = TeamMembersListAdapter()
        binding.rvTeamMembers.layoutManager = LinearLayoutManager(this)
        binding.rvTeamMembers.adapter = adapter

        LivePagedListBuilder(MainApp.repository.getAllTeamMembers(),
            PagedList.Config.Builder()
                .setPageSize(10)
                .setPrefetchDistance(10)
                .setEnablePlaceholders(false)
                .build())
            .build().observe(this, Observer {
                adapter.submitList(it)
            })
    }
}
