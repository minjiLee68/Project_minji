package com.sophia.project_minji.userPage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.getField
import com.sophia.project_minji.databinding.ActivityUserPageBinding
import com.sophia.project_minji.dataclass.FollowDto
import com.sophia.project_minji.dataclass.FollowUser
import com.sophia.project_minji.utillties.PreferenceManager
import com.sophia.project_minji.viewmodel.FirebaseViewModel
import com.sophia.project_minji.viewmodel.FirebaseViewModelFactory

class UserPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserPageBinding

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var uid: String
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var userId: String

    private val viewModel by viewModels<FirebaseViewModel> {
        FirebaseViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra("followUserId").toString()

        init()
        getFolloerAndFollowing()
        detailUser()
    }

    private fun init() {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()
        preferenceManager = PreferenceManager(applicationContext)
        binding.followBtn.setOnClickListener { requestFollow() }
    }

    private fun detailUser() {
        firestore.collection("Users").document(userId).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result!!.exists()) {
                        val name = task.result!!.getString("name")
                        val image = task.result!!.getString("image")
                        val school = task.result!!.getString("school")
                        val introduce = task.result!!.getString("introduce")
                        val webSite = task.result!!.getString("webSite")
                        binding.nickName.text = name
                        binding.tvUserSchool.text = school
                        binding.tvUserIntroduece.text = introduce
                        binding.tvUserWebSite.text = webSite
                        Glide.with(applicationContext).load(image).into(binding.myProfile)
                    }
                }
            }
    }

    private fun requestFollow() {
        val followingUser = firestore.collection("follow").document(uid)
        firestore.runTransaction {
            var followDto = it.get(followingUser).toObject(FollowDto::class.java)
            if (followDto == null) {
                followDto = FollowDto()
                followDto.followingCount = 1
                followDto.followings[userId] = true
                it.set(followingUser, followDto)

                return@runTransaction
            } else {
                with(followDto) {
                    if (followings.containsKey(userId)) {
                        //언팔로우
                        followingCount -= 1
                        followings.remove(userId)
                        viewModel.deleteFollowUser(userId)
                    } else {
                        //팔로우
                        followingCount += 1
                        followings[userId] = true
                        viewModel.setFollowingUser(userId)
                    }
                }
            }
            it.set(followingUser, followDto)
            return@runTransaction
        }

        val followerUser = firestore.collection("follow").document(userId)
        firestore.runTransaction {
            var followDto = it.get(followerUser).toObject(FollowDto::class.java)
            if (followDto == null) {
                followDto = FollowDto()
                followDto!!.followerCount = 1
                followDto!!.followers[uid] = true

                it.set(followerUser, followDto!!)
                return@runTransaction
            } else {
                with(followDto) {
                    if (this!!.followers.containsKey(uid)) {
                        //언팔로우
                        followerCount -= 1
                        followers.remove(uid)
                        viewModel.deleteFollowUser(uid)
                        binding.followBtn.text = "팔로우"
                    } else {
                        //팔로우
                        followerCount += 1
                        followers[uid] = true
                        viewModel.setFollowingUser(userId)
                        binding.followBtn.text = "언팔로우"
                    }
                }
            }
            it.set(followerUser, followDto!!)
            return@runTransaction
        }
    }

    private fun getFolloerAndFollowing() {
        firestore.collection("follow").document(userId).addSnapshotListener { value, _ ->
            if (value != null) {
                val followDto = value.toObject(FollowDto::class.java)
                if (followDto?.followingCount != null) {
                    binding.following.text = followDto.followingCount.toString()
                }
                if (followDto?.followerCount != null) {
                    binding.follower.text = followDto.followerCount.toString()
                }
            }
        }
    }

}