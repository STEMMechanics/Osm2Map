# Releases

The [build workflow](../workflows/build.yml) runs the full build and compatibility
checks for pull requests into `master`, pushes to `master`, and manual runs.
After a successful push to `master`, including a merged pull request, it publishes
a GitHub release containing the verified shaded plugin JAR and generated release
notes. Pull requests and manual runs only build and upload an Actions artifact.

The base version stays in `build.gradle`. CI appends the workflow run number:

| Item | Example |
| --- | --- |
| Base version | `4.0.0` |
| Release title | `Osm2Map 4.0.0 build 4` |
| Git tag | `v4.0.0-build.4` |
| Plugin version | `4.0.0-build.4` |
| Download | `osm2map-4.0.0-build.4-all.jar` |

The workflow run number increases for every new run, including pull request
builds, so published build numbers can have gaps. Rerunning a workflow keeps its
build number. Published releases are left intact; an interrupted draft can be
completed by rerunning the failed release job. Tags point to the exact commit
whose JAR passed the build.

The release job uses the repository's `GITHUB_TOKEN` with `contents: write`; no
personal access token is needed. Build jobs have read-only repository access.

Local builds without a build number keep version `4.0.0`. To verify a numbered
build locally without publishing anything:

```sh
python3 .github/scripts/test_release.py
bash gradlew clean build writeReleaseMetadata -PbuildNumber=4
```

This writes `build/release.properties` and the numbered JAR. The in-game help
command reads its version from plugin metadata, so it shows the same build number.
