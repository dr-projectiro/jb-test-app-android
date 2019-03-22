package com.jetbridge.testapp.yevhen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jetbridge.testapp.yevhen.databinding.ActivityDetailedProfileBinding
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import java.lang.IllegalStateException

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
        // this is the screen displaying user profile details
        // team member data and all available project are obtained as screen params
        val encodedTeamMember = intent.getStringExtra(TEAM_MEMBER_KEY)
        val encodedProjects = intent.getStringExtra(PROJECTS_KEY)

        if (encodedTeamMember == null || encodedProjects == null) {
            finish()
            return
        }

        // decode params from json to data class objects
        teamMember = gson.fromJson(encodedTeamMember, TeamMemberEntity::class.java)
        projects = gson.fromJson<List<ProjectEntity>>(encodedProjects, object :
            TypeToken<List<ProjectEntity>>() {}.type)

        // create GUI bindings
        binding = DataBindingUtil.setContentView(this, R.layout.activity_detailed_profile)

        displayProfileData()

        binding.btnBack.setOnClickListener { onBackPressed() }
        binding.btnChangeProject.setOnClickListener { onChangeProjectPressed() }
    }

    private fun displayProfileData() {
        // display skills data of the team member
        binding.tvSkills.text = teamMember.skills.reduce { acc, skill -> "$acc, $skill" }

        // prepare projects data to display later
        binding.rvProjects.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.HORIZONTAL, false)
        binding.rvProjects.adapter = initProjectsAdapter()

        // display who's manager of the team member
        val managerId = teamMember.managerId
        binding.tvManager.text =
            if (managerId != null) "${managerId.firstName} ${managerId.lastName}"
            else getString(R.string.no_manager)

        // display current project data
        val currentProject = teamMember.currentProject
        displayProject(currentProject)
        binding.btnChangeProject.setText(
            if (currentProject != null) R.string.change_project
            else R.string.assign_to_project)

        // display avatar and name
        colorizeAvatar(binding.ivAvatar, teamMember)
        binding.tvName.text = "${teamMember.firstName} ${teamMember.lastName}"

        displayAvailabilityData()
    }

    private fun displayAvailabilityData() {
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
        binding.tvFreeSince.text = teamMember.freeSinceIsoDate?.let {
            getString(R.string.free_since, LocalDateTime.parse(it).toString(PROFILE_DATE_FORMAT))
        } ?: getString(R.string.no_record)
    }

    private fun displayProject(project: ProjectEntity?) {
        binding.tvCurrentProject.text = project?.projectName ?: getString(R.string.no_project)
    }

    private fun initProjectsAdapter() = BubbleAdapter(
        data = projects, selectionEnabled = true, multiSelectionEnabled = false,
        darkText = true, selectionCallback = { onProjectSelected(it) })

    private fun onProjectSelected(listWithSingleProject: List<ProjectEntity>) {
        if (listWithSingleProject.isEmpty()) {
            return // it's possible that user de-selected project
        }
        if (listWithSingleProject.size > 1) {
            throw IllegalStateException("Project adapter should deny multi selection")
        }
        val selectedProject = listWithSingleProject[0]

        // build confirmation dialog
        val dialog = MaterialDialog.Builder(this)
            .positiveText(R.string.yes_button)
            .negativeText(R.string.cancel_button)
            .title(R.string.transfer_to_project)
            .content(getString(R.string.are_you_sure_to_transfer_x_to_y,
                teamMember.firstName + " " + teamMember.lastName, selectedProject.projectName))
            .onPositive { _, _ ->
                resetProjectList()
                binding.btnChangeProject.setText(R.string.changing)
                MainApp.repository.changeTeamMemberProject(teamMember.id, selectedProject.id)
                    .subscribe({
                        displayProject(selectedProject)
                        binding.btnChangeProject.setText(R.string.project_changed_successfully)
                    }, {
                        binding.btnChangeProject.setText(R.string.failed_to_change_retry)
                    })
            }
            .onNegative { _, _ -> resetProjectList() }
            .build()

        dialog.show()
    }

    private fun resetProjectList() {
        binding.rvProjects.visibility = View.GONE
        binding.rvProjects.adapter = initProjectsAdapter()
    }

    private fun onChangeProjectPressed() {
        binding.rvProjects.visibility = View.VISIBLE
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
