package com.jetbridge.testapp.yevhen

import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.graphics.PorterDuff
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jetbridge.testapp.yevhen.databinding.ItemTeamMemberBinding
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import java.util.*

class TeamMembersListAdapter(private val data: List<TeamMemberEntity>,
                             private val projects: List<ProjectEntity>)
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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: ViewHolder, itemIndex: Int) {
        val teamMember = data[itemIndex]
        viewHolder.binding.tvName.text = "${teamMember.firstName} ${teamMember.lastName}"
        viewHolder.binding.tvSkills.text = teamMember.skills.reduce { acc, skill -> "$acc, $skill" }
        colorizeAvatar(viewHolder.binding.ivAvatar, teamMember)
        // bind working hours
        val isWorkingNow = isTeamMemberWorkingNow(teamMember.workingHours)

        viewHolder.binding.ivWorkingHours
            .setImageResource(if (isWorkingNow) R.drawable.ic_time else R.drawable.ic_time_off)
        viewHolder.binding.tvWorkingOrNotWorking
            .setText(if (isWorkingNow) R.string.working else R.string.not_working)
        viewHolder.binding.tvWorkingOrNotWorking
            .setTextColor(ResourcesCompat.getColor(viewHolder.itemView.resources,
                if (isWorkingNow) R.color.green_working_hours else R.color.red_not_working_hours, null))

        viewHolder.binding.cntTeamMember.setOnClickListener {
            DetailedProfileActivity.start(viewHolder.itemView.context,
                teamMember, projects)
        }
    }

    private fun isTeamMemberWorkingNow(workingHours: WorkingHoursEntity): Boolean {
        val currentTimeInWorkerTimeZone = LocalDateTime
            .now(DateTimeZone.forTimeZone(TimeZone.getTimeZone(workingHours.timezone))).toLocalTime()
        val workingHoursStart = LocalTime.parse(workingHours.startLocalIsoTime)
        val workingHoursEnd = LocalTime.parse(workingHours.endLocalIsoTime)

        return currentTimeInWorkerTimeZone.isBefore(workingHoursEnd) &&
            currentTimeInWorkerTimeZone.isAfter(workingHoursStart)
    }

    inner class ViewHolder(val binding: ItemTeamMemberBinding)
        : RecyclerView.ViewHolder(binding.root)
}
