#!/usr/bin/env python3
"""Exercise prerelease gates without network, credentials, APK signing or publish."""
import json,os,pathlib,runpy,subprocess,tempfile,unittest,contextlib,io
from unittest.mock import patch
SCRIPT=pathlib.Path(__file__).resolve().parents[1]/'scripts/publish_public_beta.py'
CERT='7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290'
class PublishBetaTest(unittest.TestCase):
 def scenario(self,*,change=1,conclusion='success',certificate=CERT,existing=None):
  calls=[]
  def command(args,**kw):
   calls.append(args)
   if args[:2]==['git','diff']:return subprocess.CompletedProcess(args,change,'','')
   if args[:3]==['gh','release','view']:return subprocess.CompletedProcess(args,0 if existing else 1,json.dumps(existing) if existing else '','')
   if args[:2]==['gh','api']:
    return subprocess.CompletedProcess(args,0,json.dumps({'check_runs':[dict(name=n,status='completed',conclusion=conclusion) for n in ['build','emulator']]}),'')
   if args[:3]==['gh','release','create']:return subprocess.CompletedProcess(args,0,'created','')
   raise AssertionError(args)
  with tempfile.TemporaryDirectory() as tmp:
   previous=pathlib.Path.cwd();os.chdir(tmp)
   try:
    pathlib.Path('PianoLivreAndroid').mkdir();pathlib.Path('PianoLivreAndroid/gradle.properties').write_text('pianolivre.beta.versionName=0.9.31-open-beta\npianolivre.beta.versionCode=40\n')
    apk=pathlib.Path('dist/app/build/outputs/apk/debug/app-debug.apk');apk.parent.mkdir(parents=True);apk.write_bytes(b'synthetic apk')
    pathlib.Path('dist/build').mkdir();pathlib.Path('dist/build/public-signature.txt').write_text(certificate);pathlib.Path('dist/build/public-apk-metadata.txt').write_text("versionCode='40' versionName='0.9.31-open-beta'")
    error=None
    with patch.dict(os.environ,{'GITHUB_REPOSITORY':'example/public','GITHUB_SHA':'abc','BEFORE_SHA':'previous'}),patch('subprocess.run',side_effect=command),contextlib.redirect_stdout(io.StringIO()):
     try:runpy.run_path(str(SCRIPT),run_name='__main__')
     except SystemExit as ex:
      if ex.code!=0:error=ex
     except RuntimeError as ex:error=ex
    return calls,error
   finally:os.chdir(previous)
 def test_green_exact_commit_publishes_prerelease(self):
  calls,error=self.scenario();self.assertIsNone(error);publish=next(c for c in calls if c[:3]==['gh','release','create']);self.assertIn('--prerelease',publish);self.assertEqual(publish[publish.index('--target')+1],'abc')
 def test_failed_check_blocks(self):
  calls,error=self.scenario(conclusion='failure');self.assertIsNotNone(error);self.assertFalse(any(c[:3]==['gh','release','create'] for c in calls))
 def test_wrong_certificate_blocks(self):
  calls,error=self.scenario(certificate='different');self.assertIsNotNone(error);self.assertFalse(any(c[:3]==['gh','release','create'] for c in calls))
 def test_docs_without_version_skip(self):
  calls,error=self.scenario(change=0);self.assertIsNone(error);self.assertEqual(len(calls),1)
 def test_existing_same_commit_preserved(self):
  calls,error=self.scenario(existing={'targetCommitish':'abc'});self.assertIsNone(error);self.assertFalse(any(c[:3]==['gh','release','create'] for c in calls))
 def test_existing_different_commit_blocks(self):
  calls,error=self.scenario(existing={'targetCommitish':'other'});self.assertIsNotNone(error);self.assertFalse(any(c[:3]==['gh','release','create'] for c in calls))
if __name__=='__main__':unittest.main()
