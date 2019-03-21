package com.jetbridge.testapp.yevhen

import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jetbridge.testapp.yevhen.databinding.ItemTeamMemberBinding


class TeamMembersListAdapter(val data: List<TeamMemberEntity>)
    : RecyclerView.Adapter<TeamMembersListAdapter.TeamMemberHolder>() {

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parentView: ViewGroup, viewType: Int): TeamMemberHolder {
        return TeamMemberHolder(DataBindingUtil.inflate(
            LayoutInflater.from(parentView.context),
            R.layout.item_team_member, parentView, false))
    }

    override fun onBindViewHolder(viewHolder: TeamMemberHolder, itemIndex: Int) {
        viewHolder.binding.tvTempName.text = data[itemIndex].firstName
    }

    inner class TeamMemberHolder(val binding: ItemTeamMemberBinding)
        : RecyclerView.ViewHolder(binding.root)
}