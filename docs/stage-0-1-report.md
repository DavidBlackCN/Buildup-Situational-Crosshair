# Stage 0 / Stage 1 report

## Toolchain (verified 2026-10-02)

Minecraft 26.3; Fabric Loader 0.19.5; Fabric API 0.161.0+26.3;
Loom 1.18.2; Gradle 9.7.1; Oracle JDK 25.0.3+9-LTS-195.
Local JDK: `C:/Program Files/Java/jdk-25.0.3`.

Official sources:
- https://fabricmc.net/2026/09/15/263.html
- https://github.com/FabricMC/fabric-example-mod/tree/26.3
- https://meta.fabricmc.net/v2/versions/loader/26.3
- https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml
- https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml

The current official template uses Gradle 9.7.1 and Loom 1.18-SNAPSHOT;
we select published stable Loom 1.18.2 instead. These are newer than planning
values 1.17 / 9.6.0. Dependency resolution succeeded. Minecraft is unobfuscated:
`net.fabricmc.fabric-loom`, regular implementation dependencies, no Yarn or
mapping dependency. Loader's “Mappings not present!” is expected here.

## Stage 0 — PASS

Fresh Groovy project following the official template with split main/client
source sets; official wrapper downloaded rather than upgrading the old project.
Only 26.3 has been initialized as a new Git repository. Workspace root has no .git.
Metadata declares client environment and only a client entrypoint.

`clean build` completed and produced the mod jar. `runClient` loaded Minecraft
26.3, Loader and API, and logged “Buildup Situational Crosshair loaded (Stage 0)”.
Main menu visually observed with computer-use after first-run accessibility page.
See `validation/stage-0-client.log`.

Development offline credentials produce user-properties HTTP 401 and Realms
authentication errors; main menu still works. Empty source-set output directories
produce a Loader classpath warning; no missing mod classes or mixin failures.

## Stage 1 — pending

Stage 0 passed before implementation starts. Actual dependency inspection confirms
HudElementRegistry, Hud, GuiGraphicsExtractor and RenderPipelines.CROSSHAIR.
Implementation and validation results will be recorded here after migration.
