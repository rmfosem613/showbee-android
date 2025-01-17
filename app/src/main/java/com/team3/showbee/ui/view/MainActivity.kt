package com.team3.showbee.ui.view

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.team3.showbee.NotificationListener
import com.team3.showbee.R
import com.team3.showbee.SharedPref
import com.team3.showbee.data.entity.Schedule
import com.team3.showbee.data.entity.Token
import com.team3.showbee.databinding.ActivityMainBinding
import com.team3.showbee.ui.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() = requireNotNull(_binding)
    private lateinit var viewModel: UserViewModel

    lateinit var financialFragment: FinancialFragment
    lateinit var scheduleFragment: ScheduleFragment
    lateinit var listFragment: ListFragment
    lateinit var fragmentManager: FragmentManager
    lateinit var transaction: FragmentTransaction
    lateinit var scheduleListFragment:ScheduleListFragment
    var triger = "financial"
    var CHANNEL_ID = "0716"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        viewModel =ViewModelProvider(this).get(UserViewModel::class.java)
        setContentView(binding.root)

        if (!permissionGranted()) {
            val intent = Intent(
                "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
            )
            startActivity(intent)
        }

        initView()
        observeData()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = getString(R.string.app_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initView() {
        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("ShowBee")
            .setContentText("유튜브 프리미엄 결제")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("6월 22일 : 10,450원"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        binding.mainNavigationView.setNavigationItemSelectedListener {
            Log.d("Info", "navigaion item click... ${it.title}")
            when(it.title) {
                "나의 계정" -> {
                    val intent = Intent(this, UserAccountActivity::class.java)
                    startActivity(intent)
                }
                "로그아웃" -> {
                    val dialog = LogOutDialog()
                    dialog.setButtonClickListener(object : LogOutDialog.OnButtonClickListener {
                        override fun onLogOutOkClicked() {
                            SharedPref.saveToken(Token("", ""))
                            val intent = Intent(this@MainActivity, LogInActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                    })
                    dialog.show(supportFragmentManager, "CustomDialog")
                }
                "회원탈퇴" -> {
                    val dialog = UserLeaveDialog()
                    dialog.setButtonClickListener(object : UserLeaveDialog.OnButtonClickListener {
                        override fun onLeaveOkClicked() {
                            viewModel.deleteUser()
                        }
                    })
                    dialog.show(supportFragmentManager, "CustomDialog")
                }
                "친구 관리" -> {
                    val intent = Intent(this, SharedScheduleActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }

        setSupportActionBar(binding.include.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.menu)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        fragmentManager = supportFragmentManager
        financialFragment = FinancialFragment()
        scheduleFragment = ScheduleFragment()
        listFragment = ListFragment()
        scheduleListFragment = ScheduleListFragment()

        choiceFragment(triger)

        binding.btnAddExpenseAndIncome.setOnClickListener {
            if (triger == "schedule") {
                val intent = Intent(this, AddIncomeExpenditureActivity::class.java)
                intent.putExtra("mode", true)
                startActivity(intent)
            }
            else {
                val intent = Intent(this, AddFinancialActivity::class.java)
                startActivity(intent)
            }

        }

        binding.floatingActionButton2.setOnClickListener {
            with(NotificationManagerCompat.from(this)) {
                // notificationId is a unique int for each notification that you must define
                notify(0, builder.build())
            }
            choiceFragment(triger)
        }
    }

    fun choiceFragment(tag: String) {
        transaction = fragmentManager.beginTransaction()

        when (tag) {
            "schedule" -> {
                transaction.replace(binding.frameLayout.id, financialFragment).commitAllowingStateLoss()
                triger = "financial"
            }
            "financial" -> {
                transaction.replace(binding.frameLayout.id, scheduleFragment).commitAllowingStateLoss()
                triger = "schedule"
            }
            "list" -> {
                transaction.replace(binding.frameLayout.id, listFragment).commit()
            }
            "scheduleList" -> {
                transaction.replace(binding.frameLayout.id, scheduleListFragment).commit()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home->{ // 메뉴 버튼
                binding.mainDrawerLayout.openDrawer(GravityCompat.START)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun permissionGranted(): Boolean {
        val sets = NotificationManagerCompat.getEnabledListenerPackages(this)
        return sets.contains(packageName)
    }

    private fun observeData() {
        with(viewModel) {
            msg.observe(this@MainActivity) { event ->
                event.getContentIfNotHandled()?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                    if (it=="성공하였습니다.") {
                        val intent = Intent(this@MainActivity, LogInActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                }
            }

            token.observe(this@MainActivity) {
                SharedPref.saveToken(it)
            }
        }
    }
}