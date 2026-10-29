#!/usr/bin/env python3
"""Source identity in real Android WebView; screenshots require inspection."""
import json,time,re
from pathlib import Path
from collections import Counter
import xml.etree.ElementTree as ET
from emulator_ui import adb,dump,responsive_evidence,assert_running,tap_text,screenshot
from emulator_polyphony import records,latest
from complex_notation_source_test import inspect
PACKAGE='br.org.pianolivre.opensource'
def signature(n):
 return (n['measure'],n['staff'],n['voice'],round(n['beat'],6),n['midi'],round(n['length'],6),n['writtenStep'],n['writtenOctave'],n['writtenAlter'])
def oracle(root):
 rows=[]
 for mi,m in enumerate(root.findall('.//part/measure')):
  pos=0;last=0
  for n in m:
   if n.tag in ('backup','forward'):pos+=int(n.findtext('duration'))*(1 if n.tag=='forward' else -1)
   if n.tag!='note':continue
   grace=n.find('grace') is not None
   if grace:continue
   dur=int(n.findtext('duration'));chord=n.find('chord') is not None
   onset=last if chord else pos
   if not chord:last=pos;pos+=dur
   p=n.find('pitch')
   if p is not None:
    rows.append({'measure':mi+1,'staff':int(n.findtext('staff')),'voice':n.findtext('voice'),
     'beat':mi*4+onset/60,'length':dur/60,'writtenStep':p.findtext('step'),'writtenOctave':int(p.findtext('octave')),'writtenAlter':int(p.findtext('alter','0')),
     'midi':12*(int(p.findtext('octave'))+1)+dict(C=0,D=2,E=4,F=5,G=7,A=9,B=11)[p.findtext('step')]+int(p.findtext('alter','0'))})
 return Counter(signature(n) for n in rows)
def run(name,label):
 root,count,graces=inspect(Path('app/src/debug/assets/scores')/('engraving-'+name+'-06.musicxml'))
 adb('shell','am','force-stop',PACKAGE);adb('logcat','-c')
 adb('shell','am','start','-S','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity','--es','engraving_notation06_selftest',name)
 buttons=[n for n in dump().iter('node') if n.attrib.get('text')=='Espera']
 assert buttons,'Study mode controls are missing on fixture'
 proof=None
 for _ in range(50):
  assert_running();proof=latest('POC_OSMD_READY ')
  if proof:break
  time.sleep(.4)
 assert proof,'Task06 OSMD render missing'
 rows=[n['note'] for n in records('POC_NOTATION06_NOTE ')]
 end=latest('POC_NOTATION06_END ')
 assert end and end['count']==len(rows),'Incomplete audit log'
 Path('piano-notation06-'+name+'-'+label+'-raw.json').write_text(json.dumps({'proof':proof,'notes':rows},indent=2),encoding='utf-8')
 screenshot('piano-notation06-'+name+'-'+label+'-initial.png')
 mapping=proof['midiMapping']
 assert mapping['matched']==count and mapping['unmatchedNative']==mapping['unmatched']==mapping['ambiguous']==0,(name,mapping)
 main=[n for n in rows if not n['grace']]
 assert Counter(signature(n) for n in main)==oracle(root),(name,'OSMD source identities or durations differ',main)
 assert sum(n['grace'] for n in rows)==graces,(name,'Missing ornament heads',rows)
 assert all(n['headBox'] and n['headBox'][2]>0 for n in rows),'SVG head unresolved by audit; inspect raw model and image before classifying engraving'
 assert proof['viewportMaxScrollTop']==0 and proof['height']<=proof['availableHeight']+.5
 ink=proof['sourceInkEnvelope']
 assert ink['viewBoxTop']<=ink['top'] and ink['viewBoxBottom']>=ink['bottom'],('Musical ink outside viewport',ink)
 # Directions are explicit in the authored source: no heuristic voice inference.
 assert all(n['stem']==( -1 if n['voice'] in ('2','5') else 1) for n in main if n['length']<4),(name,'Explicit stem direction',main)
 if name=='accidentals':
  convention=latest('POC_ACCIDENTAL_CONVENTIONS ')
  assert convention=={'requested':1,'applied':1,'unresolved':0},('Parenthesized accidental metadata not resolved',convention)
  caution=[n for n in main if n['beat']==13 and n['staff']==1]
  assert len(caution)==1 and any(m.get('type')=='n' and m.get('cautionary') is True for m in caution[0]['modifiers']), 'Explicit parenthesized natural lost after native layout'
 if name=='beams':
  assert [n['beamNotes'] for n in main if n['voice']=='1'][:4]==[4]*4,'Primary semiquaver group lost'
 if name=='tuplets':
  assert any(n['tuplet'] and n['tuplet']['count']==3 for n in main),'Triplet model missing'
  assert any(n['tuplet'] and n['tuplet']['count']==5 for n in main),'Quintuplet model missing'
 # No note taps: transient task08 label cannot obscure acceptance screenshots.
 responsive_evidence('notation06-'+name+'-'+label)
 if name=='accidentals':
  tap_text(['Contínuo'],exact=True,scroll=True)
  pause=tap_text(['Iniciar'],return_point=True,scroll=True)
  time.sleep(5.0);adb('shell','input','tap',*pause);time.sleep(.3)
  screenshot('piano-notation06-accidentals-double-'+label+'.png')
  adb('shell','input','tap',*pause)
  time.sleep(2.4);adb('shell','input','tap',*pause);time.sleep(.3)
  screenshot('piano-notation06-accidentals-late-'+label+'.png')
 Path('piano-notation06-'+name+'-'+label+'.json').write_text(json.dumps({'proof':proof,'notes':rows,'sourceMain':count,'sourceGrace':graces},indent=2),encoding='utf-8')
 print('NOTATION06_WEBVIEW_PASS '+name+' '+label,flush=True)
def main():
 original=adb('shell','wm','size');previous=re.search(r'Override size: (\d+x\d+)',original)
 try:
  for label,size in [('low',None),('taller','1440x2160'),('tablet','1600x2560')]:
   adb('shell','am','force-stop',PACKAGE)
   if size:adb('shell','wm','size',size)
   for name in ['accidentals','beams','tuplets','grace','adversarial']:
    try:run(name,label)
    except Exception:
     Path('piano-notation06-'+name+'-'+label+'-failure.log').write_text(adb('logcat','-d','-v','brief'),encoding='utf-8')
     screenshot('piano-notation06-'+name+'-'+label+'-failure.png')
     raise
 finally:
  adb('shell','am','force-stop',PACKAGE);adb('shell','wm','size',previous.group(1) if previous else 'reset')
if __name__=='__main__':main()
