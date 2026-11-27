from pathlib import Path
import hashlib,json,zipfile,subprocess,tempfile

project=Path(__file__).resolve().parents[1]
bundle=project.parent/'HDOS Music - Windows'
archive=project.parent/'HDOS-Music-Windows-0.1.0.zip'
# The first reduced Java image lacked optional RuneLite modules. Ship only
# java-runtime, whose module list was checked with jdeps.
def payload():
    return sorted(p for p in bundle.rglob('*') if p.is_file() and p.relative_to(bundle).parts[0]!='runtime' and p.name!='checksums.sha256')
summary=bundle/'reports/hdos-validation-summary.json'
data=json.loads(summary.read_text())
data['cache']='assets/cache (bundled read-only HDOS cache)'
summary.write_text(json.dumps(data,indent=2)+'\n')
files=payload()
for p in files:
    rel=p.relative_to(bundle)
    assert not any(x.lower() in {'.git','.gradle','profiles2','credentials.properties','session','test-console.log','test-error.log'} for x in rel.parts),rel
lines=[]
for p in files:
    lines.append(hashlib.file_digest(p.open('rb'),'sha256').hexdigest()+'  '+p.relative_to(bundle).as_posix())
(bundle/'checksums.sha256').write_text('\n'.join(lines)+'\n',encoding='utf-8')
files.append(bundle/'checksums.sha256')
with zipfile.ZipFile(archive,'x',compression=zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
    for p in files:z.write(p,bundle.name+'/'+p.relative_to(bundle).as_posix())
print(f'ZIP created: {archive} ({archive.stat().st_size/1024/1024:.1f} MiB)',flush=True)
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None,'ZIP CRC verification failed'
    home_tmp=Path.home()/'tmp';home_tmp.mkdir(exist_ok=True)
    verification=Path(tempfile.mkdtemp(prefix='HDOS Portable Verify ',dir=home_tmp)).resolve()
    for name in z.namelist():
        assert (verification/name).resolve().is_relative_to(verification),name
    z.extractall(verification)
root=verification/bundle.name
for line in (root/'checksums.sha256').read_text().splitlines():
    expected,name=line.split('  ',1)
    with (root/name).open('rb') as f:assert hashlib.file_digest(f,'sha256').hexdigest()==expected,name
command=[str(root/'java-runtime/bin/java.exe'),'-ea','-Xmx1024m','-Duser.home='+str(verification/'Clean Recipient Home'),'-Dhdos.bundle='+str(root),'-cp',str(root/'launcher.jar')+';'+str(root/'client/*'),'com.brandon.hdos.launcher.Bootstrap','--check']
result=subprocess.run(command,cwd=root,text=True,capture_output=True,timeout=60)
print(result.stdout,flush=True)
if result.stderr:print(result.stderr,flush=True)
assert result.returncode==0,'Extracted bundle installation check failed'
assert not (verification/'Clean Recipient Home/.runelite').exists()
sha=hashlib.file_digest(archive.open('rb'),'sha256').hexdigest()
(archive.with_suffix('.zip.sha256')).write_text(sha+'  '+archive.name+'\n')
print(f'Verified {len(files)} payload files, ZIP CRCs, SHA256 hashes, and native synthesis from a clean extracted path.',flush=True)
print(f'Verification folder: {verification}',flush=True)
