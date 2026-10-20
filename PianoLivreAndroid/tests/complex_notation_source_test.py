"""Exact rhythmic integrity and explicit editorial coverage; not a visual test."""
from fractions import Fraction as F
from pathlib import Path
import xml.etree.ElementTree as E
TYPES={'whole':F(4),'half':F(2),'quarter':F(1),'eighth':F(1,2),'16th':F(1,4)}
def inspect(path):
 root=E.parse(path).getroot();graces=0;main=0
 for measure in root.findall('.//part/measure'):
  pos=F(0);ends={};last=F(0)
  for node in measure:
   if node.tag in ('backup','forward'):
    pos+=int(node.findtext('duration'))*F(1,60)*(1 if node.tag=='forward' else -1)
    assert pos>=0,(path,'negative position');continue
   if node.tag!='note':continue
   if node.find('grace') is not None:
    assert node.find('duration') is None;graces+=1;continue
   duration=F(int(node.findtext('duration')),60)
   written=TYPES[node.findtext('type')]
   if node.find('dot') is not None:written*=F(3,2)
   tm=node.find('time-modification')
   if tm is not None:written*=F(int(tm.findtext('normal-notes')),int(tm.findtext('actual-notes')))
   assert duration==written,(path,measure.attrib,node.findtext('type'),duration,written)
   onset=last if node.find('chord') is not None else pos
   if node.find('chord') is None:last=pos;pos+=duration
   key=(node.findtext('staff'),node.findtext('voice'))
   ends[key]=max(ends.get(key,F(0)),onset+duration)
   if node.find('pitch') is not None:main+=1
  assert all(v==4 for v in ends.values()),(path,'incomplete/overfilled voice',ends)
 return root,main,graces
if __name__=='__main__':
 expected={'accidentals':(22,0),'beams':(11,0),'tuplets':(14,0),'grace':(6,4),'adversarial':(12,1)}
 for name,counts in expected.items():
  root,main,grace=inspect(Path('app/src/debug/assets/scores')/('engraving-'+name+'-06.musicxml'))
  assert (main,grace)==counts,(name,main,grace)
  if name=='accidentals':assert {'sharp','flat','natural','double-sharp','flat-flat'}<={n.text for n in root.findall('.//accidental')}
  if name=='tuplets':assert {n.text for n in root.findall('.//actual-notes')}=={'3','5'}
  if name=='grace':assert {n.get('slash') for n in root.findall('.//grace')}=={'yes','no'}
  print('TASK06_SOURCE_PASS',name,'main=',main,'grace=',grace)
