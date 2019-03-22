package com.jetbridge.testapp.yevhen

import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import com.jetbridge.testapp.yevhen.databinding.ActivityMembersListBinding
import io.reactivex.Completable
import android.widget.ProgressBar
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.RadioButton

const val PROGRESS_BAR_INCREMENT_DURATION = 300L

@SuppressLint("CheckResult")
class MembersListActivity : AppCompatActivity(), MembersListView {
    private lateinit var binding: ActivityMembersListBinding
    private lateinit var presenter: MembersListPresenter
    private var tuneToCancel: AnimatedVectorDrawableCompat? = null
    private var cancelToTune: AnimatedVectorDrawableCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_members_list)
        presenter = MembersListPresenter(this)

        binding.rvTeamMembers.layoutManager = LinearLayoutManager(this)
        binding.rvTeamMembers.adapter = TeamMembersListAdapter(emptyList(), emptyList())
        binding.rvTeamMembers.addOnScrollListener(OnScrollSeparatorVisibilityHandler(binding.upperTeamDataSeparator))

        // init animated drawables (switching state of filter actionbar button)
        tuneToCancel = AnimatedVectorDrawableCompat.create(this, R.drawable.anim_tune_to_cancel)
        cancelToTune = AnimatedVectorDrawableCompat.create(this, R.drawable.anim_cancel_to_tune)
        // init appbar header text switching animations
        binding.tsHeader.setInAnimation(this, R.anim.anim_slow_fade_in)
        binding.tsHeader.setOutAnimation(this, R.anim.anim_fast_fade_out)

        binding.btnFilterOpenCancel.setOnClickListener { presenter.onFilterClick() }
        binding.btnFilterAccept.setOnClickListener { presenter.onFilterAcceptClick(obtainFilterFromViews()) }

        // setup radio buttons
        joinRadioButtons(listOf(binding.rbFilterAllSkills,
            binding.rbFilterAnySkills, binding.rbFilterNoSkills))
        joinRadioButtons(listOf(binding.rbOnHolidaysNow,
            binding.rbWorkingNow, binding.rbDontFilterAvailability))

        // setup skills and project filter recycler views
        binding.rvFilterSkills.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFilterProjects.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false)

        // load initial data
        presenter.start()
    }

    class OnScrollSeparatorVisibilityHandler(val viewVisibleIfScrolled: View) : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            viewVisibleIfScrolled.visibility =
                if (recyclerView.computeVerticalScrollOffset() > 0) View.VISIBLE
                else View.INVISIBLE
        }
    }

    private fun joinRadioButtons(radioButtons: List<RadioButton>) {
        radioButtons.forEach { rb ->
            rb.setOnClickListener {
                radioButtons.minus(rb).forEach { otherRb -> otherRb.isChecked = false }
            }
        }
    }

    override fun displayLoadingProgress(percentage: Float): Completable {
        return Completable.create { emitter ->
            binding.pbLoadingProgress.visibility = View.VISIBLE
            binding.tvDataLoadingLabel.visibility = View.VISIBLE
            binding.containerRvTeamMembers.visibility = View.INVISIBLE
            binding.tvSubtitle.visibility = View.INVISIBLE
            binding.upperTeamDataSeparator.visibility = View.INVISIBLE
            val anim = ProgressBarAnimation(binding.pbLoadingProgress,
                binding.pbLoadingProgress.progress.toFloat(), percentage * 100)
            anim.duration = PROGRESS_BAR_INCREMENT_DURATION
            anim.fillAfter = true
            anim.fillBefore = true
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationEnd(animation: Animation?) {
                    emitter.onComplete()
                }

                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationStart(animation: Animation?) {}
            })
            binding.pbLoadingProgress.startAnimation(anim)
        }
    }

    override fun displayTeamMemberItems(teamMembers: List<TeamMemberEntity>, filter: TeamMemberFilter) {
        binding.pbLoadingProgress.clearAnimation()
        binding.pbLoadingProgress.visibility = View.INVISIBLE
        binding.tvDataLoadingLabel.visibility = View.INVISIBLE
        binding.pbLoadingProgress.progress = 0
        binding.rvTeamMembers.adapter = TeamMembersListAdapter(teamMembers, presenter.projects)
        binding.containerRvTeamMembers.visibility = View.VISIBLE
        binding.tvSubtitle.visibility = View.VISIBLE
        binding.tvSubtitle.text = buildFilterDescriptionText(filter)
    }

    override fun setActionBarButtonsEnabled(enabled: Boolean) {
        binding.btnFilterAccept.isEnabled = enabled
        binding.btnFilterOpenCancel.isEnabled = enabled
    }

    override fun hideFilter() {
        binding.tsHeader.setText(getString(R.string.appbar_header_team_members))
        binding.btnFilterOpenCancel.setImageDrawable(tuneToCancel)
        tuneToCancel?.start()
        binding.btnFilterAccept.visibility = View.INVISIBLE
        binding.cntFilter.visibility = View.GONE
    }

    override fun displayFilter(filter: TeamMemberFilter,
                               skills: List<String>,
                               projects: List<ProjectEntity>) {
        binding.tsHeader.setText(getString(R.string.appbar_header_select_filter))
        binding.btnFilterOpenCancel.setImageDrawable(cancelToTune)
        cancelToTune?.start()
        binding.btnFilterAccept.visibility = View.VISIBLE
        binding.cntFilter.visibility = View.VISIBLE
        // skills radio buttons
        binding.rbFilterAllSkills.isChecked = filter.skillCombinationOperator == BooleanOp.AND
        binding.rbFilterAnySkills.isChecked = filter.skillCombinationOperator == BooleanOp.OR
        binding.rbFilterNoSkills.isChecked = filter.skillCombinationOperator == null
        // availability radio buttons
        binding.rbWorkingNow.isChecked = filter.workingNow == true
        binding.rbOnHolidaysNow.isChecked = filter.onHolidaysNow == true
        binding.rbDontFilterAvailability.isChecked =
            filter.workingNow != true && filter.onHolidaysNow != true
        // setup skills recyclerview
        binding.rvFilterSkills.adapter = BubbleAdapter(skills, true)
        binding.rvFilterProjects.adapter = BubbleAdapter(projects, true)
    }

    private fun buildFilterDescriptionText(filter: TeamMemberFilter): String {
        val filteringOptions = mutableListOf<Int>().apply {
            if (filter.skillCombinationOperator != null) add(R.string.skills_filter_option)
            if (filter.projectId != null) add(R.string.projects_filter_option)
            if (filter.onHolidaysNow != null) add(R.string.holidays_filter_option)
            if (filter.workingNow != null) add(R.string.work_time_filter_option)
        }.map { getString(it) }

        return if (filteringOptions.isEmpty()) getString(R.string.all_data_no_filters)
        else getString(R.string.filtering_by, filteringOptions.reduce { acc, s -> "$acc, $s" })
    }

    private fun obtainFilterFromViews(): TeamMemberFilter {
        return TeamMemberFilter()
    }
}

class ProgressBarAnimation(private val progressBar: ProgressBar,
                           private val from: Float,
                           private val to: Float) : Animation() {

    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
        super.applyTransformation(interpolatedTime, t)
        val value = from + (to - from) * interpolatedTime
        progressBar.progress = value.toInt()
    }
}
