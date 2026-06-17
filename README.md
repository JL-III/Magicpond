# Magicpond
A quick side project to encourage players to fish in one place on the server.
A magic pond always yields fish (vanilla junk/treasure are converted to fish) plus
a bonus fish per catch — unless the spot is overfished, in which case it yields
junk like anywhere else until it recovers.

Magic ponds are designated per-chunk in-game: stand in the chunk and run
`/magicpond set` (`/magicpond unset` to remove, `/magicpond list` to review).
Designations persist in `ponds.yml` in the plugin's data folder.

It also includes an **overfishing** system that deters autofishing: a player who
fishes one spot too frequently for too long stops catching fish there until the
spot recovers. See [docs/overfishing.md](docs/overfishing.md) for the model,
configuration, and in-game verification steps.

### Dependencies
- None at runtime. Built against the Paper API (`paper.version` in `pom.xml`).

### Building
```
mvn package
```
The plugin jar is written to `target/`.

### Running a test server
Launch a local Paper server with the freshly built plugin already installed:
```
mvn -Prun verify        # build, then download/boot Paper with the plugin
# or, equivalently:
bash scripts/run-server.sh
```
On first run the script downloads the requested Paper build (via the PaperMC
Fill v3 API) into a gitignored `run/` directory, copies the plugin into
`run/plugins/`, accepts the EULA, and starts an interactive server console (type
`stop` or press Ctrl+C to quit). Rebuild-and-rerun reinstalls the latest jar
automatically; delete `run/` for a clean slate.

The server version/build is pinned in `pom.xml` (`paper.run.version` /
`paper.run.build`, default **26.1.2 build 69**) and overridable per-run:
```
PAPER_VERSION=26.1.2 PAPER_BUILD=69 bash scripts/run-server.sh   # exact build
PAPER_VERSION=26.1.2-69 bash scripts/run-server.sh               # combined form
PAPER_BUILD= PAPER_VERSION=26.1.2 bash scripts/run-server.sh     # latest build
```

`scripts/run-server.sh` is project-agnostic — copy it (and the `run` profile from
`pom.xml`) into any Maven-based Paper plugin and it auto-detects the built jar
from the project's `artifactId`; nothing in it is specific to Magicpond.

> Requires `bash`, `curl`, and a JDK on your PATH (`jq` optional but recommended).
> On Windows use WSL or Git Bash.
