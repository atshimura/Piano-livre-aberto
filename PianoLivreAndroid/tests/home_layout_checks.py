"""Home refinement measurements on real Android views; synthetic CI data only."""
import json, time
from pathlib import Path
from emulator_ui import adb, dump, screenshot, tap_text, assert_running, wait_for_text

PACKAGE='br.org.pianolivre.opensource'

def layout():
    pid=adb('shell','pidof',PACKAGE).strip()
    lines=adb('logcat','-d','--pid='+pid,'-v','brief').splitlines()
    rows=[line.split('HOME_LAYOUT ',1)[1] for line in lines if 'HOME_LAYOUT ' in line]
    assert rows,'Debug Home measurement missing'
    report=json.loads(rows[-1]);prefix='HOME_ITEM '+str(report['sequence'])+' '
    report['items']=[json.loads(line.split(prefix,1)[1]) for line in lines if prefix in line]
    assert len(report['items'])==report['itemCount'],'Incomplete Home measurements'
    return report

def capture_home_viewports():
    reports=[]
    try:
        for name,size,old_height,old_art,columns in [('phone','720x540',220,205,1),('landscape','1280x720',220,205,2),('tablet','1920x1080',215,590,2)]:
            adb('shell','am','force-stop',PACKAGE)
            adb('shell','wm','size',size);adb('shell','wm','density','240')
            # Each viewport uses only a fresh CI installation with synthetic data.
            adb('shell','pm','clear',PACKAGE)
            adb('shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity')
            wait_for_text('Bem-vindo ao',timeout=20);time.sleep(.8);assert_running()
            report=layout();items=report['items'];density=report['density']
            hero=next(i for i in items if i['kind']=='home-banner')
            art=next(i for i in items if i['kind']=='home-art')
            shortcuts=[i for i in items if i['kind']=='home-shortcut']
            cards=[i for i in items if i['kind']=='home-score']
            assert abs(hero['height']/density-old_height*.7)<=1,('Banner must shrink 30%',name,hero)
            assert art['width']/density>old_art,('Artwork did not grow',name,art)
            assert len(shortcuts)==3 and len(cards)==10,('Shortcut or score lost',name,len(shortcuts),len(cards))
            assert len({c['x'] for c in cards})==columns,('Incorrect score columns',name,cards)
            assert not any(t['text']=='Opções' for i in shortcuts for t in i['labels']),'Removed Options card returned'
            for i in [hero]+shortcuts+cards:
                for label in i['labels']:
                    assert label['textHeight']<=label['height']+1,('Clipped Home text',name,label)
            for label in hero['labels']:
                assert label['y']>=hero['y'] and label['y']+label['height']<=hero['y']+hero['height']+1,('Banner action outside banner',name,label)
            for i in shortcuts:
                assert i['height']/density>=96
                title=next(t for t in i['labels'] if t['text'] in ('Biblioteca Musical','Laboratório Musical','Histórico'))
                assert title['sizeSp']>=16
            for i in cards:
                assert i['labels'][0]['sizeSp']>=17
                assert all(t['sizeSp']>=14 for t in i['labels'] if t['text'] in ('▶  Estudar','Partitura livre'))
            screenshot('piano-home-refined-'+name+'.png')
            if name in ('phone','tablet'):screenshot('piano-home-'+name+'.png')
            tap_text(['Opções e configurações'],exact=True)
            assert any('Calibrar acordes MIDI' in n.attrib.get('text','') for n in dump().iter('node')),'Header gear did not open existing Options'
            adb('shell','input','keyevent','4')
            tap_text(['Biblioteca Musical'],exact=True,scroll=True);time.sleep(.8)
            root=dump();labels=[n.attrib.get('text','')+' '+n.attrib.get('content-desc','') for n in root.iter('node')]
            for essential in ('Buscar título ou arquivo','Todas as categorias','Importar partitura','Coleções','10 de 10 partituras'):
                assert any(essential.casefold() in value.casefold() for value in labels),('Library control missing',name,essential)
            screenshot('piano-home-refined-'+name+'-library.png')
            width,height=map(int,size.split('x'))
            adb('shell','input','swipe','36',str(int(height*.82)),'36',str(int(height*.3)),'350');time.sleep(.5)
            screenshot('piano-home-refined-'+name+'-grid.png')
            reports.append(dict(viewport=size,density=density,betaBase='f91c27853be576a353628e220d4e716d02507bed',baselineBannerDp=old_height,baselineArtDp=old_art,banner=hero,art=art,shortcutCount=len(shortcuts),scoreCount=len(cards),columns=columns,items=items))
    finally:
        adb('shell','am','force-stop',PACKAGE)
        adb('shell','wm','size','reset');adb('shell','wm','density','reset')
        adb('shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity')
        time.sleep(1);assert_running()
    Path('piano-home-refined-report.json').write_text(json.dumps(reports,ensure_ascii=False,indent=2),encoding='utf-8')
    print('HOME_REFINEMENTS_PASS '+json.dumps([dict(viewport=r['viewport'],bannerPx=r['banner']['height'],artPx=r['art']['width'],columns=r['columns'],scores=r['scoreCount']) for r in reports]),flush=True)
