package com.jetbridge.testapp.yevhen

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.jetbridge.testapp.yevhen.databinding.ActivityMembersListBinding

class MembersListActivity : AppCompatActivity() {

    lateinit var binding: ActivityMembersListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_members_list)


    }
}