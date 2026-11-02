#!/usr/bin/env python3
"""Basic source contracts for Home change #1; no APK, emulator or full suite."""
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]
SCREENS=ROOT/"app/src/main/java/br/org/pianolivre/PianoScreens.java"
TESTS=ROOT/"tests"

def method(source,name,next_name):
    start=source.index(" void "+name+"(){")
    end=source.index(" void "+next_name+"(){",start)
    return source[start:end]

class BannerShortcutsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.src=SCREENS.read_text(encoding="utf-8")
        cls.banner=method(cls.src,"buildWelcomeBanner","buildLibraryPanel")
        cls.settings=method(cls.src,"options","buildStudio")

    def test_equal_width_height_and_blue_green(self):
        b=self.banner
        self.assertEqual(b.count('new LinearLayout.LayoutParams(0,d(44),1)'),2)
        self.assertIn('libraryAction.setBackground(bg(blue,12,0))',b)
        self.assertIn('labAction.setBackground(bg(0xff128263,12,0))',b)
        self.assertIn('libraryAction.setTextColor(Color.WHITE)',b)
        self.assertIn('labAction.setTextColor(Color.WHITE)',b)

    def test_expected_destinations_and_accessibility(self):
        b=self.banner
        self.assertIn('btn("Biblioteca Musical",true,()->focusLibrary())',b)
        self.assertIn('btn("Laboratório Musical",true,()->showLab())',b)
        self.assertEqual(b.count('setTag("home-banner-action")'),2)
        self.assertIn('Biblioteca Musical. Suas partituras para estudar e tocar',b)
        self.assertIn('Laboratório Musical. Notas, escalas, acordes e exercícios',b)

    def test_old_buttons_and_cards_absent(self):
        self.assertNotIn("buildNavigationCards();",self.src)
        self.assertNotIn("void buildNavigationCards()",self.src)
        self.assertNotIn("home-shortcut",self.src)
        self.assertNotIn('btn("▶  Começar pela Ode à Alegria"',self.src)
        self.assertNotIn('btn("▣  Como usar"',self.src)
        self.assertIn("buildWelcomeBanner();",self.src)
        self.assertIn("buildLibraryPanel();",self.src)

    def test_gear_retains_history_help_and_all_settings(self):
        b=self.settings
        for label in ("Histórico","Como usar","Fazer backup","Restaurar backup",
                      "Importar MusicXML","Calibrar acordes MIDI",
                      "Preparação do Espera","Sobre esta versão","Gerenciar perfil"):
            self.assertIn('"'+label+'"',b)
        self.assertIn("if(i==0)a.showHistory()",b)
        self.assertIn("else if(i==1)showHelp()",b)
        self.assertIn("else if(i==8)a.manageProfile()",b)

    def test_library_toolbar_equal_three_controls_and_order(self):
        s=self.src
        start=s.index(" void buildLibraryPanel(){")
        end=s.index(" void focusLibrary(){",start)
        toolbar=s[start:end]
        self.assertEqual(toolbar.count('new LinearLayout.LayoutParams(0,d(44),1)'),3)
        self.assertLess(toolbar.index('toolbar.addView(collections,'),toolbar.index('toolbar.addView(importButton,'))
        self.assertLess(toolbar.index('toolbar.addView(importButton,'),toolbar.index('toolbar.addView(librarySearch,'))
        self.assertIn('home-library-collections',toolbar)
        self.assertIn('home-library-import',toolbar)
        self.assertIn('home-library-search',toolbar)
        self.assertIn('int toolbarGap=availableDp<300?4:8',toolbar)
        self.assertIn('boolean tight=availableDp<390||fontScale>1.30f',toolbar)

    def test_typography_not_shrunk_and_category_removed(self):
        s=self.src
        self.assertIn('collections.setTextSize(13)',s)
        self.assertIn('importButton.setTextSize(13)',s)
        self.assertIn('librarySearch.setTextSize(availableDp<320?14:16)',s)
        self.assertIn('librarySearch.setHint(tight?',s)
        self.assertNotIn('Spinner libraryFilter',s)
        self.assertNotIn('home-filter',s)
        self.assertNotIn('Filtrar biblioteca',s)

    def test_collections_preserves_filters_and_persistence(self):
        s=self.src
        self.assertIn('"Todas as partituras","Favoritos","Ver coleção"',s)
        self.assertIn('void chooseLibraryCollection()',s)
        self.assertIn('selectedLibraryFilter=which==0?"":"*"',s)
        self.assertIn('metadata.collections.get(selectedLibraryFilter)',s)
        self.assertIn('metadata.favorites.contains(key)',s)
        self.assertIn('libraryUser!=a.user',s)
        self.assertIn('new LibraryStore(a.prefs,a.user).create(name)',s)
        self.assertIn('new LibraryStore(a.prefs,a.user).delete(selected.id)',s)

    def test_existing_emulator_routes_updated(self):
        for name in ("emulator_ui.py","emulator_ui_fast.py","emulator_release.py",
                     "emulator_public_upgrade.py","emulator_study_session.py"):
            s=(TESTS/name).read_text(encoding="utf-8")
            self.assertNotIn("Começar pela Ode à Alegria",s,name)
        ui=(TESTS/"emulator_ui.py").read_text(encoding="utf-8")
        self.assertIn('tap_text(["Como usar"],exact=True)',ui)
        self.assertIn('tap_text(["Histórico"],exact=True)',ui)

    def test_layout_regression_now_requires_two_actions(self):
        l=(TESTS/"home_layout_checks.py").read_text(encoding="utf-8")
        self.assertIn("assert len(shortcuts)==2",l)
        self.assertIn("not equal width",l)
        self.assertIn("not equal height",l)
        self.assertIn("('Histórico','Como usar','Calibrar acordes MIDI')",l)
        self.assertNotIn("len(shortcuts)==3",l)

if __name__=="__main__":
    unittest.main()
