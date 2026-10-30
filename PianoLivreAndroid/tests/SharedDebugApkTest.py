"""Provenance checks prevent installing bytes from another commit/run."""
import contextlib,io,pathlib,runpy,tempfile,unittest,os
from unittest.mock import patch
SCRIPT=pathlib.Path(__file__).resolve().parents[1]/'scripts/shared_debug_apk.py'
class SharedDebugApkTest(unittest.TestCase):
    def test_roundtrip_and_reject_wrong_source_or_bytes(self):
        with tempfile.TemporaryDirectory() as tmp:
            old=os.getcwd();os.chdir(tmp)
            try:
                folder=pathlib.Path('app/build/outputs/apk/debug');folder.mkdir(parents=True)
                apk=folder/'app-debug.apk';apk.write_bytes(b'APK fixture')
                store=pathlib.Path(tmp)/'configured-debug.keystore';store.write_bytes(b'disposable fixture')
                report='Variant: debug\nConfig: debug\nStore: '+str(store)+'\nAlias: AndroidDebugKey\n'
                def invoke(mode,sha='current-checkout',signing=report):
                    with patch('sys.argv',[str(SCRIPT),mode]),patch('subprocess.check_output',side_effect=[sha+'\n',signing]),contextlib.redirect_stdout(io.StringIO()):
                        runpy.run_path(str(SCRIPT),run_name='__main__')
                invoke('export');store.unlink();invoke('restore')
                self.assertEqual(store.read_bytes(),b'disposable fixture')
                with self.assertRaisesRegex(AssertionError,'Different source'):invoke('verify',sha='other-checkout')
                apk.write_bytes(b'different APK')
                with self.assertRaisesRegex(AssertionError,'Different APK'):invoke('restore')
                with self.assertRaisesRegex(AssertionError,'disposable debug'):invoke('export',signing='Variant: debug\nConfig: publicBeta\nStore: permanent\n')
            finally:os.chdir(old)
if __name__=='__main__':unittest.main()
