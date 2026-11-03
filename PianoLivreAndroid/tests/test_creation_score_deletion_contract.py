#!/usr/bin/env python3
"""Implementation 2: deletion of derived score and preservation of original MIDI."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
def java(name):
    return (ROOT/"app/src/main/java/br/org/pianolivre"/name).read_text(encoding="utf-8")
class Contract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ui=java("MusicCreationScreen.java")
        cls.score=java("CreationScoreDeletion.java")
        cls.store=java("LibraryStore.java")
        cls.main=java("MainActivity.java")
        cls.selftest=java("LibrarySelfTest.java")
    def test_separate_actions_and_consents(self):
        for text in ('button("Excluir somente a partitura…"', 'button("Mover composição para a Lixeira"',
                     'button("Excluir definitivamente…"','setPositiveButton("Excluir partitura"',
                     'setNegativeButton("Cancelar",null)','"Gerar partitura"'):
            self.assertIn(text,self.ui)
        self.assertIn('if(hasScore){',self.ui)
        isolated=self.ui.split('private void deleteOnlyScore(',1)[1].split('private void updatePlaybackControls()',1)[0]
        self.assertNotIn('store.deletePermanently(id)',isolated)
        self.assertNotIn('store.moveToTrash(id',isolated)
        self.assertIn('showDetails(id)',isolated)
    def test_owner_project_and_midi(self):
        self.assertIn('original.profileId!=owner||original.trash||original.recoverable',self.ui)
        self.assertIn('new CreationScoreStore(a.getFilesDir()).read(id)',self.ui)
        self.assertIn('CreationScoreDeletion.delete(a.getFilesDir(),id,projectId->',self.ui)
        self.assertIn('if(!project.isFile()',self.score)
        self.assertIn('a.clearRemovedScoreInMemory("creation-score:"+id)',self.ui)
        self.assertIn('CreationScoreDeletion.recover(a.getFilesDir(),projectId->',self.ui)
        details=self.ui.split('private void renderDetails(',1)[1].split('private void confirmDeleteOnlyScore(',1)[0]
        self.assertLess(details.index('CreationScoreDeletion.recover('),details.index('base("Detalhes da composição"'))
        self.assertIn('original MIDI bytes preserved after score deletion',self.selftest)
    def test_crash_recovery_and_prefs_cleanup(self):
        self.assertLess(self.score.index('marker(new File(stage,COMMITTED),"ok")'),self.score.index('detach.accept(projectId);',self.score.index('public static synchronized void delete(')))
        self.assertIn('if(!metadataApplied)try{detach.accept(projectId);}',self.score)
        self.assertIn('recover(root,detach,true)',self.score)
        self.assertIn('static void removeCreationEveryProfile(SharedPreferences prefs,String projectId)',self.store)
        self.assertIn('if(changed){originals.put(preference',self.store)
        self.assertIn('if(!stored.isEmpty()&&score.equals(SessionFiles.portable(stored)))',self.store)
        self.assertIn('if(!editor.commit())',self.store)
    def test_full_suite_already_includes_implementation_two(self):
        smoke=(ROOT/"tests/c2_d_ui_smoke.py").read_text(encoding="utf-8")
        native=java("CreationSelfTest.java")
        workflow=(ROOT.parent/".github/workflows/emulator.yml").read_text(encoding="utf-8")
        self.assertIn('python3 tests/c2_d_ui_smoke.py',workflow)
        self.assertIn('click("Excluir somente a partitura…")',smoke)
        self.assertIn('click("Cancelar")',smoke)
        self.assertIn('click("Excluir partitura")',smoke)
        self.assertIn('original_bytes',smoke)
        self.assertIn('assert after_bytes==original_bytes',smoke)
        self.assertIn('find("Gerar partitura",clickable=True)',smoke)
        self.assertIn('project_id not in json.loads(entry.text).get("addedCreations",[])',smoke)
        self.assertIn('findButton(screen,"Mover composição para a Lixeira")',native)
        self.assertNotIn('"Mover para Lixeira"',native)

    def test_recover_before_backup_and_startup(self):
        self.assertIn('CreationScoreDeletion.recover(getFilesDir(),id->LibraryStore.removeCreationEveryProfile(prefs,id))',self.main)
        self.assertIn('BackupArchive.export(prefs,getFilesDir()',self.main)
        self.assertIn('BackupArchive.create(prefs,getFilesDir())',self.main)
        self.assertNotIn('new StudyHistory(',self.store)
if __name__=="__main__":unittest.main()
