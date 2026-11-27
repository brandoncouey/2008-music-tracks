"""Repack only sound indexes, retaining IDs/metadata for the existing reader."""
from pathlib import Path
import sys,json,hashlib,zipfile,subprocess,shutil
sys.path.insert(0,str(Path.home()/'midi-dump-work'))
from extract import Cache

desktop=Path.home()/'OneDrive/Desktop'
bundle=desktop/'HDOS Music - Windows'
source=bundle/'assets/cache'
out=desktop/'HDOS Music Assets - Compact'
if out.exists():raise FileExistsError(out)
cache=out/'cache';cache.mkdir(parents=True)
store=Cache(source)
indices=sorted(int(p.name.split('idx')[1]) for p in source.glob('main_file_cache.idx*') if p.name.split('idx')[1].isdigit())
counts={};raw_total=0
with (cache/'main_file_cache.dat2').open('wb') as data:
 data.write(bytes(520))
 for index in indices:
  original=(source/f'main_file_cache.idx{index}').read_bytes()
  result=bytearray(len(original))
  count=0
  if index in (4,14,15,255):
   for archive in range(len(original)//6):
    length=int.from_bytes(original[archive*6:archive*6+3],'big')
    if length==0:continue
    raw=store.read(index,archive)
    first=data.tell()//520
    for chunk,start in enumerate(range(0,len(raw),512)):
     # These soundbank archive IDs fit the standard two-byte sector header.
     assert archive<=65535
     part=raw[start:start+512]
     sector=data.tell()//520
     following=sector+1 if start+512<len(raw) else 0
     header=archive.to_bytes(2,'big')+chunk.to_bytes(2,'big')+following.to_bytes(3,'big')+bytes([index])
     data.write(header+part+bytes(512-len(part)))
    result[archive*6:archive*6+6]=len(raw).to_bytes(3,'big')+first.to_bytes(3,'big')
    count+=1;raw_total+=len(raw)
  (cache/f'main_file_cache.idx{index}').write_bytes(result)
  counts[index]=count
store.f.close()
# Exhaustively check every preserved compressed archive against its source.
original=Cache(source);compact=Cache(cache);checked=0
for index in (4,14,15,255):
 entries=(cache/f'main_file_cache.idx{index}').read_bytes()
 for archive in range(len(entries)//6):
  if int.from_bytes(entries[archive*6:archive*6+3],'big'):
   assert original.read(index,archive)==compact.read(index,archive),(index,archive)
   checked+=1
original.f.close();compact.f.close()
for folder,target in [('music','music'),('fanfares','fanfares')]:
 shutil.copytree(bundle/'assets'/folder,out/target)
# Compare actual synthesis from original and repacked archives in separate JVMs.
java=bundle/'java-runtime/bin/java.exe';plugin=bundle/'plugin/hdos-music.jar'
midi=next((out/'music').glob('00002*'))
hashes=[]
for label,path in [('original',source),('compact',cache)]:
 wave=out/f'verification-{label}.wav'
 result=subprocess.run([str(java),'-cp',str(plugin),'main.HdosSynth',str(path),str(midi),str(wave),'3'],capture_output=True,text=True,timeout=60,check=True)
 hashes.append(hashlib.sha256(wave.read_bytes()).hexdigest())
 print(label,result.stdout.strip(),flush=True)
assert hashes[0]==hashes[1],'PCM output changed after soundbank compaction'
report={'preserved_archives':counts,'all_archive_bytes_verified':checked,'original_cache_bytes':sum(p.stat().st_size for p in source.iterdir() if p.is_file()),'compact_cache_bytes':sum(p.stat().st_size for p in cache.iterdir()),'three_second_autumn_voyage_wav_sha256':hashes[0],'pcm_identical':True}
(out/'verification.json').write_text(json.dumps(report,indent=2))
(out/'README.txt').write_text('Compact HDOS audio assets. cache/ contains only sound indexes4,14,15 plus reference metadata and empty compatibility index files. No maps, models, textures, or packed music archives are included. music/ contains826 MIDIs; fanfares/ contains396 MIDIs. Original archive IDs and bytes are preserved. Existing HDOS Music plugin can use these cache/ and music/ paths in its settings. This does not add missing patch658 or automatic jingle replacement. Redistribution permissions remain unresolved as documented in the original bundle. Verification WAVs are test outputs and excluded from the ZIP.\n')
archive=desktop/'HDOS-Music-Assets-Compact.zip'
with zipfile.ZipFile(archive,'x',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
 for p in sorted(out.rglob('*')):
  if p.is_file() and p.suffix!='.wav':z.write(p,out.name+'/'+p.relative_to(out).as_posix())
with zipfile.ZipFile(archive) as z:assert z.testzip() is None
print(json.dumps(report),flush=True)
print('Assets ZIP bytes:',archive.stat().st_size,flush=True)
