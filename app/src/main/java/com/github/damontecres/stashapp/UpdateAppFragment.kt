package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.Release
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.UpdateChecker
import com.github.damontecres.stashapp.util.Version
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.joinNotNullOrBlank
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.launch

class UpdateAppFragment : GuidedStepSupportFragment() {
    private val serverViewModel: ServerViewModel by activityViewModels()

    private lateinit var release: Release

    override fun onCreate(savedInstanceState: Bundle?) {
        release = requireArguments().getDestination<Destination.UpdateApp>().release
        super.onCreate(savedInstanceState)
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        val installedVersion = UpdateChecker.getInstalledVersion(requireActivity())
        val serverVersion = StashServer.getCurrentServerVersion()
        val description =
            buildList {
                add("${getString(R.string.stashapp_package_manager_installed_version)}: $installedVersion")
                if (!Version.isServerSupportedByAppVersion(serverVersion, release.version)) {
                    add("Warning!! This update does not support the current server's version $serverVersion!!")
                }
                addAll(release.notes)
            }.joinNotNullOrBlank("\n\n")

        return GuidanceStylist.Guidance(
            "${getString(R.string.stashapp_package_manager_latest_version)}: ${release.version}",
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
            GuidedAction
                .Builder(requireContext())
                .id(GuidedAction.ACTION_ID_YES)
                .title("Download & Install")
                .description("${release.version}")
                .hasNext(false)
                .build(),
        )
        if (release.body.isNotNullOrBlank()) {
            actions.add(
                GuidedAction
                    .Builder(requireContext())
                    .id(1000L)
                    .title("See changelog")
                    .hasNext(false)
                    .build(),
            )
        }
        actions.add(
            GuidedAction
                .Builder(requireContext())
                .clickAction(GuidedAction.ACTION_ID_CANCEL)
                .hasNext(false)
                .build(),
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_YES) {
            viewLifecycleOwner.lifecycleScope.launch(
                StashCoroutineExceptionHandler {
                    Toast.makeText(
                        requireContext(),
                        "Update failed: ${it.message}",
                        Toast.LENGTH_LONG,
                    )
                },
            ) {
                UpdateChecker.installRelease(requireActivity(), release)
            }
        } else if (action.id == 1000L) {
            serverViewModel.navigationManager.navigate(Destination.Fragment(UpdateChangelogFragment::class.qualifiedName!!))
        } else {
            finishGuidedStepSupportFragments()
        }
    }

    override fun onProvideTheme(): Int = R.style.Theme_StashAppAndroidTV_GuidedStep
}
