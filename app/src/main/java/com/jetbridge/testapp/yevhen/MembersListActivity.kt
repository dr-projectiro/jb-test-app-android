package com.jetbridge.testapp.yevhen

import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.jetbridge.testapp.yevhen.databinding.ActivityMembersListBinding
import io.reactivex.Completable
import android.widget.ProgressBar
import android.view.animation.Animation
import android.view.animation.Transformation

const val PROGRESS_BAR_INCREMENT_DURATION = 300L

@SuppressLint("CheckResult")
class MembersListActivity : AppCompatActivity(), MembersListView {

    lateinit var binding: ActivityMembersListBinding
    lateinit var presenter: MembersListPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_members_list)
        presenter = MembersListPresenter(this)

        binding.rvTeamMembers.layoutManager = LinearLayoutManager(this)
        binding.rvTeamMembers.adapter = TeamMembersListAdapter(emptyList())

        presenter.start()
    }

    override fun displayLoadingProgress(percentage: Float): Completable {
        return Completable.create { emitter ->
            binding.pbLoadingProgress.visibility = View.VISIBLE
            binding.tvDataLoadingLabel.visibility = View.VISIBLE
            binding.rvTeamMembers.visibility = View.INVISIBLE
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

    override fun displayTeamMemberItems(teamMembers: List<TeamMemberEntity>) {
        binding.pbLoadingProgress.clearAnimation()
        binding.pbLoadingProgress.visibility = View.INVISIBLE
        binding.tvDataLoadingLabel.visibility = View.INVISIBLE
        binding.pbLoadingProgress.progress = 0
        binding.rvTeamMembers.adapter = TeamMembersListAdapter(teamMembers)
        binding.rvTeamMembers.visibility = View.VISIBLE
    }

    override fun displayFilter(filter: TeamMemberFilter,
                               skills: List<String>,
                               projects: List<ProjectEntity>): Completable {
        return Completable.complete()
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
