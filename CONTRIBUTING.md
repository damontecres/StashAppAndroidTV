# Contributing

## Technical Debt
Please be sure to consider how heavily your contribution impacts the maintainability of the project long term, sometimes less is more.

## Contributor Checklist
Please make sure that you've considered the following before you submit your Pull Requests as ready for merging:
* Your code is written in Kotlin and follows the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide). If Java is used, please explain in your pull request why Java is required.
* You have run code linters to make sure that the code is readable.
* You have read through existing/merged [pull requests](https://github.com/damontecres/StashAppAndroidTV/pulls) and open/closed [issues](https://github.com/damontecres/StashAppAndroidTV/issues) to make sure that this contribution is required and isn't a duplicate.
* You commented adequately your code to make it understandable for other developers.

## Legal Agreements

You acknowledge that, if applicable, submitting and subsequent acceptance of any pull request you, the code contributor of the pull request, agree and acknowledge that the code will be licensed under [AGPL](LICENSE).

**In case you are unable to follow any of the above, please include an explanation in your pull request**

## Tips

### Submodule

Certain assets are provided by thehttps://github.com/stashapp/stash repo which is included as a submodule. Make sure to run `git submodule update --init --recursive --remote` after cloning this repo to get the latest version of the submodule.

If you get compile errors such as `java.nio.file.NoSuchFileException: .../StashAppAndroidTV/app/src/main/res/mipmap-anydpi/stash_logo.png`, you need to update the submodule (or the paths in `stashapp/stash` has changed!).
