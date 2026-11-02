#!/usr/bin/env python3
"""Basic contracts: generated MusicXML joins Library by reference, never a copy."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
def src(name):
    return (ROOT/"app/src/main/java/br/org/pianolivre"/name).read_text(encoding="utf-8")
LIB=src("LibraryStore.java")
MAIN=src("MainActivity.java")
UI=src("PianoScreens.java")
CREATE=src("MusicCreationScreen.java")
PREVIEW=src("CreationScorePreviewScreen.java")
SEARCH=src("LibrarySearch.java")
BACKUP=src("BackupArchive.java")
SESSION=src("SessionFiles.java")
SMOKE=(ROOT/"tests/c2_d_ui_smoke.py").read_text(encoding="utf-8")

class CreationLibraryContracts(unittest.TestCase):
    def test_only_explicit_add_registers_and_no_duplicate(self):
        self.assertIn('void addCreation(String projectId)',LIB)
        self.assertIn('if(creationIds(previous).contains(projectId))return;',LIB)
        self.assertIn('obj.put("addedCreations",updated);save(obj)',LIB)
        self.assertIn('addedCreations=Collections.unmodifiableSet(added)',LIB)
        self.assertIn('root.has("addedCreations")?root.getJSONArray("addedCreations"):new JSONArray()',LIB)
        self.assertIn('validateCreationId(key.substring(15))',LIB)

    def test_ui_requires_saved_score_and_has_no_study_shortcut(self):
        self.assertIn('if(saved){',PREVIEW)
        self.assertIn('Button add=button("Adicionar à Biblioteca"',PREVIEW)
        self.assertIn('Button add=button("Adicionar à Biblioteca"',CREATE)
        self.assertIn('if(hasScore&&!new LibraryStore(a.prefs,a.user).hasCreation(id))',CREATE)
        self.assertNotIn('button("Estudar esta partitura"',PREVIEW)
        self.assertNotIn('button("Estudar esta partitura"',CREATE)
        self.assertIn('Salve a partitura antes de adicioná-la',PREVIEW)
        self.assertIn('✓ Na Biblioteca Musical',PREVIEW)

    def test_addition_verifies_owner_integrity_and_thread_context(self):
        self.assertIn('new Thread(()->{',MAIN)
        self.assertIn('new CreationScoreStore(getFilesDir()).read(id)',MAIN)
        self.assertIn('source.profileId!=owner||source.trash||source.recoverable',MAIN)
        self.assertIn('if(failure==null&&user==owner)',MAIN)
        self.assertIn('runOnUiThread(()->{',MAIN)
        self.assertIn('new LibraryStore(prefs,owner).addCreation(id)',MAIN)
        self.assertIn('screens.refreshLibrary()',MAIN)
        self.assertIn('onDone.accept(added)',MAIN)

    def test_normal_library_routes_no_musicxml_duplication(self):
        self.assertIn('for(String id:metadata.addedCreations)',UI)
        self.assertIn('project.profileId==a.user&&!project.trash&&!project.recoverable',UI)
        self.assertIn('entries.add(new String[]{project.title,"creation-score:"+id})',UI)
        self.assertIn('a.loadScore(entry[1],true)',UI)
        self.assertIn('a.openFreeScore(entry[1])',UI)
        self.assertIn('void backFromStudy(){',UI)
        self.assertIn('showHome();',UI.split("void backFromStudy(){",1)[1].split("void showStudy(){",1)[0])
        self.assertIn('source.startsWith("creation-score:")?source:',SEARCH)
        self.assertIn('if(path.startsWith("creation-score:"))',SESSION)
        self.assertNotIn('MidiImportStorage.save(',MAIN.split('void addCreationScoreToLibrary',1)[1].split('void loadScore(',1)[0])

    def test_profile_backups_and_stale_entries(self):
        self.assertIn('key="user_"+user+"_library"',LIB)
        self.assertIn('LibraryStore.decode((String)item.getValue())',BACKUP)
        self.assertIn('if(project.profileId==a.user&&!project.trash&&!project.recoverable&&scoreStore.has(id))',UI)
        self.assertIn('catch(Exception absentOrDeleted)',UI)
        self.assertIn('store.deletePermanently(id)',CREATE)

    def test_c2d_ui_contract_updated_without_running_emulator(self):
        self.assertIn('click("Adicionar à Biblioteca")',SMOKE)
        self.assertIn('find("Na Biblioteca Musical")',SMOKE)
        self.assertIn('study-via-library',SMOKE)
        self.assertNotIn('click("Estudar esta partitura")',SMOKE)

if __name__=="__main__":
    unittest.main()
