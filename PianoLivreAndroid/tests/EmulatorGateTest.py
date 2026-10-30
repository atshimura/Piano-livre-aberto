"""The required emulator check must fail closed, including skipped/cancelled jobs."""
import itertools,os,pathlib,re,shutil,subprocess,textwrap,unittest
class EmulatorGateTest(unittest.TestCase):
    def test_real_gate_commands_for_all_dependency_outcomes(self):
        workflow=(pathlib.Path(__file__).resolve().parents[2]/'.github/workflows/emulator.yml').read_text(encoding='utf-8')
        block=re.search(r'(?ms)^  emulator:\n(.*?)(?=^  [\w-]+:\n|\Z)',workflow).group(1)
        self.assertIn('needs: [debug-apk, emulator-suite]',block)
        self.assertIn('if: ${{ always() }}',block)
        self.assertIn('DEBUG_RESULT: ${{ needs.debug-apk.result }}',block)
        self.assertIn('FULL_RESULT: ${{ needs.emulator-suite.result }}',block)
        self.assertNotIn('continue-on-error',block)
        commands=textwrap.dedent(block.split('run: |',1)[1]).strip()
        bash=shutil.which('bash');self.assertIsNotNone(bash,'Bash required to execute actual Actions guard')
        for debug,full in itertools.product(('success','failure','cancelled','skipped'),repeat=2):
            with self.subTest(debug=debug,full=full):
                result=subprocess.run([bash,'-e','-c',commands],env={**os.environ,'DEBUG_RESULT':debug,'FULL_RESULT':full},capture_output=True)
                self.assertEqual(result.returncode==0,debug==full=='success')
if __name__=='__main__':unittest.main()
