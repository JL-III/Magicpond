# Magicpond
A quick side project to encourage players to fish in one place on the server.
Players receive bonus fish for fishing in a specific location on the server.

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
On first run the script downloads the matching Paper build into a gitignored
`run/` directory, copies the plugin into `run/plugins/`, accepts the EULA, and
starts an interactive server console (type `stop` or press Ctrl+C to quit).
Rebuild-and-rerun reinstalls the latest jar automatically; delete `run/` for a
clean slate. Override the version with `PAPER_VERSION=1.21.4 bash scripts/run-server.sh`.

> Requires `bash`, `curl`, and a JDK on your PATH. On Windows use WSL or Git Bash.
