"""Assemble a Windows distribution bundle."""
from pathlib import Path
import shutil, subprocess

project = Path(__file__).resolve().parents[1]
desktop = project.parent
bundle = desktop / '2008-music-soundtrack-bundle'
home = Path.home()

def copy(source, target):
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)

copy(project / 'build/libs/2008-music-soundtrack-0.1.0-local.jar', bundle / 'plugin/2008-music-soundtrack.jar')

for name in (project / 'runtime-jars.txt').read_text().splitlines():
    if name.startswith(('injected-client-', 'runelite-api-')): continue
    copy(home / '.runelite/repository2' / name, bundle / 'client' / name)

cache = home / '.runelite/cache'
for source in cache.iterdir():
    if source.name == 'main_file_cache.dat2' or (source.name.startswith('main_file_cache.idx') and source.name.removeprefix('main_file_cache.idx').isdigit()):
        copy(source, bundle / 'assets/cache' / source.name)

for src, dest, expected in [('Music 1 - Index 6', 'music', 826), ('Music 2 - Index 11 - Fanfares', 'fanfares', 396)]:
    files = list((desktop / 'MIDI Dump/2008-music' / src).glob('*.mid'))
    assert len(files) == expected
    for source in files:
        copy(source, bundle / 'assets' / dest / source.name)

for folder in ['src/main', 'src/test', 'packaging', 'gradle']:
    for source in (project / folder).rglob('*'):
        if source.is_file() and '__pycache__' not in source.parts:
            copy(source, bundle / 'source' / source.relative_to(project))

for name in ['build.gradle', 'settings.gradle', 'gradlew', 'gradlew.bat', 'LICENSE']:
    copy(project / name, bundle / 'source' / name)
for source in (project / 'libs').glob('*.jar'):
    copy(source, bundle / 'source/libs' / source.name)

print('Assembled', bundle, flush=True)
