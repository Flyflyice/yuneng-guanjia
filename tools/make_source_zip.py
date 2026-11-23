# -*- coding: utf-8 -*-
"""打包源码 zip（排除 build/dist/.avd 等构建产物）。"""
import os
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "dist", "驭能管家-1.2.0-source.zip")
INCLUDE = ["app/src/main", "tools", "README.md", "app-icon-512.png"]


def main():
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
        for item in INCLUDE:
            path = os.path.join(ROOT, item)
            if os.path.isfile(path):
                z.write(path, item)
                continue
            for base, dirs, files in os.walk(path):
                dirs[:] = [d for d in dirs if d not in ("__pycache__", ".gradle")]
                for name in files:
                    full = os.path.join(base, name)
                    rel = os.path.relpath(full, ROOT).replace("\\", "/")
                    z.write(full, rel)
    print(OUT, os.path.getsize(OUT))


if __name__ == "__main__":
    main()
