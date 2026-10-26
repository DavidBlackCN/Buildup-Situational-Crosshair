# Buildup Situational Crosshair

Client-only Fabric mod for Minecraft **26.3**, version `0.1.0-adaptive-crosshair.1+mc26.3`.
No server installation is required. Core Reset replaces the previous expansion
roadmap. All four Classic states use borderless adaptive light/dark rendering by
default, with vanilla inversion available as a saved setting.

The mod retains the original 15x15 Classic DOT, BLOCK, ATTACK and ERROR textures.
Air uses DOT; entity targets use ATTACK; mining checks native breakability and
harvest-tool suitability (creative bypasses these checks). Charged crossbows
retain Classic's ranged off-hand precedence. Unavailable contexts and spectator
block targets delegate to Vanilla. Vanilla still controls HUD visibility,
third-person/spectator restrictions, debug crosshair and attack indicators.

Sidecars, Classic+, JSON rules and secondary-action prediction remain removed.
The central crosshair now has a small time-based transition; there is no theme system.

## Configuration

Install **YACL 3.9.7 for Minecraft 26.3 Fabric** (required), alongside Fabric API.
Install **Mod Menu 21** to open the YACL configuration page:
**Mods > Buildup Situational Crosshair > Configure**.
There is no extra button in Minecraft's vanilla Options screen.

- Color mode: **Adaptive contrast** (default) or **Vanilla inversion**.
- Enable situational crosshair: on by default; off restores the vanilla crosshair.
- Shape animation: gentle expansion (default), fade, or off.

YACL provides Save, Cancel, Undo and Reset controls. Changes are applied when saved
and persist in `config/buildup_situational_crosshair.json` across client restarts.
All former `/crosshair` debug commands have been removed.
Without Mod Menu, the same configuration can be edited while the game is closed:

```json
{
  "enabled": true,
  "animation": "expand",
  "colorMode": "adaptive"
}
```

Use `"vanilla_invert"` for original inversion. Animation values are `"expand"`,
`"fade"` and `"off"`. Invalid configuration falls back to defaults without
silently overwriting the existing file.

The center remains steady. All shape transitions take **140ms**, including
return to DOT and entry into ERROR; the cubic ease-out curve is unchanged. Shared BLOCK/ERROR outline pixels remain
unchanged; interruptions continue from the displayed frame. Expand distributes
moving pixels across neighboring GUI pixels during its maximum one-pixel travel
per axis, so intermediate frames are softer; endpoints use the original PNGs.
Resource reload refreshes the masks. Packs with non-15x15, colored/translucent
pixels, missing shared centers or a non-single-pixel DOT render immediately
without animation, preserving their selected textures.

The rejected dark contour remains removed. Adaptive mode changes only the body
color. It combines a larger background sample, temporal smoothing and stable tone
selection on the same block face or entity to resist texture-driven flashes.
Sustained extreme lighting changes can still change the tone. All four states and
their transitions use the same color policy. Unsupported resources, unavailable
or stale samples fall back to inversion. No outline, shadow or new artwork is added.

## Build and validation

Use **JDK 25** and the existing **Gradle 9.7.1** Wrapper. Set `JAVA_HOME` if needed.
Loom 1.18.2, Fabric Loader 0.19.5 and Fabric API 0.161.0+26.3 are unchanged.

```sh
./gradlew clean build
./gradlew runClientGameTest
./gradlew runClient
```

Windows: use `gradlew.bat`. Output is in `build/libs/`. The client integration
suite creates temporary worlds under `build/run/clientGameTest`; its classes and
assets do not ship in the mod JAR. It checks actual native mining/item behavior
and transformed HUD submissions. Automated tests do not replace manual visual
and modpack acceptance. `build` runs deterministic transition contracts via
`animationTest`; client tests also capture six transition galleries.
Development reports, validation screenshots and logs are kept in the workspace's
`../docs/` directory, outside this Git repository. They are not shipped with the mod.

## Attribution

Classic design and the four unchanged PNGs originate from Kiley (KiOrlando)'s
Crosshair Mod. Independently authored code uses MIT; inherited artwork retains
its original CC BY-NC-SA 4.0 terms and recorded distribution restrictions. The
combined mod is not exclusively MIT licensed. See
[third-party notices](THIRD_PARTY_NOTICES.md) and `licenses/CC-BY-NC-SA-4.0.txt`.
