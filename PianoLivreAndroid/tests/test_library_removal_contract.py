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
        self.assertIn('ImportedScoreDeletion.delete(a.getFilesDir(),key,()->LibraryStore.removeImportedEveryProfile(a.prefs,key))',self.ui)
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
        self.assertIn('updateMetadata.run()',self.files)
        self.assertIn('marker(new File(stage,COMMITTED),"ok")',self.files)
        self.assertIn('try{recover(root);}catch(IOException rollback)',self.files)
        self.assertIn('ImportedScoreDeletion.recover(getFilesDir())',self.main)
        self.assertIn('removeImportedEveryProfile',self.store)
        self.assertIn('if(detach(obj,score))',self.store)
        self.assertIn('editor.remove(preference)',self.store)
        self.assertIn('ImportedScoreDeletion.delete(root,imported',self.selftest)

    def test_study_history_preserved(self):
        self.assertNotIn('new StudyHistory(',self.store)
        self.assertNotIn('.clear()',self.files)
        self.assertIn('void clearRemovedScoreInMemory(String scoreKey)',self.main)

if __name__=="__main__":
    unittest.main()
