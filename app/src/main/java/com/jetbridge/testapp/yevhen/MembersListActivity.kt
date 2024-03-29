package com.jetbridge.testapp.yevhen

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.graphics.PorterDuff.*
import android.os.Bundle
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.jetbridge.testapp.yevhen.databinding.ActivityMembersListBinding
import io.reactivex.Completable
import android.widget.ProgressBar
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.RadioButton
import io.reactivex.CompletableEmitter

const val PROGRESS_BAR_INCREMENT_DURATION = 300L

/**
 * MemberListActivity is main application screen.
 *
 * It is split to MemberListActivity (logic closes to gui) and MemberListPresenter (other logic
 * like data loading, taking care about correct state)
 *
 * MemberListPresenter retains a reference to an interface MemberListView,
 * which is implemented by MemberListActivity.
 *
 * The motivation of using interface for view is born as the desire to more clearly see how
 * View (Activity) and Presenter classes are coupled rather then from some proven theory.
 */
@SuppressLint("CheckResult") // suppress warnings that some function return values are not used
class MembersListActivity : AppCompatActivity(), MembersListView {
    private lateinit var binding: ActivityMembersListBinding
    private lateinit var presenter: MembersListPresenter
    private var tuneToCancel: AnimatedVectorDrawableCompat? = null
    private var cancelToTune: AnimatedVectorDrawableCompat? = null
    private lateinit var skillsFilterAdapter: BubbleAdapter<String>
    private lateinit var projectsFilterAdapter: BubbleAdapter<ProjectEntity>

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

        // setup animations
        binding.cntCard.layoutTransition.enableTransitionType(LayoutTransition.APPEARING)
        binding.cvCard.layoutTransition.enableTransitionType(LayoutTransition.APPEARING)

