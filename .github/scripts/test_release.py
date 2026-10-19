import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


RELEASE_SCRIPT = Path(__file__).with_name("release.sh").resolve()


class ReleaseScriptTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.jar = self.root / "osm2map-4.0.0-build.4-all.jar"
        self.jar.write_bytes(b"test plugin")
        self.calls = self.root / "calls.jsonl"
        mock_gh = self.root / "gh"
        mock_gh.write_text(
            "#!/usr/bin/env python3\n"
            "import json, os, sys\n"
            "with open(os.environ['GH_CALLS'], 'a') as log:\n"
            "    log.write(json.dumps(sys.argv[1:]) + '\\n')\n"
            "if sys.argv[1:3] == ['release', 'view']:\n"
            "    state = os.environ['RELEASE_STATE']\n"
            "    if state == 'missing': sys.exit(1)\n"
            "    print(state)\n"
            "if sys.argv[1:3] == ['release', 'create']:\n"
            "    sys.exit(int(os.environ.get('CREATE_EXIT', '0')))\n"
        )
        mock_gh.chmod(0o755)
        self.environment = {
            **os.environ,
            "PATH": str(self.root) + os.pathsep + os.environ["PATH"],
            "GH_TOKEN": "test-only",
            "GH_CALLS": str(self.calls),
            "GITHUB_REPOSITORY": "test/osm2map",
            "GITHUB_SHA": "a" * 40,
            "RELEASE_STATE": "missing",
        }

    def run_release(self, **environment):
        result = subprocess.run(
            ["bash", str(RELEASE_SCRIPT), "4.0.0-build.4", str(self.jar)],
            env={**self.environment, **environment},
            text=True,
            capture_output=True,
        )
        calls = (
            [json.loads(line) for line in self.calls.read_text().splitlines()]
            if self.calls.exists() else []
        )
        return result, calls

    def test_new_release_uses_verified_jar_and_exact_commit(self):
        result, calls = self.run_release()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(calls[-1], [
            "release", "create", "v4.0.0-build.4", str(self.jar),
            "--repo", "test/osm2map", "--target", "a" * 40,
            "--title", "Osm2Map 4.0.0 build 4", "--generate-notes", "--latest",
        ])

    def test_rerun_leaves_published_release_untouched(self):
        result, calls = self.run_release(RELEASE_STATE="false")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual([call[:2] for call in calls], [["release", "view"]])

    def test_interrupted_draft_uploads_jar_before_publishing(self):
        result, calls = self.run_release(RELEASE_STATE="true")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(calls[1:], [
            ["release", "upload", "v4.0.0-build.4", str(self.jar),
             "--repo", "test/osm2map", "--clobber"],
            ["release", "edit", "v4.0.0-build.4", "--repo", "test/osm2map",
             "--draft=false", "--latest"],
        ])

    def test_missing_jar_does_not_contact_github(self):
        self.jar.unlink()
        result, calls = self.run_release()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(calls, [])

    def test_publish_failure_fails_the_job(self):
        result, _ = self.run_release(CREATE_EXIT="1")
        self.assertNotEqual(result.returncode, 0)


if __name__ == "__main__":
    unittest.main()
