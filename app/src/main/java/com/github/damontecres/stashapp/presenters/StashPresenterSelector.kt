package com.github.damontecres.stashapp.presenters

import androidx.leanback.widget.ClassPresenterSelector
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.data.Tag


val stashPresenterSelector = ClassPresenterSelector()
    .addClassPresenter(PerformerData::class.java, PerformerPresenter())
    .addClassPresenter(SlimSceneData::class.java, ScenePresenter())
    .addClassPresenter(StudioData::class.java, StudioPresenter())
    .addClassPresenter(Tag::class.java, TagPresenter())
