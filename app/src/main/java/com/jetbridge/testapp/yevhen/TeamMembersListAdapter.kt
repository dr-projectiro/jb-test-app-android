package com.jetbridge.testapp.yevhen

import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jetbridge.testapp.yevhen.databinding.ItemTeamMemberBinding


class TeamMembersListAdapter(private val data: List<TeamMemberEntity>)
    : RecyclerView.Adapter<TeamMembersListAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = data[position].id.toLong()

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parentView: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(DataBindingUtil.inflate(
            LayoutInflater.from(parentView.context),
            R.layout.item_team_member, parentView, false))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, itemIndex: Int) {
        viewHolder.binding.tvTempName.text = data[itemIndex].firstName
    }

    inner class ViewHolder(val binding: ItemTeamMemberBinding)
        : RecyclerView.ViewHolder(binding.root)
}