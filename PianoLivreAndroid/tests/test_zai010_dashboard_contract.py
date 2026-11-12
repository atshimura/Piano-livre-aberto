#!/usr/bin/env python3
"""ZAI-010: offline navigation, contextual landscape dashboard and safe data invariants."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def src(name):
 return (root/"app/src/main/java/br/org/pianolivre"/name).read_text(encoding="utf-8")
landing=src("MusicLabLandingScreen.java")
screens=src("PianoScreens.java")
creation=src("MusicCreationScreen.java")
quant=src("ScoreRhythmQuantizer.java")
xml=src("ScoreXmlComposer.java")
preview=src("CreationScorePreviewScreen.java")
assert 'setOrientation(HORIZONTAL)' in landing
assert 'lab-landing-three-columns' in landing
for expected in ["Escolha um modo","Descobrir","Aprender","Abrir Aprender","Criação",
                 "Explore notas, acordes","lições progressivas","transforme suas gravações"]:
 assert expected in landing,expected
assert 'card("♧","Aprender"' in landing
assert '"Abrir Aprender   →",true,goLearn' in landing, "Learn is now active in ZAI-015"
assert "labLanding=new MusicLabLandingScreen" in screens
assert "void showDiscover()" in screens and "void showLab()" in screens
assert 'if(labLanding!=null)labLanding.setVisibility(View.VISIBLE)' in screens
assert "void backFromLab()" in screens and "labLanding.getVisibility()==View.VISIBLE" in screens
assert "creation.requestLeave();return;" in screens, "unsaved recording cannot be skipped"
assert "dashboard();leftMenu();showRightRecent()" in creation
for expected in ["creation-dashboard-header","creation-dashboard-two-columns",
                 "creation-dashboard-actions","creation-dashboard-context",
                 "Gravações recentes","Ver todas","Minhas criações",
                 "Revisar gravação","creation-review-back",
                 "← Voltar às gravações recentes","creation-capture-actions",
                 "Informações USB-MIDI","a.showMidiDiagnostics()",
                 "creation-live-recording-card","creation-return-to-review"]:
 assert expected in creation,expected
assert 'responsiveActions(recordingReviewPanel,9,save,preview,export)' in creation
assert 'attach(dashboardLeft,captureActions,52,9)' in creation
assert 'attach(recordingReviewPanel,discard,48,7)' in creation
assert '"Gravação ainda não salva"' in creation
assert "showRightRecent()" in creation and "showRightReview(recordingReviewPanel)" in creation
assert "requestList()" in creation and "hasUnsavedTake()" in creation
assert "a.exportCreationTake(" in creation
assert "store.saveRecoverable(" in creation and "new CreationJournal(" in creation
assert "store.deletePermanently(id)" in creation and "CreationScoreDeletion.delete" in creation
assert "button(\"Excluir somente a partitura…" in creation
assert 'String[] names={"Todas","Com partitura","Só gravação","Recuperáveis","Lixeira"}' in creation
assert "scoreStore.has(item.id)" in creation
assert "catalogFilter==3?CreationProjectStore.Filter.RECOVERABLE" in creation
assert 'detailsBack="home".equals(page)?"home":"list"' in creation
assert 'String[] sortNames={"Mais recentes","Mais antigos","Nome A–Z"' in creation
assert "query=search.getText().toString()" in creation
assert "showDetails(p.id)" in creation
assert "ZAI-010-simple-figures" in xml
assert "PITCH_FIGURES = {16, 8, 4, 2, 1}" in quant
assert "for (int candidate : PITCH_FIGURES)" in quant
assert 'if (token.rest && dotted(token.cells)) xml.append("<dot/>")' in xml
assert "isSimplePitchFigure(token.cells)" in xml
assert "isCurrentDerivedXml(prior.xml)" in preview
assert "Gerar partitura básica" in preview
print("ZAI010_LAB_DASHBOARD_FIGURES_CONTRACT_PASS")
