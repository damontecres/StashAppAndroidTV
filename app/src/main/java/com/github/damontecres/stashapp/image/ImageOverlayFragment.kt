package com.github.damontecres.stashapp.image

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.damontecres.stashapp.R

class ImageOverlayFragment : Fragment(R.layout.image_overlay) {
    private val viewModel: ImageViewModel by activityViewModels<ImageViewModel>()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val titleView = view.findViewById<TextView>(R.id.image_view_title)

        val zoomInButton = view.findViewById<Button>(R.id.zoom_in_button)
        zoomInButton.setOnClickListener {
            viewModel.imageController?.zoomIn()
        }

        val zoomOutButton = view.findViewById<Button>(R.id.zoom_out_button)
        zoomOutButton.setOnClickListener {
            viewModel.imageController?.zoomOut()
        }

        val rotateLeftButton = view.findViewById<Button>(R.id.rotate_left_button)
        rotateLeftButton.setOnClickListener {
            viewModel.imageController?.rotateLeft()
        }

        val rotateRightButton = view.findViewById<Button>(R.id.rotate_right_button)
        rotateRightButton.setOnClickListener {
            viewModel.imageController?.rotateRight()
        }

        val flipButton = view.findViewById<Button>(R.id.flip_button)
        flipButton.setOnClickListener {
            viewModel.imageController?.flip()
        }

        val resetButton = view.findViewById<Button>(R.id.reset_button)
        resetButton.setOnClickListener {
            viewModel.imageController?.reset()
        }

//        viewModel.image.observe(viewLifecycleOwner) { newImage ->
//            titleView.text = newImage.title
//        }
    }
}
