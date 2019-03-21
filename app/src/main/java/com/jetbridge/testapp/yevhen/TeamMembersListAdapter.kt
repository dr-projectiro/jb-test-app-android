package com.jetbridge.testapp.yevhen

import android.arch.paging.PagedListAdapter
import android.databinding.DataBindingUtil
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jetbridge.testapp.yevhen.databinding.ItemTeamMemberBinding


class TeamMembersListAdapter
    : PagedListAdapter<TeamMemberEntity, TeamMembersListAdapter.TeamMemberHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parentView: ViewGroup, viewType: Int): TeamMemberHolder {
        return TeamMemberHolder(DataBindingUtil.inflate(
            LayoutInflater.from(parentView.context),
            R.layout.item_team_member, parentView, false))
    }

    override fun onBindViewHolder(viewHolder: TeamMemberHolder, itemIndex: Int) {
        viewHolder.binding.tvTempName.text = getItem(itemIndex)?.firstName ?: "NULL"
    }

    inner class TeamMemberHolder(val binding: ItemTeamMemberBinding)
        : RecyclerView.ViewHolder(binding.root)

    companion object {

        val DIFF_CALLBACK: DiffUtil.ItemCallback<TeamMemberEntity> =
            object : DiffUtil.ItemCallback<TeamMemberEntity>() {
                override fun areItemsTheSame(
                    item1: TeamMemberEntity, item2: TeamMemberEntity): Boolean {
                    return item1.id == item2.id
                }

                override fun areContentsTheSame(
                    item1: TeamMemberEntity, item2: TeamMemberEntity): Boolean {
                    return item1 == item2
                }
            }
    }
}