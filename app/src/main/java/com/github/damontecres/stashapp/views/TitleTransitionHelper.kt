// Adapted from https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:leanback/leanback/src/main/java/androidx/leanback/widget/TitleHelper.java
package com.github.damontecres.stashapp.views

import android.transition.Scene
import android.transition.Transition
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup

class TitleTransitionHelper(val sceneRoot: ViewGroup, val titleView: View) {
    private val mTitleUpTransition: Transition
    private val mTitleDownTransition: Transition
    private val mSceneWithTitle: Scene
    private val mSceneWithoutTitle: Scene

    init {
        mTitleUpTransition =
            TransitionInflater.from(sceneRoot.context)
                .inflateTransition(androidx.leanback.R.transition.lb_title_out)

        mTitleDownTransition =
            TransitionInflater.from(sceneRoot.context)
                .inflateTransition(androidx.leanback.R.transition.lb_title_in)

        val sceneTitle = Scene(sceneRoot)
        sceneTitle.setEnterAction {
            titleView.visibility = View.VISIBLE
        }
        mSceneWithTitle = sceneTitle

        val sceneWithoutTitle = Scene(sceneRoot)
        sceneWithoutTitle.setEnterAction {
            titleView.visibility = View.GONE
        }
        mSceneWithoutTitle = sceneWithoutTitle
    }

    /**
     * Shows the title.
     */
    fun showTitle(show: Boolean) {
        if (show) {
            TransitionManager.go(mSceneWithTitle, mTitleDownTransition)
        } else {
            TransitionManager.go(mSceneWithoutTitle, mTitleUpTransition)
        }
    }
}
