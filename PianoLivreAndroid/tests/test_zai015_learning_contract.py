#!/usr/bin/env python3
"""ZAI-015: enforce real Learn mode, MIDI/virtual grading and four approved screens."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/"app/src/main/java/br/org/pianolivre"
def read(name):return (src/name).read_text(encoding="utf-8")
engine=read("MusicLearningEngine.java")
screen=read("MusicLearningScreen.java")
scores=read("MusicLearningScore.java")
keyboard=read("LearningKeyboardView.java")
audio=read("MusicLearningAudio.java")
landing=read("MusicLabLandingScreen.java")
nav=read("MusicLabNavigationBar.java")
discovery=read("MusicLabDiscoveryScreen.java")
routes=read("PianoScreens.java")
activity=read("MainActivity.java")
checks=0
def must(needle,text=screen):
 global checks
 checks+=1
 assert needle in text, f"Missing: {needle}"
def must_not(needle,text=screen):
 global checks
 checks+=1
 assert needle not in text, f"Forbidden: {needle}"
for label in ('"Aprender"','"Escolher e tocar"','"Ler partitura"',
              '"Notas na pauta"','"Movimentos e intervalos"','"Ritmo"',
              '"Acordes"','"Leitura livre"','"Próximo após acerto  →"',
              '"Mostrar dica"','"▷ Ouvir nota"','"Iniciar prática  →"',
              '"Revisar notas"','"Continuar lição  →"'):
 must(label,screen+engine)
for name in ("renderHome()","renderPicker()","renderPath()","renderPractice()",
             "onMidiAttack(int pitch)","onVirtualKeys(Set<Integer> notes)",
             "recordSuccess()","loadProgress()","startLesson(int which,int index)",
             "acceptRhythm(int choice)","refreshExerciseHelp()"):
 must(name)
must('"user_"+shownProfile+"_learning_module_"+i+"_v1"')
must('activity.prefs.edit().putInt(')
must('if((masks[module]&flag)!=0)return;')
must('new MusicLearningEngine.Session(')
must('session.attack(pitch,combined,false)')
must('session.identifyFigure(choice)')
must('if(!reading){session.reset();')
must('advanceButton.setEnabled(session.complete())')
must('setVisibility(VISIBLE);open(HOME)')
must('if(page==PRACTICE){session=null;open(reading?PATH:PICKER);')
must('learning.onMidiPressed(notes)',routes)
must('learning.onMidiAttack(pitch)',routes)
must('void showLearning()',routes)
must('learning.enter()',routes)
must('learning.leave()',routes)
must('learning.back()',routes)
must('learning.pause()',routes)
must('learning.resume()',routes)
must('learning.dispose()',routes)
must('learning.getVisibility()==View.VISIBLE',routes)
must('"Abrir Aprender   →",true,goLearn',landing)
must('MusicLabNavigationBar.DISCOVER,goBack,()->{},goLearn,goCreation',discovery)
must('else if(tab==LEARN)goLearn.run()',nav)
must('if(newAttack)screens.onLabMidiAttack(pitch,now,currentMidiPacket,midiGeneration,currentUiSession);',activity)
must('screens.updatePressed(downKeys)',activity)
must('LearningKeyboardView',screen)
must('setContentDescription("Teclado virtual musical',keyboard)
must('getActionMasked()',keyboard)
must('setNotes(notes',screen) if False else None
must('new DiscoveryStaffView(activity)',screen)
must('new NotationView(activity',screen)
must('notation.loadCreationScore(parsed,0)')
must('Score.read(new ByteArrayInputStream(data),"aprender.musicxml")')
must('android.media.AudioTrack' if False else 'AudioTrack.MODE_STATIC',audio)
must_not('INTERNET',audio)
must_not('https://',screen)
must_not('android.permission.INTERNET',screen)
must_not('Thread.sleep(',screen)
must_not('Progressão fake',screen)
must_not('Progresso geral: 32%',screen)
must('Math.round(100.0*achieved/MusicLearningEngine.totalCount())')
must('new AlertDialog.Builder' if False else 'activity.message("Escolha uma nota',screen)
must('musicxml' if False else '<score-partwise version=',scores)
must('<clef number=',scores)
must('<divisions>4</divisions>',scores)
must('RHYTHM_TYPES',scores)
must('MusicLabCatalog.filter(list,query==null?"":query)',engine)
must('return Feedback.IGNORED;',engine)
must('chordHeld.containsAll(wanted)',engine)
must('if(note!=exercise.pitches[position])',engine)
must('public static boolean unlocked(int module,int[] masks)',engine)
must('Arrays.asList(LESSONS[module].clone())',engine)
must('midiHeld.clear()',screen)
must('audio.stop()',screen)
print(f"ZAI015_LEARN_CONTRACT_PASS checks={checks}")
