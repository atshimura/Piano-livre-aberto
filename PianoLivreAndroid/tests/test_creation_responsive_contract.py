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
        self.assertIn('responsiveActions(left,14,record,list)',CREATION)
        self.assertIn('responsiveActions(body,8,save,preview,export)',CREATION)
        self.assertIn('setShown(revisionActions,review)',CREATION)
        self.assertNotIn('attach(body,export,48,9)',CREATION)
        self.assertIn('attach(body,discard,48,0)',CREATION)
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
        self.assertIn('responsiveActions(body,12,rename,export,infoButton)',details)
        self.assertIn('infoButton.setOnClickListener',details)
        self.assertIn('store.restore(id,System.currentTimeMillis())',details)
        self.assertIn('store.deletePermanently(id)',details)
        self.assertIn('.setPositiveButton("Excluir"',details)
        self.assertIn('attach(body,trash,48,0)',details)
        self.assertIn('attach(body,warning,-2,12)',details)
        self.assertIn('finalizeRecovery(id,System.currentTimeMillis())',details)
        self.assertIn('button("Excluir somente a partitura…"',details)
        self.assertIn('button("Mover composição para a Lixeira"',details)
        self.assertIn('setPositiveButton("Excluir partitura"',details)
        self.assertIn('A gravação MIDI original será preservada.',details)
        self.assertIn('CreationScoreDeletion.delete(a.getFilesDir()',details)

    def test_block_c_preview_commands_and_note_rows(self):
        self.assertIn('private void actions(LinearLayout parent,Button... buttons)',SCORE)
        self.assertEqual(SCORE.count('reflow.accept(0)'),2)
        self.assertIn('((android.view.ViewGroup)button.getParent()).removeView(button)',SCORE)
        self.assertIn('((android.view.ViewGroup)field.getParent()).removeView(field)',SCORE)
        self.assertIn('group.post(()->{if(group.getWidth()>0)reflow.accept(group.getWidth());})',SCORE)
        self.assertIn('button.setSingleLine(false);button.setMaxLines(2)',SCORE)
        self.assertIn('b.setTextSize(13)',SCORE)
        self.assertIn('b.setMinHeight(dp(48))',SCORE)
        self.assertIn('actions(contents,back,save,add)',SCORE)
        self.assertIn('actions(contents,back,save)',SCORE)
        self.assertIn('creation-score-note-row',SCORE)
        self.assertIn('noteRow.addView(select,new LayoutParams(0,dp(52),1))',SCORE)
        self.assertIn('Button chord=button("Acorde"',SCORE)
        self.assertIn('chord.setContentDescription("Marcar acorde que contém a nota "',SCORE)
        self.assertIn('review.selectChord(n.sourceId);render(true)',SCORE)
        self.assertIn('actions(list,button("Selecionar intervalo de compassos"',SCORE)
        for label in ('Desfazer','Refazer','Pauta superior (Sol)',
                      'Pauta inferior (Fá)','Restaurar sugestão','Concluir ajustes'):
            self.assertIn(label,SCORE)

    def test_block_c_settings_and_range_fields(self):
        self.assertIn('private void responsiveFields(LinearLayout parent,String[] labels,View[] fields)',SCORE)
        self.assertIn('int cell=dp(Math.max(168,Math.round(168*scale)))',SCORE)
        self.assertIn('responsiveFields(content,new String[]{"Andamento (BPM)","Compasso","Precisão rítmica"}',SCORE)
        self.assertIn('responsiveFields(panel,new String[]{"Do compasso","Até o compasso (inclusive)"}',SCORE)
        self.assertIn('attach(field,fields[i],48,0)',SCORE)
        self.assertIn('title.setMaxLines(2)',SCORE)
        self.assertIn('review.configure(bpm,beats,beatType,step,chosenRegularization)',SCORE)
        self.assertIn('button("Uniformizar trecho"',SCORE)
        self.assertIn('button("Atribuir mão / voz"',SCORE)
        self.assertIn('previewUniformize(duration,hand,voice)',SCORE)
        self.assertIn('applyUniformize(plan)',SCORE)
        self.assertIn('review.selectMeasures(',SCORE)

    def test_musical_data_logic_untouched_by_ui_reflow(self):
        self.assertIn('new CreationScoreStore(activity.getFilesDir()).save(',SCORE)
        self.assertIn('review.markSaved()',SCORE)
        self.assertIn('new CreationJournal(a.getFilesDir()',CREATION)
        self.assertIn('store.saveRecoverable(',CREATION)
        self.assertIn('a.exportCreationTake(',CREATION)
        self.assertIn('a.exportCreationMidi(',CREATION)
        self.assertIn('a.addCreationScoreToLibrary(id,success->',CREATION)
        self.assertIn('activity.addCreationScoreToLibrary(project.id,ok->',SCORE)
        self.assertNotIn('button("Estudar esta partitura"',SCORE)

if __name__=="__main__":
    unittest.main()
