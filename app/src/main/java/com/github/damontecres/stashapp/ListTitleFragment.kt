package com.github.damontecres.stashapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment

class ListTitleFragment : Fragment(R.layout.list_title_view) {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val filterButton = view.findViewById<Button>(R.id.filter_button)
        filterButton.setOnClickListener {
            Toast.makeText(requireContext(), "Filter button clicked!", Toast.LENGTH_SHORT).show()
        }
        filterButton.setOnFocusChangeListener { view: View, focused: Boolean ->
            if (focused) {
                filterButton.setBackgroundColor(requireContext().getColor(R.color.selected_background))
            } else {
                filterButton.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }
}
