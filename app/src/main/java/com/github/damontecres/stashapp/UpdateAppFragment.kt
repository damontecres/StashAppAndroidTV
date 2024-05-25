package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.UpdateChecker
import kotlinx.coroutines.launch

class UpdateAppFragment(private val release: UpdateChecker.Release) : GuidedStepSupportFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        val installedVersion = UpdateChecker.getInstalledVersion(requireActivity())
        val description =
            """
            ${getString(R.string.stashapp_package_manager_installed_version)}: $installedVersion

            Note: Upgrading in-app is experimental!
            """.trimIndent()
        return GuidanceStylist.Guidance(
            "${release.version} available!",
            description,
            null,
            AppCompatResources.getDrawable(requireContext(), R.mipmap.stash_logo),
        )
    }

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        super.onCreateActions(actions, savedInstanceState)
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_YES)
                .title("Download & Install")
                .description("${release.version}")
                .hasNext(false)
                .build(),
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .clickAction(GuidedAction.ACTION_ID_CANCEL)
                .hasNext(false)
                .build(),
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_YES) {
            viewLifecycleOwner.lifecycleScope.launch(
                StashCoroutineExceptionHandler {
                    Toast.makeText(requireContext(), "Update failed: ${it.message}", Toast.LENGTH_LONG)
                },
            ) {
                UpdateChecker.installRelease(requireActivity(), release)
            }
        } else {
            finishGuidedStepSupportFragments()
        }
    }

    override fun onProvideTheme(): Int {
        return R.style.Theme_StashAppAndroidTV_GuidedStep
    }
}
