package com.droidhats.campuscompass.views

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.droidhats.campuscompass.R
import com.droidhats.campuscompass.viewmodels.ShuttleViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ShuttleFragment : Fragment() {

    private lateinit var shuttleAdapter: ShuttleAdapter
    private lateinit var viewModel: ShuttleViewModel
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.shuttle_fragment, container, false)
        val sideDrawerButton: ImageButton = root.findViewById(R.id.button_menu)
        sideDrawerButton.setOnClickListener {
            requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout).openDrawer(
                GravityCompat.START
            )
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this).get(ShuttleViewModel::class.java)
        super.onViewCreated(view, savedInstanceState)
        shuttleAdapter = ShuttleAdapter(this)
        viewPager = view.findViewById(R.id.pager)
        viewPager.adapter = shuttleAdapter

        val tabLayout: TabLayout = view.findViewById(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            if (position == 0) {
                tab.text = "SGW TO LOY"
            }
            else{
                tab.text = "LOY TO SGW"
            }
        }.attach()
    }
}

class ShuttleAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2 // 1.SGW TO LOY, 2.LOY TO SGW

    override fun createFragment(position: Int): Fragment {
        val fragment = CampusTabFragment()
        fragment.arguments = Bundle().apply {
            putInt(ARG_OBJECT, position)
        }
        return fragment
    }
}

private const val ARG_OBJECT = "object"

class CampusTabFragment : Fragment() {

    private lateinit var viewModel: ShuttleViewModel
    private lateinit var root: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.shuttle_campus_tab, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this).get(ShuttleViewModel::class.java)

        arguments?.takeIf { it.containsKey(ARG_OBJECT) }?.apply {
            if (getInt(ARG_OBJECT) == 0)
                observeSGWShuttle()
            else
                observeLOYShuttle()
        }
    }

    private fun observeSGWShuttle() {
        viewModel.getSGWShuttleTime().observe(viewLifecycleOwner, Observer { sgwShuttleTimes ->
            val table = root.findViewById<TableLayout>(R.id.shuttleTimesTable)
            for (dataRow in sgwShuttleTimes) {
                val tableRow = TableRow(root.context)

                val dayText = TextView(root.context)
                dayText.text = dataRow.shuttle_day

                val timeText = TextView(root.context)
                timeText.text = dataRow.shuttle_time

                dayText.gravity = Gravity.CENTER_HORIZONTAL
                dayText.typeface = Typeface.DEFAULT_BOLD
                timeText.gravity = Gravity.CENTER_HORIZONTAL
                timeText.typeface = Typeface.DEFAULT_BOLD
                tableRow.setPadding(0, 25, 0,25)
                timeText.setBackgroundResource(R.color.colorShuttleRow)

                tableRow.addView(dayText)
                tableRow.addView(timeText)
                table.addView(tableRow)
            }
        })
    }

    private fun observeLOYShuttle() {
        viewModel.getLoyolaShuttleTime().observe(viewLifecycleOwner, Observer { loyShuttleTimes ->
            val table = root.findViewById<TableLayout>(R.id.shuttleTimesTable)
            for (dataRow in loyShuttleTimes) {
                val tableRow = TableRow(root.context)

                val dayText = TextView(root.context)
                dayText.text = dataRow.shuttle_day

                val timeText = TextView(root.context)
                timeText.text = dataRow.shuttle_time

                dayText.gravity = Gravity.CENTER_HORIZONTAL
                dayText.typeface = Typeface.DEFAULT_BOLD
                timeText.gravity = Gravity.CENTER_HORIZONTAL
                timeText.typeface = Typeface.DEFAULT_BOLD
                tableRow.setPadding(0, 25, 0,25)
                timeText.setBackgroundResource(R.color.colorShuttleRow)

                tableRow.addView(dayText)
                tableRow.addView(timeText)
                table.addView(tableRow)
            }
        })
    }
}

