#!/usr/bin/env python3
"""Export the models.zip Blockbench projects into the mod's GeckoLib assets.

This drives a *running* Blockbench through the MCP plugin
(https://github.com/jasonjgardner/blockbench-mcp-plugin, endpoint ``http://localhost:3000/bb-mcp``).
Nothing here converts geometry by hand: Blockbench compiles the files with the same code path as
File > Export, so the assets carry Blockbench's own Bedrock conventions.

Why that matters (all of it verified against Blockbench's `js/formats/bedrock/bedrock.js` and against
the shipped Aquanaut assets):

* ``compileCube``/``compileGroup`` mirror the model in X and negate the X/Y rotations, because
  Blockbench's editor coordinates are not the Bedrock ones. Exporting the project's raw coordinates
  produces a mirrored model with swapped left/right textures.
* The GeckoLib plugin forces ``format_version`` to ``1.12.0`` and strips ``item_display_transforms``
  on export when the project is in the ``geckolib_model`` format.
* For GeckoLib projects the plugin also replaces the Bedrock keyframe compiler, writing
  ``{"post": {"vector": [x, y, z]}}`` keyframes with the X/Y rotations inverted - the form every
  shipped Aquanaut animation uses. Exporting the Bedrock form instead makes GeckoLib play mirrored
  rotations.

Usage::

    # Blockbench must be running with the MCP plugin active
    python3 scripts/export_blockbench_models.py --src /path/to/models --write
    python3 scripts/export_blockbench_models.py --check        # diff a fresh export against the repo

The source tree is the extracted ``models.zip`` (``*.bbmodel`` plus ``*.animation.json``).
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid
from dataclasses import dataclass

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(REPO_ROOT, "src/main/resources/assets/aquanaut")
DEFAULT_SRC = os.path.join(REPO_ROOT, "scripts/blockbench/src")
BLOCKBENCH = os.environ.get("BLOCKBENCH_BIN", "blockbench")
DEFAULT_ENDPOINT = os.environ.get("BLOCKBENCH_MCP_URL", "http://localhost:3000/bb-mcp")


@dataclass(frozen=True)
class Model:
    slug: str
    bbmodel: str
    kind: str  # "entity" | "block"


MODELS: tuple[Model, ...] = (
    Model("vamprey", "vamprey.bbmodel", "entity"),
    Model("oresucker", "oresucker.bbmodel", "entity"),
    Model("flagellonautilus", "flagellonautilus.bbmodel", "entity"),
    Model("skeleton_carp", "skeleton_carp.bbmodel", "entity"),
    Model("golden_carp", "golden_carp.bbmodel", "entity"),
    Model("silver_carp", "silver_carp.bbmodel", "entity"),
    Model("gentlefish", "Gentlefish.bbmodel", "entity"),
    Model("slimmy", "Slimy.bbmodel", "entity"),
    Model("ionfin", "Ionfin.bbmodel", "entity"),
    Model("opticichthus", "Opticichthus.bbmodel", "entity"),
    Model("gemini_jellyfish", "gemini_jellyfish.bbmodel", "entity"),
    Model("ecofish", "ecofish.bbmodel", "entity"),
    Model("pale_abyss_hydra", "Pale Abyss Hydra.bbmodel", "entity"),
    Model("three_headed_shark", "Three-Headed Shark.bbmodel", "entity"),
    Model("dissection_table", "dissection_table/dissection_table.bbmodel", "block"),
    Model("dissection_table_x2", "dissection_table_x2.bbmodel", "block"),
    Model("dissection_table_x4", "dissection_table_x4.bbmodel", "block"),
)

# The entity classes play these clip names; the projects name them
# `<slug>_animation_<verb>`, `bbpi:<model>:animation:<verb>`, `animation.<slug>.<verb>` or plainly
# `<verb>`, so the export renames each clip to its verb.
CANONICAL_CLIPS = ("swim", "charge", "attack", "open", "close", "target")

CONVERT_JS = ("(function(){if(Format.id!=='geckolib_model')Formats.geckolib_model.convertTo();"
              "return Format.id;})()")

RENAME_CLIPS_JS = """
(function () {
    const allowed = %s;
    const canon = name => {
        let s = String(name).toLowerCase();
        s = s.replace(/^bbpi:[a-z0-9_]+:animation:/, '');
        s = s.replace(/^animation\\./, '');
        s = s.replace(/^[a-z0-9_]+_animation_/, '');
        return s;
    };
    const seen = [];
    Animator.animations.forEach(animation => {
        const clip = canon(animation.name);
        if (!allowed.includes(clip)) throw new Error('unmapped clip name: ' + animation.name);
        animation.name = clip;
        seen.push(clip);
    });
    return seen.join(',');
})()
""" % json.dumps(list(CANONICAL_CLIPS))

COMPILE_GEO_JS = "Codecs.bedrock.compile()"
COMPILE_ANIM_JS = (
    "(function(){const codec=AnimationCodec.getCodec();"
    "const compiled=codec.compileFile(Animator.animations);"
    "return typeof compiled==='string'?compiled:autoStringify(compiled);})()"
)

# Authoring corrections applied *inside* the project before Blockbench exports it. They exist because
# the source projects are not all authored in the mod's conventions; everything else is exported
# untouched.
#
# flagellonautilus: the root group is yawed -90, which points the head at +Z while every other model
# (and every shipped Aquanaut creature) faces -Z, and the geometry is authored ~6.5 units off-centre
# in X. Recentring the cubes/pivots and turning the root onto -Z fixes both; animation channels are
# deltas relative to the rest pose, so they follow along.
FIX_CENTRE_AND_YAW_JS = """
(function () {
    const worldZ = group => {
        const v = new THREE.Vector3();
        group.mesh.getWorldPosition(v);
        return v.z;
    };
    const mean = (groups, fn) => groups.length ? groups.reduce((sum, g) => sum + fn(g), 0) / groups.length : null;
    const heads = Group.all.filter(g => /head|mouth|snout|jaw/i.test(g.name));
    const tails = Group.all.filter(g => /tail|peduncle|rear/i.test(g.name));
    const headZ = mean(heads, worldZ);
    const tailZ = mean(tails, worldZ);
    let yawed = false;
    if (headZ !== null && tailZ !== null && tailZ < headZ) {
        Group.all.filter(g => g.parent === 'root').forEach(g => { g.rotation[1] += 180; });
        Canvas.updateAll();
        yawed = true;
    }

    const box = new THREE.Box3().setFromObject(Project.model_3d);
    let loX = Infinity, hiX = -Infinity, loZ = Infinity, hiZ = -Infinity;
    Cube.all.forEach(cube => {
        loX = Math.min(loX, cube.from[0], cube.to[0]); hiX = Math.max(hiX, cube.from[0], cube.to[0]);
        loZ = Math.min(loZ, cube.from[2], cube.to[2]); hiZ = Math.max(hiZ, cube.from[2], cube.to[2]);
    });
    const shift = [0, 0, 0];
    const centre = [(loX + hiX) / 2, 0, (loZ + hiZ) / 2];
    if (Math.abs(centre[0]) > 0.5) shift[0] = -centre[0];
    if (Math.abs(centre[2]) > 0.5) shift[2] = -centre[2];
    if (shift[0] || shift[2]) {
        Cube.all.forEach(cube => {
            for (const axis of [0, 2]) {
                cube.from[axis] += shift[axis];
                cube.to[axis] += shift[axis];
                cube.origin[axis] += shift[axis];
            }
        });
        Group.all.forEach(group => {
            group.origin[0] += shift[0];
            group.origin[2] += shift[2];
        });
        Canvas.updateAll();
    }
    return JSON.stringify({yawed: yawed, shift: shift, centre: centre, box: box.getCenter(new THREE.Vector3()).toArray()});
})()
"""

PROJECT_FIXES = {"flagellonautilus": FIX_CENTRE_AND_YAW_JS}


class McpClient:
    """Tiny JSON-RPC client for the Blockbench MCP server (streamable HTTP transport)."""

    def __init__(self, url: str):
        self.url = url
        self.session: str | None = None

    def _post(self, payload: dict) -> tuple[str | None, str, int]:
        request = urllib.request.Request(self.url, data=json.dumps(payload).encode(), method="POST")
        request.add_header("Content-Type", "application/json")
        request.add_header("Accept", "application/json, text/event-stream")
        if self.session:
            request.add_header("mcp-session-id", self.session)
        try:
            with urllib.request.urlopen(request, timeout=180) as response:
                return response.headers.get("mcp-session-id"), response.read().decode(), response.status
        except urllib.error.HTTPError as error:
            return error.headers.get("mcp-session-id"), error.read().decode(), error.code
        except urllib.error.URLError as error:
            raise SystemExit(
                f"cannot reach the Blockbench MCP server at {self.url} ({error.reason}).\n"
                "Start Blockbench and enable the MCP plugin (its settings default to port 3000 and"
                " endpoint /bb-mcp)."
            ) from error

    @staticmethod
    def _decode(body: str) -> dict:
        body = body.strip()
        if body.startswith("{"):
            return json.loads(body)
        for line in body.splitlines():
            if line.startswith("data:"):
                chunk = line[5:].strip()
                if chunk:
                    return json.loads(chunk)
        raise SystemExit(f"empty reply from the MCP server: {body[:200]!r}")

    def connect(self) -> None:
        session, body, status = self._post({
            "jsonrpc": "2.0", "id": 1, "method": "initialize",
            "params": {"protocolVersion": "2024-11-05", "capabilities": {},
                       "clientInfo": {"name": "aquanaut-export", "version": "1"}},
        })
        if not session:
            raise SystemExit(f"MCP initialize failed (HTTP {status}): {body[:300]}")
        self.session = session
        self._post({"jsonrpc": "2.0", "method": "notifications/initialized"})

    def call(self, method: str, params: dict) -> dict | None:
        _, body, status = self._post({"jsonrpc": "2.0", "id": str(uuid.uuid4()),
                                      "method": method, "params": params})
        parsed = self._decode(body)
        if "error" in parsed:
            raise SystemExit(f"MCP {method} failed (HTTP {status}): {json.dumps(parsed['error'])[:400]}")
        return parsed.get("result")

    def tool(self, name: str, arguments: dict | None = None) -> str:
        result = self.call("tools/call", {"name": name, "arguments": arguments or {}})
        if result is None:
            return ""
        if result.get("isError"):
            raise SystemExit(f"Blockbench tool {name} failed: {json.dumps(result)[:500]}")
        return "\n".join(chunk.get("text", "") for chunk in result.get("content", [])
                         if chunk.get("type") == "text")

    def eval(self, code: str):
        """Run an expression inside Blockbench and decode its JSON-encoded result."""
        text = self.tool("risky_eval", {"code": code})
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            return text

    def project_path(self) -> str | None:
        info = self.tool("get_project_info")
        if info.startswith("No project is open"):
            return None
        try:
            return (json.loads(info).get("project") or {}).get("save_path")
        except json.JSONDecodeError:
            return None


def open_model(client: McpClient, path: str) -> None:
    """Ask the running Blockbench to open a file (Electron forwards it to the primary instance)."""
    path = os.path.abspath(path)
    if client.project_path() == path:
        return
    subprocess.Popen([BLOCKBENCH, path], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                     stdin=subprocess.DEVNULL, start_new_session=True)
    for _ in range(120):
        time.sleep(0.5)
        if client.project_path() == path:
            time.sleep(0.5)
            return
    raise SystemExit(f"Blockbench never opened {path}")


def export_model(client: McpClient, model: Model, src: str) -> tuple[str, str | None, dict]:
    path = os.path.join(src, model.bbmodel)
    if not os.path.isfile(path):
        raise SystemExit(f"missing source model {path}")
    open_model(client, path)

    fmt = client.eval(CONVERT_JS)
    if fmt != "geckolib_model":
        raise SystemExit(f"{model.slug}: expected the geckolib_model format, got {fmt!r}")
    clips = str(client.eval(RENAME_CLIPS_JS))

    if model.slug in PROJECT_FIXES:
        client.eval(PROJECT_FIXES[model.slug])

    geo = client.eval(COMPILE_GEO_JS)
    geo_object = json.loads(geo)
    if geo_object.get("format_version") != "1.12.0":
        raise SystemExit(f"{model.slug}: Blockbench exported format_version "
                         f"{geo_object.get('format_version')!r}, expected 1.12.0")
    geometry = geo_object["minecraft:geometry"][0]

    anim = None
    animation_object = None
    if model.kind == "entity":
        anim = client.eval(COMPILE_ANIM_JS)
        animation_object = json.loads(anim)
        exported = sorted(animation_object["animations"])
        expected = sorted(c for c in clips.split(",") if c)
        if exported != expected:
            raise SystemExit(f"{model.slug}: exported clips {exported} do not match {expected}")

    report = {
        "slug": model.slug,
        "clips": clips,
        "bones": len(geometry["bones"]),
        "cubes": sum(len(bone.get("cubes", [])) for bone in geometry["bones"]),
        "geo_bytes": len(geo),
        "anim_bytes": len(anim) if anim else 0,
    }
    return geo, anim, report


def paths(slug: str) -> tuple[str, str]:
    return (os.path.join(ASSETS, "geo", f"{slug}.geo.json"),
            os.path.join(ASSETS, "animations", f"{slug}.animation.json"))


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--src", default=DEFAULT_SRC,
                        help="directory holding the extracted models.zip contents")
    parser.add_argument("--url", default=DEFAULT_ENDPOINT, help="Blockbench MCP endpoint")
    parser.add_argument("--only", action="append", default=None, help="export only these slugs")
    parser.add_argument("--write", action="store_true", help="write the assets into the repo")
    parser.add_argument("--check", action="store_true",
                        help="re-export and fail if the checked-in assets differ")
    args = parser.parse_args(argv)

    specs = [m for m in MODELS if not args.only or m.slug in args.only]
    if not specs:
        raise SystemExit("--only did not match any known slug")

    client = McpClient(args.url)
    client.connect()

    failures: list[str] = []
    for model in specs:
        geo, anim, report = export_model(client, model, args.src)
        geo_path, anim_path = paths(model.slug)
        if args.check:
            for path, text in ((geo_path, geo), (anim_path, anim)):
                if text is None:
                    continue
                with open(path, encoding="utf-8") as handle:
                    if handle.read() != text + "\n":
                        failures.append(f"{model.slug}: {os.path.relpath(path, REPO_ROOT)} is stale")
        elif args.write:
            with open(geo_path, "w", encoding="utf-8") as handle:
                handle.write(geo + "\n")
            if anim is not None:
                with open(anim_path, "w", encoding="utf-8") as handle:
                    handle.write(anim + "\n")
        print(f"ok   {report['slug']:<22} bones={report['bones']:<4} cubes={report['cubes']:<4} "
              f"clips={report['clips'] or '-'} geo={report['geo_bytes']}B anim={report['anim_bytes']}B")

    if failures:
        print("\n".join(failures), file=sys.stderr)
        return 1
    action = "written" if args.write else ("verified" if args.check else "analysed")
    print(f"\n{len(specs)} models {action}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