        // let user retry data download when network connection failed
        binding.btnRetry.setOnClickListener { presenter.retryLoadData() }
    }

    override fun onResume() {
        super.onResume()
        // load actual data
        presenter.restart()
    }

    class OnScrollSeparatorVisibilityHandler(val viewVisibleIfScrolled: View) : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            viewVisibleIfScrolled.visibility =
                if (recyclerView.computeVerticalScrollOffset() > 0) View.VISIBLE
                else View.GONE
        }
    }

    // joins radio buttons into a group; among a group only single radio button can be in 'checked' state
    private fun joinRadioButtons(radioButtons: List<RadioButton>) {
        radioButtons.forEach { rb ->
            rb.setOnClickListener {
                radioButtons.minus(rb).forEach { otherRb -> otherRb.isChecked = false }
            }
        }
    }

    override fun displayLoadingProgress(percentage: Float): Completable {
        showNoDataScreen(false)
        return Completable.create { emitter ->
            val viewTagAsEmitter = binding.pbLoadingProgress.tag
            if (viewTagAsEmitter is CompletableEmitter) {
                viewTagAsEmitter.onComplete()
            }
            binding.pbLoadingProgress.tag = emitter
            binding.pbLoadingProgress.visibility = View.VISIBLE
            binding.tvDataLoadingLabel.visibility = View.VISIBLE
            binding.containerRvTeamMembers.visibility = View.GONE
            binding.tvSubtitle.visibility = View.GONE
            binding.upperTeamDataSeparator.visibility = View.GONE
            val anim = ProgressBarAnimation(binding.pbLoadingProgress,
                binding.pbLoadingProgress.progress.toFloat(), percentage * 100)
            anim.duration = PROGRESS_BAR_INCREMENT_DURATION
            anim.fillAfter = true
            anim.fillBefore = true
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationEnd(animation: Animation?) {
                    if (binding.pbLoadingProgress.tag == emitter) {
                        binding.pbLoadingProgress.tag = null
                    }
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
        binding.tvSubtitle.text = buildFilterDescriptionText(filter)
        binding.pbLoadingProgress.visibility = View.GONE
        binding.tvDataLoadingLabel.visibility = View.GONE
        binding.pbLoadingProgress.progress = 0
        binding.rvTeamMembers.adapter = TeamMembersListAdapter(teamMembers, presenter.projects)
        binding.containerRvTeamMembers.visibility = View.VISIBLE
        binding.tvSubtitle.visibility = View.VISIBLE
        binding.rvTeamMembers.isLayoutFrozen = false

        hideDataFailure()
        showNoDataScreen(teamMembers.isEmpty())
    }

    override fun displayDataFailure() {
        binding.pbLoadingProgress.progressDrawable.setColorFilter(
            ResourcesCompat.getColor(resources, R.color.red_not_working, null), Mode.SRC_IN)
        binding.tvDataLoadingLabel.setText(R.string.network_error_please_retry)
        binding.btnRetry.visibility = View.VISIBLE
        displayLoadingProgress(1f).subscribe()
    }

    override fun hideDataFailure() {
        binding.pbLoadingProgress.progressDrawable.setColorFilter(
            ResourcesCompat.getColor(resources, R.color.colorAccent, null), Mode.SRC_IN)
        binding.tvDataLoadingLabel.setText(R.string.your_data_is_loading)
        binding.btnRetry.visibility = View.GONE
    }

    override fun setActionBarButtonsEnabled(enabled: Boolean) {
        binding.btnFilterAccept.isEnabled = enabled
        binding.btnFilterOpenCancel.isEnabled = enabled
    }

    override fun hideFilter() {
        binding.tsHeader.setText(getString(R.string.appbar_header_team_members))
        binding.btnFilterOpenCancel.setImageDrawable(tuneToCancel)
        tuneToCancel?.start()
        binding.btnFilterAccept.visibility = View.GONE
        binding.cntFilter.visibility = View.GONE
        binding.rvTeamMembers.isLayoutFrozen = false
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
        // setup skills recycler view
        skillsFilterAdapter = BubbleAdapter(
            data = skills,
            selectedData = filter.skills ?: emptyList(),
            selectionEnabled = true,
            selectionCallback = { updateSkillRadioButtons(it) })
        projectsFilterAdapter = BubbleAdapter(
            data = projects,
            selectedData = filter.projects ?: emptyList(),
            selectionEnabled = true)
        binding.rvFilterSkills.adapter = skillsFilterAdapter
        binding.rvFilterProjects.adapter = projectsFilterAdapter
        binding.rvTeamMembers.isLayoutFrozen = true
    }

    private fun showNoDataScreen(display: Boolean) {
        val visibility = if (display) View.VISIBLE else View.GONE
        binding.ivNoData.visibility = visibility
        binding.tvNoData.visibility = visibility
    }

    private fun updateSkillRadioButtons(selectedSkills: List<String>) {
        // avoid user confusion: if they selected a skill to filter, then filter should be turned on
        // otherwise user may select some skills and forget to select skill combination rule (OR or AND)
        if (!selectedSkills.isEmpty() && binding.rbFilterNoSkills.isChecked) {
            binding.rbFilterNoSkills.isChecked = false
            binding.rbFilterAnySkills.isChecked = true
        }
    }

    private fun buildFilterDescriptionText(filter: TeamMemberFilter): String {
        val filteringOptions = mutableListOf<Int>().apply {
            if (filter.skillCombinationOperator != null) add(R.string.skills_filter_option)
            if (filter.projects != null && filter.projects.isNotEmpty()) add(R.string.projects_filter_option)
            if (filter.onHolidaysNow != null) add(R.string.holidays_filter_option)
            if (filter.workingNow != null) add(R.string.work_time_filter_option)
        }.map { getString(it) }

        return if (filteringOptions.isEmpty()) getString(R.string.all_data_no_filters)
        else getString(R.string.filtering_by, filteringOptions.reduce { acc, s -> "$acc, $s" })
    }

    // constructs filter object from GUI state
    private fun obtainFilterFromViews(): TeamMemberFilter {
        return TeamMemberFilter(
            onHolidaysNow = if (binding.rbOnHolidaysNow.isChecked) true else null,
            workingNow = if (binding.rbWorkingNow.isChecked) true else null,
            projects = replaceEmptyToNull(projectsFilterAdapter.getSelectedData()),
            skills = replaceEmptyToNull(skillsFilterAdapter.getSelectedData()),
            skillCombinationOperator = when {
                binding.rbFilterAnySkills.isChecked -> BooleanOp.OR
                binding.rbFilterAllSkills.isChecked -> BooleanOp.AND
                else -> null
            })
    }

    // returns null if parameter is and empty list, otherwise it is id transformation
    // motivation of the method: to preserve the following semantics:
    // 1) non-null list in filter means that we use list items for filtering
    // 2) 'null' means that filter is ignored
    // ---
    // within this semantics, emptyList() is not convenient, thus force converted to null
    private fun <T> replaceEmptyToNull(data: List<T>?) =
        if (data?.isEmpty() == true) null else data
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
