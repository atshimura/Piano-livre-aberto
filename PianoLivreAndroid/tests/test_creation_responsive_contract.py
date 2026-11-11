#!/usr/bin/env python3
"""Basic creation UI contracts (Blocks A/B/C), without APK or emulator."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
CREATION=(ROOT/"app/src/main/java/br/org/pianolivre/MusicCreationScreen.java").read_text(encoding="utf-8")
SCORE=(ROOT/"app/src/main/java/br/org/pianolivre/CreationScorePreviewScreen.java").read_text(encoding="utf-8")

def section(source, start, end):
    first=source.index(start)
    return source[first:source.index(end,first)]

class CreationResponsiveContracts(unittest.TestCase):
    def test_grouping_uses_actual_container_and_preserves_font(self):
        self.assertIn('private LinearLayout responsiveActions(LinearLayout parent,int bottom,Button... buttons)',CREATION)
        self.assertIn('reflow.accept(0)',CREATION)
        self.assertIn('button.getParent() instanceof android.view.ViewGroup',CREATION)
        self.assertIn('((android.view.ViewGroup)button.getParent()).removeView(button)',CREATION)
        self.assertIn('group.post(()->{if(group.getWidth()>0)reflow.accept(group.getWidth());})',CREATION)
        self.assertIn('getResources().getConfiguration().fontScale',CREATION)
        self.assertIn('int columns=width<=0?1:Math.max(1,Math.min(buttons.length,',CREATION)
        self.assertIn('button.setSingleLine(false);button.setMaxLines(2)',CREATION)
        self.assertIn('b.setTextSize(14)',CREATION)
        self.assertIn('b.setMinHeight(dp(48))',CREATION)

    def test_block_a_home_recorder_and_project_toolbar(self):
        self.assertIn('dashboard();leftMenu();showRightRecent()',CREATION)
        self.assertIn('responsiveActions(recordingReviewPanel,9,save,preview,export)',CREATION)
        self.assertIn('setShown(revisionActions,review)',CREATION)
        self.assertNotIn('attach(body,export,48,9)',CREATION)
        self.assertIn('attach(recordingReviewPanel,discard,48,7)',CREATION)
        self.assertIn('confirmDiscard()',CREATION)
        self.assertIn('creation-capture-actions',CREATION)
        self.assertIn('creation-project-toolbar',CREATION)
        self.assertIn('boolean wideList=',CREATION)
        self.assertIn('getResources().getConfiguration().fontScale<=1.25f',CREATION)
        for label in ('Buscar composição','Filtrar composições','Ordenar composições'):
            self.assertIn(label,CREATION)

    def test_block_b_details_player_and_safe_data_actions(self):
        details=section(CREATION,'private void renderDetails(','private void updatePlaybackControls(){')
        self.assertIn('new Button[]{play,pause,resume,stop}',details)
        self.assertIn('setShown(detailPlay,!playing&&!paused)',CREATION)
        self.assertIn('responsiveActions(scoreCard,5,viewScore',details)
        self.assertIn('responsiveActions(panel,12,rename,export,infoButton)',details)
        self.assertIn('infoButton.setOnClickListener',details)
        self.assertIn('store.restore(id,System.currentTimeMillis())',details)
        self.assertIn('store.deletePermanently(id)',details)
        self.assertIn('.setPositiveButton("Excluir"',details)
        self.assertIn('attach(panel,trash,48,0)',details)
        self.assertIn('attach(panel,warning,-2,12)',details)
        self.assertIn('finalizeRecovery(id,System.currentTimeMillis())',details)
        self.assertIn('button("Excluir somente a partitura…"',details)
        self.assertIn('button("Mover composição para a Lixeira"',details)
        self.assertIn('setPositiveButton("Excluir partitura"',details)
        self.assertIn('A gravação MIDI original será preservada.',details)
        self.assertIn('CreationScoreDeletion.delete(a.getFilesDir()',details)

    def test_block_c_preview_is_clean_and_zoomable(self):
        # Z-P3: preview without musical adjustments, with zoom and fit-to-width.
        self.assertIn('private void actions(LinearLayout parent,Button... buttons)',SCORE)
        self.assertIn('((android.view.ViewGroup)button.getParent()).removeView(button)',SCORE)
        self.assertIn('group.post(()->{if(group.getWidth()>0)reflow.accept(group.getWidth());})',SCORE)
        self.assertIn('button.setSingleLine(false);button.setMaxLines(2)',SCORE)
        self.assertIn('b.setTextSize(13)',SCORE)
        self.assertIn('b.setMinHeight(dp(48))',SCORE)
        self.assertIn('actions(contents,back,save,add)',SCORE)
        self.assertIn('actions(contents,back,save)',SCORE)
        self.assertIn('button("A−",false,()->applyZoom(zoom/1.25f))',SCORE)
        self.assertIn('button("A+",false,()->applyZoom(zoom*1.25f))',SCORE)
        self.assertIn('button("Ajustar à largura",false,()->applyZoom(1.0f))',SCORE)
        self.assertIn('setCreationZoom(zoom)',SCORE)
        self.assertIn('Andamento estimado: ',SCORE)
        self.assertIn('Andamento não identificado com segurança — prévia a 120 BPM (provisória)',SCORE)
        self.assertIn('getResources().getDisplayMetrics().heightPixels*0.65f',SCORE)
        # Retired C2 controls must not return in another screen of the preview.
        for retired in ('creation-score-note-row','Button chord=button("Acorde"','Selecionar intervalo de compassos',
                        'Desfazer','Refazer','Restaurar sugestão','Concluir ajustes',
                        'responsiveFields','scrollableSettings','Uniformizar trecho','Atribuir mão / voz'):
            self.assertNotIn(retired,SCORE)

    def test_block_c_retired_adjustment_pipeline_is_gone(self):
        self.assertNotIn('review.configure(bpm,beats,beatType,step,chosenRegularization)',SCORE)
        self.assertNotIn('previewUniformize(',SCORE)
        self.assertNotIn('applyUniformize(',SCORE)
        self.assertNotIn('review.selectMeasures(',SCORE)
        self.assertNotIn('Ajustar ritmo',SCORE)
        self.assertNotIn('Divisão entre pautas',SCORE)

    def test_musical_data_logic_untouched_by_ui_reflow(self):
        self.assertIn('new CreationScoreStore(activity.getFilesDir()).save(',SCORE)
        self.assertIn('CreationScoreGenerator.generate(',SCORE)
        self.assertIn('CreationStudyCompatibility.check(activity,target',SCORE)
        self.assertIn('new CreationJournal(a.getFilesDir()',CREATION)
        self.assertIn('store.saveRecoverable(',CREATION)
        self.assertIn('a.exportCreationTake(',CREATION)
        self.assertIn('a.exportCreationMidi(',CREATION)
        self.assertIn('a.addCreationScoreToLibrary(id,success->',CREATION)
        self.assertIn('activity.addCreationScoreToLibrary(project.id,ok->',SCORE)
        self.assertNotIn('button("Estudar esta partitura"',SCORE)

if __name__=="__main__":
    unittest.main()
