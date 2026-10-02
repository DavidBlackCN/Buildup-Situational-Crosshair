# Third-party notices

Classic behavior and artwork originate from Crosshair Mod / Situational Crosshair
by Kiley (KiOrlando; README image links also use ZaOrlando).
Upstream: https://github.com/KiOrlando/crosshairmod
Local reference: ../1.20.1 at HEAD d15e3ea plus its existing uncommitted Fabric port.
The original concept credits TheTellerverse's Minecraft Suggestions post in the
original README.

License evidence is conflicting and must be retained:
- Local and upstream LICENSE: Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International.
- Local fabric.mod.json: "All Rights Reserved".
- Original README: "You do NOT have permission to upload this mod or any of my mods to any place without my permission."
- Original README permits inclusion in modpacks and asks to be informed.

Independently authored Buildup code is now offered under MIT (root LICENSE).
This does not relicense the artwork or any adaptations of third-party work.
The original LICENSE is retained verbatim in `licenses/CC-BY-NC-SA-4.0.txt`.
No additional rights are claimed and
no publication or upload is performed by this local bootstrap. Resolve the
conflicting distribution statements with the rights holder before publication.
Stage 1 copies the four Classic crosshair PNGs byte-for-byte to the new namespace.
Its resolver reimplements the local behavior using 26.3 APIs, and its renderer is
new HUD API integration. No pixel, size or color changes were made to the PNGs.

The Gradle wrapper is from FabricMC/fabric-example-mod's 26.3 branch;
its Apache-2.0 notices are preserved in the wrapper scripts.
