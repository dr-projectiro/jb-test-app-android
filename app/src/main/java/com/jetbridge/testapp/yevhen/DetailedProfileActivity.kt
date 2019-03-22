package com.jetbridge.testapp.yevhen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jetbridge.testapp.yevhen.databinding.ActivityDetailedProfileBinding
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime

const val PROJECTS_KEY = "projects"
const val TEAM_MEMBER_KEY = "team_member"
const val PROFILE_DATE_FORMAT = "MMM dd, YYYY"

@SuppressLint("SetTextI18n")
class DetailedProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailedProfileBinding
    private lateinit var teamMember: TeamMemberEntity
    private lateinit var projects: List<ProjectEntity>
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val encodedTeamMember = intent.getStringExtra(TEAM_MEMBER_KEY)
        val encodedProjects = intent.getStringExtra(PROJECTS_KEY)

        if (encodedTeamMember == null || encodedProjects == null) {
            finish()
            return
        }

        teamMember = gson.fromJson(encodedTeamMember, TeamMemberEntity::class.java)
        projects = gson.fromJson<List<ProjectEntity>>(encodedProjects, object :
            TypeToken<List<ProjectEntity>>() {}.type)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_detailed_profile)

        // display skills data
        binding.tvSkills.text = teamMember.skills.reduce { acc, skill -> "$acc, $skill" }

        // set projects data
        binding.rvProjects.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.HORIZONTAL, false)
        binding.rvProjects.adapter = BubbleAdapter(
            data = projects, selectionEnabled = true, multiSelectionEnabled = false, darkText = true)

        // display manager data
        val managerId = teamMember.managerId
        binding.tvManager.text =
            if (managerId != null) "${managerId.firstName} ${managerId.lastName}"
            else getString(R.string.no_manager)

        // display current project data
        val currentProject = teamMember.currentProject
        binding.tvCurrentProject.text =
            if (currentProject != null) currentProject.projectName
            else getString(R.string.no_project)
        binding.btnChangeProject.setText(
            if (currentProject != null) R.string.change_project
            else R.string.assign_to_project)

        // display avatar and name
        colorizeAvatar(binding.ivAvatar, teamMember)
        binding.tvName.text = "${teamMember.firstName} ${teamMember.lastName}"

        // display availability details
        // working hours
        val workStart = LocalTime.parse(teamMember.workingHours.startLocalIsoTime).toString("HH:mm")
        val workEnd = LocalTime.parse(teamMember.workingHours.endLocalIsoTime).toString("HH:mm")
        binding.tvTime.text = getString(R.string.works_from_till_within_timezone,
            workStart, workEnd, teamMember.workingHours.timezone)
        // holidays
        binding.tvHolidays.text = teamMember.onHolidaysTillIsoDate?.let {
            getString(R.string.on_holidays_until, LocalDateTime.parse(it).toString(PROFILE_DATE_FORMAT))
        } ?: getString(R.string.no_record)

        // free since
        binding.tvFreeSince.text = teamMember.onHolidaysTillIsoDate?.let {
            getString(R.string.free_since, LocalDateTime.parse(it).toString(PROFILE_DATE_FORMAT))
        } ?: getString(R.string.no_record)

        binding.btnBack.setOnClickListener { onBackPressed() }
    }

    companion object {
        fun start(ctx: Context, teamMember: TeamMemberEntity, projects: List<ProjectEntity>) {
            val intent = Intent(ctx, DetailedProfileActivity::class.java)
            val gson = Gson()
            intent.putExtra(PROJECTS_KEY, gson.toJson(projects))
            intent.putExtra(TEAM_MEMBER_KEY, gson.toJson(teamMember))
            ctx.startActivity(intent)
        }
    }
}
