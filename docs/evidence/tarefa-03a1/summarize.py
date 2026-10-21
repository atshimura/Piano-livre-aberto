"""Deriva estatísticas das caixas efetivamente coletadas; não altera SVG."""
import json, sys
from pathlib import Path
raw=json.loads(Path(sys.argv[1]).read_text())
result=[]
def has(r,c):return any(c in (a['cls'] or '').split() for a in r['ancestors'])
def music(r):return any(has(r,c)for c in ['vf-stavenote','vf-text','vf-curve','vf-stavetie','vf-line','vf-ledgers'])
for s in raw['scores']:
    upper,lower=s['staves'];u,l=s['models']
    y1=upper['lines'][0]['box'][1];y2=lower['lines'][0]['box'][1]
    categories=[]
    for cat in ['vf-notehead','vf-stem','vf-ledgers','vf-curve','vf-stavetie','vf-text','vf-line','vf-beam']:
        for staff in s['staves']:
            rows=[r for r in s['records']if r['staff']==staff['id']and has(r,cat)and(cat!='vf-notehead'or r.get('semantic')=='pitched')]
            if rows:
                lo=min(rows,key=lambda r:r['box'][1]);hi=max(rows,key=lambda r:r['box'][3])
                categories.append(dict(category=cat,staff=staff['id'],leaves=len(rows),minY=lo['box'][1],maxY=hi['box'][3],minRecord=lo['i'],maxRecord=hi['i']))
    top=[r for r in s['records']if r['staff']==upper['id']and music(r)]
    bottom=[r for r in s['records']if r['staff']==lower['id']and music(r)]
    pairs=[(b['box'][1]-a['box'][3],a['i'],b['i'])for a in top for b in bottom if a['box'][0]<=b['box'][2]and b['box'][0]<=a['box'][2]]
    constraints=[]
    for j,b in enumerate(u['bottom']):
        sky=min(0,*l['sky'][max(0,j-6):j+7]);x=10*(u['x']+j/u['sampling'])
        constraints.append(dict(j=j,x=x,bottom=b,sky=sky,required=b-sky,measures=[m['id']for m in upper['measures']if m['box'][0]<=x<=m['box'][2]]))
    peak=max(constraints,key=lambda x:x['required'])
    perMeasure={}
    for c in constraints:
        for m in c['measures']:
            if m not in perMeasure or c['required']>perMeasure[m]['required']:perMeasure[m]=c
    result.append(dict(name=s['name'],measures=s['measureCount'],leaves=len(s['records']),staffTopY=[y1,y2],staffGap=y2-y1-40,staffGapSpaces=(y2-y1-40)/10,inkUnion=s['union'],geometricMargins=[s['union'][1],float(s['svg']['height'])-s['union'][3]],fitScaleCalculated=min(1,188/s['svg']['css'][1]),categories=categories,minOverlappingBoxGap=min(pairs),skylinePeak=peak,modelRequiredSeparation=peak['required']+s['rules']['MinSkyBottomDistBetweenStaves'],modelActualSeparation=l['relativeY']-u['relativeY'],constraintsByMeasure=perMeasure))
Path(sys.argv[2]).write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps([{k:s[k]for k in ['name','staffGap','staffGapSpaces','minOverlappingBoxGap','skylinePeak','modelRequiredSeparation','modelActualSeparation']}for s in result],indent=2))
