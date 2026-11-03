#!/usr/bin/env python3
"""Targeted source invariants for the Library deletion UI (no APK or emulator)."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
def source(name):
    return (ROOT/"app/src/main/java/br/org/pianolivre"/name).read_text(encoding="utf-8")

class LibraryRemovalContracts(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ui=source("PianoScreens.java")
        cls.store=source("LibraryStore.java")
        cls.files=source("ImportedScoreDeletion.java")
        cls.main=source("MainActivity.java")
        cls.selftest=source("LibrarySelfTest.java")
        cls.emulator_ui=(ROOT/"tests/emulator_ui.py").read_text(encoding="utf-8")
        cls.c2_smoke=(ROOT/"tests/c2_d_ui_smoke.py").read_text(encoding="utf-8")
        cls.emulator_workflow=(ROOT.parent/".github/workflows/emulator.yml").read_text(encoding="utf-8")

    def test_discovery_and_accessible_options(self):
        self.assertIn('card.setOnLongClickListener(v->{scoreActions(entry[0],scoreKey);return true;});',self.ui)
        self.assertIn('new String[]{"Organizar em coleção","Excluir da biblioteca"}',self.ui)
        self.assertIn('restoreHiddenScores()',self.ui)
        self.assertIn('"Restaurar músicas ocultas"',self.ui)
        self.assertIn('setHidden(keys.get(index),false)',self.ui)
        self.assertIn('metadata.hiddenScores.contains(key)',self.ui)

    def test_types_have_distinct_paths_and_consent(self):
        self.assertIn('setPositiveButton(builtIn?"Ocultar":"Excluir"',self.ui)
        self.assertIn('setNegativeButton("Cancelar",null)',self.ui)
        self.assertIn('new LibraryStore(a.prefs,a.user).setHidden(key,true)',self.ui)
        self.assertIn('new LibraryStore(a.prefs,a.user).removeCreation(key.substring(15))',self.ui)
        self.assertIn('ImportedScoreDeletion.delete(a.getFilesDir(),key,scoreKey->LibraryStore.removeImportedEveryProfile(a.prefs,scoreKey))',self.ui)
        self.assertNotIn('new CreationScoreStore(a.getFilesDir()).delete(',self.ui)

    def test_backward_compatible_hidden_metadata(self):
        self.assertIn('root.has("hiddenScores")?root.getJSONArray("hiddenScores"):new JSONArray()',self.store)
        self.assertIn('for(String score:hidden)if(!score.startsWith("scores/"))',self.store)
        self.assertIn('hiddenScores=Collections.unmodifiableSet(hidden)',self.store)
        self.assertIn('obj.put("hiddenScores",next);save(obj,hidden?score:null)',self.store)
        self.assertIn('detach(obj,"creation-score:"+projectId)',self.store)
        self.assertIn('save(obj,"creation-score:"+projectId)',self.store)

    def test_import_deletion_is_transactional_and_recoverable(self):
        self.assertIn('move(score,new File(stage,filename))',self.files)
        self.assertIn('source.exists())move(source,new File(stage,filename+".source.mid"))',self.files)
        self.assertIn('updateMetadata.accept(key)',self.files)
        self.assertIn('marker(new File(stage,COMMITTED),"ok")',self.files)
        self.assertIn('try{recover(root,updateMetadata);}catch(IOException rollback)',self.files)
        self.assertIn('ImportedScoreDeletion.recover(getFilesDir(),scoreKey->LibraryStore.removeImportedEveryProfile(prefs,scoreKey))',self.main)
        self.assertIn('removeImportedEveryProfile',self.store)
        self.assertIn('if(detach(obj,score))',self.store)
        self.assertIn('editor.remove(preference)',self.store)
        self.assertIn('ImportedScoreDeletion.delete(root,imported',self.selftest)
        self.assertLess(self.files.index('marker(new File(stage,COMMITTED),"ok")'),
                        self.files.index('updateMetadata.accept(key)'))
        self.assertIn('if(committed&&!metadataAlreadyUpdated)',self.files)
        self.assertIn('finishMetadata.accept("imported/"+filename)',self.files)
        self.assertIn('recover(root,updateMetadata,true)',self.files)
        self.assertIn('hidden state survives backup/restore',self.selftest)
        self.assertIn('unlinked creation cleans stale collection',self.selftest)

    def test_study_history_preserved(self):
        self.assertNotIn('new StudyHistory(',self.store)
        self.assertNotIn('.clear()',self.files)
        self.assertIn('void clearRemovedScoreInMemory(String scoreKey)',self.main)

    def test_full_emulator_suite_exercises_import_deletion_and_recovery(self):
        self.assertIn("python3 tests/emulator_ui.py",self.emulator_workflow)
        self.assertIn('long_press_library_card("Synthetic.musicxml")',self.emulator_ui)
        self.assertIn('tap_text(["Excluir da biblioteca"],exact=True)',self.emulator_ui)
        self.assertIn('assert not imported_file_present("Synthetic.musicxml")',self.emulator_ui)
        self.assertIn('assert not imported_file_present("Synthetic.musicxml.source.mid")',self.emulator_ui)
        self.assertNotIn('"rm","files/imported/Synthetic.musicxml"',self.emulator_ui)
        self.assertIn('wait_for_text("Partitura removida da Biblioteca Musical.")',self.emulator_ui)
        self.assertIn('focus_library_search_field()',self.emulator_ui)

    def test_full_emulator_suite_exercises_hide_restore_and_creation_unlink(self):
        self.assertIn("python3 tests/c2_d_ui_smoke.py",self.emulator_workflow)
        self.assertIn('long_press_library_card(DEFAULT_SCORE_TITLE)',self.emulator_ui)
        self.assertIn('tap_text(["Restaurar músicas ocultas"],exact=True,scroll=True)',self.emulator_ui)
        self.assertIn('wait_for_text("9 de 9 partituras")',self.emulator_ui)
        self.assertIn('wait_for_text("10 de 10 partituras")',self.emulator_ui)
        self.assertIn('project_id=linked_creation_id()',self.c2_smoke)
        self.assertIn('find("Excluir da biblioteca",clickable=True)',self.c2_smoke)
        self.assertIn('not nodes("Estudar C2-D partitura de teste",clickable=True)',self.c2_smoke)
        self.assertIn('private_file_exists("files/creations/"+project_id+".plc")',self.c2_smoke)
        self.assertIn('private_file_exists("files/creation-scores/"+project_id+".pls")',self.c2_smoke)

if __name__=="__main__":
    unittest.main()
