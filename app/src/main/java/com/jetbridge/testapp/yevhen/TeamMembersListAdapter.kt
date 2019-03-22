package com.jetbridge.testapp.yevhen

import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.graphics.PorterDuff
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jetbridge.testapp.yevhen.databinding.ItemTeamMemberBinding
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import java.util.*


class TeamMembersListAdapter(private val data: List<TeamMemberEntity>)
    : RecyclerView.Adapter<TeamMembersListAdapter.ViewHolder>() {

    private val mildColors = listOf(
        R.color.mild_color_2, R.color.mild_color_3,
        R.color.mild_color_5, R.color.mild_color_6,
        R.color.mild_color_8, R.color.mild_color_9,
        R.color.mild_color_11, R.color.mild_color_12,
        R.color.mild_color_14, R.color.mild_color_15)

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
        // colorize avatar
        viewHolder.binding.ivAvatar.setImageResource(R.drawable.ic_person)
        viewHolder.binding.ivAvatar.setBackgroundResource(R.drawable.bg_avatar)
        val avatarColorId = mildColors[teamMember.id.hashCode() % mildColors.size]
        val avatarColor = ResourcesCompat.getColor(viewHolder.itemView.context.resources, avatarColorId, null)
        viewHolder.binding.ivAvatar.imageTintMode = PorterDuff.Mode.MULTIPLY
        viewHolder.binding.ivAvatar.setColorFilter(avatarColor)
        // bind working hours
        val isWorkingNow = isTeamMemberWorkingNow(teamMember.workingHours)

        viewHolder.binding.ivWorkingHours
            .setImageResource(if (isWorkingNow) R.drawable.ic_time else R.drawable.ic_time_off)
        viewHolder.binding.tvWorkingOrNotWorking
            .setText(if (isWorkingNow) R.string.working else R.string.not_working)
        viewHolder.binding.tvWorkingOrNotWorking
            .setTextColor(ResourcesCompat.getColor(viewHolder.itemView.resources,
                if (isWorkingNow) R.color.green_working_hours else R.color.red_not_working_hours, null))
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
