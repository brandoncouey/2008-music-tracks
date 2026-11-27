"""Assemble a whitelisted Windows bundle. Never copy user profiles or credentials."""
from pathlib import Path
import shutil,json,hashlib,subprocess

project=Path(__file__).resolve().parents[1]
desktop=project.parent
bundle=desktop/'HDOS Music - Windows'
home=Path.home()
def copy(source,target):
    target.parent.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(source,target)

copy(project/'build/libs/hdos-music-0.1.0-local.jar',bundle/'plugin/hdos-music.jar')
for name in (project/'runtime-jars.txt').read_text().splitlines():
    if name.startswith(('injected-client-','runelite-api-')):continue
    copy(home/'.runelite/repository2'/name,bundle/'client'/name)
api=list((home/'.gradle/caches/modules-2/files-2.1/net.runelite/runelite-api/1.13.8').rglob('*.jar'))
assert len(api)==1,api
copy(api[0],bundle/'client'/api[0].name)

cache=home/'hdos/bin/cache/staged/runescape'
for source in cache.iterdir():
    if source.name=='main_file_cache.dat2' or (source.name.startswith('main_file_cache.idx') and source.name.removeprefix('main_file_cache.idx').isdigit()):
        before=source.stat()
        copy(source,bundle/'assets/cache'/source.name)
        after=source.stat()
        assert (before.st_size,before.st_mtime_ns)==(after.st_size,after.st_mtime_ns),f'Cache changed during copy: {source}'
for src,dest,expected in [('Music 1 - Index 6','music',826),('Music 2 - Index 11 - Fanfares','fanfares',396)]:
    files=list((desktop/'MIDI Dump/HDOS'/src).glob('*.mid'))
    assert len(files)==expected
    for source in files:copy(source,bundle/'assets'/dest/source.name)
for name in ['Start-HDOS-Music.cmd','Check-Installation.cmd']:
    # Windows command files need CRLF for reliable cmd.exe parsing.
    (bundle/name).write_bytes((project/'packaging'/name).read_text().replace('\r\n','\n').replace('\n','\r\n').encode())
classes=project/'build/portable-launcher-classes'
classes.mkdir(parents=True,exist_ok=True)
subprocess.run(['javac','--release','17','-d',str(classes),str(project/'packaging/Bootstrap.java')],check=True)
subprocess.run(['jar','cf',str(bundle/'launcher.jar'),'-C',str(classes),'.'],check=True)

for folder in ['src/hdos','src/hdosTest','packaging','gradle']:
    for source in (project/folder).rglob('*'):
        if source.is_file() and '__pycache__' not in source.parts:copy(source,bundle/'source/plugin'/source.relative_to(project))
for name in ['build.gradle','settings.gradle','gradlew','gradlew.bat','LICENSE']:
    copy(project/name,bundle/'source/plugin'/name)
for source in (project/'libs').glob('*.jar'):copy(source,bundle/'source/plugin/libs'/source.name)
native=home/'hdos-render-work'
for source in (native/'src').rglob('*.java'):
    if source.name not in ['GUI.java','Main.java']:copy(source,bundle/'source/native-synth'/source.relative_to(native))
for name in ['SYNTH-INTEGRATION.txt','build-synth.ps1','read_only_cache.py']:
    copy(native/name,bundle/'source/native-synth'/name)
for name in ['osrs-to-hdos-mapping.csv','track-names.csv']:
    copy(desktop/'MIDI Dump'/name,bundle/'reports'/name)
for source in (project/'build/test-results/test').glob('TEST-*.xml'):
    copy(source,bundle/'reports/unit-tests'/source.name)
print('Assembled',bundle,flush=True)
