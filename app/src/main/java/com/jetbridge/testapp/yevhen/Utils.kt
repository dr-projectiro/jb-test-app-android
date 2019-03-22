package com.jetbridge.testapp.yevhen

import android.graphics.PorterDuff
import android.support.v4.content.res.ResourcesCompat
import android.widget.ImageView


private val mildColors = listOf(
    R.color.mild_color_2, R.color.mild_color_3,
    R.color.mild_color_5, R.color.mild_color_6,
    R.color.mild_color_8, R.color.mild_color_9,
    R.color.mild_color_11, R.color.mild_color_12,
    R.color.mild_color_14, R.color.mild_color_15)

fun colorizeAvatar(imageView: ImageView, teamMember: TeamMemberEntity) {
    imageView.setImageResource(R.drawable.ic_person)
    imageView.setBackgroundResource(R.drawable.bg_avatar)
    val avatarColorId = mildColors[teamMember.id.hashCode() % mildColors.size]
    val avatarColor = ResourcesCompat.getColor(imageView.context.resources, avatarColorId, null)
    imageView.imageTintMode = PorterDuff.Mode.MULTIPLY
    imageView.setColorFilter(avatarColor)
}