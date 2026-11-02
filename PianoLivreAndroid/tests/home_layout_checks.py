"""Home refinement measurements on real Android views; synthetic CI data only."""
import json, re, struct, time
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
        for name,size,dpi,old_height,old_art,columns in [('phone','720x540',240,220,205,1),('landscape','1280x720',240,220,205,2),('tablet','1920x1080',240,215,590,2),('compact-tablet','1080x2160',440,220,205,2)]:
            adb('shell','am','force-stop',PACKAGE)
            adb('shell','wm','size',size);adb('shell','wm','density',str(dpi))
            # Each viewport uses only a fresh CI installation with synthetic data.
            adb('shell','pm','clear',PACKAGE)
            adb('shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity','--ez','home_layout_selftest','true')
            wait_for_text('Bem-vindo ao',timeout=20);time.sleep(.8);assert_running()
            report=layout();items=report['items'];density=report['density']
            hero=next(i for i in items if i['kind']=='home-banner')
            copy=next(i for i in items if i['kind']=='home-banner-copy')
            art=next(i for i in items if i['kind']=='home-art')
            shortcuts=[i for i in items if i['kind']=='home-banner-action']
            cards=[i for i in items if i['kind']=='home-score']
            search_item=next(i for i in items if i['kind']=='home-library-search')
            collections_item=next(i for i in items if i['kind']=='home-library-collections')
            import_item=next(i for i in items if i['kind']=='home-library-import')
            toolbar_item=next(i for i in items if i['kind']=='home-library-toolbar')
            ordered=[collections_item,import_item,search_item]
            assert all(item['width']>0 and item['height']>0 for item in ordered),('Missing toolbar action size',name)
            assert len({item['y'] for item in ordered})==1,('Toolbar not on one row',name,ordered)
            assert max(item['width'] for item in ordered)-min(item['width'] for item in ordered)<=2,('Toolbar controls not equal width',name,ordered)
            assert len({item['height'] for item in ordered})==1,('Toolbar controls not equal height',name,ordered)
            assert all(item['height']/density>=44 for item in ordered),('Toolbar touch targets below 44dp',name,ordered)
            assert collections_item['x']+collections_item['width']<=import_item['x'],('Collections overlap Import',name)
            assert import_item['x']+import_item['width']<=search_item['x'],('Import overlaps Search',name)
            assert search_item['x']+search_item['width']<=toolbar_item['x']+toolbar_item['width']+1,('Toolbar overflow',name)
            assert all(item['y']>=toolbar_item['y'] for item in ordered),('Toolbar alignment wrong',name)
            assert collections_item.get('sizeSp',0)>=13,('Collections font shrunk',name,collections_item)
            assert import_item.get('sizeSp',0)>=13,('Import font shrunk',name,import_item)
            assert search_item.get('sizeSp',0)>=14,('Search font shrunk',name,search_item)
            assert not any(i['kind']=='home-filter' for i in items),('Removed Category selector returned',name)
            assert abs(hero['height']/density-old_height*.7)<=1,('Banner must shrink 30%',name,hero)
            assert art['width']/density>old_art,('Artwork did not grow',name,art)
            assert len(shortcuts)==2 and len(cards)==10,('Banner actions or scores missing',name,len(shortcuts),len(cards))
            ordered_shortcuts=sorted(shortcuts,key=lambda item:item['x'])
            assert len({item['y'] for item in ordered_shortcuts})==1,('Banner actions not in one row',name)
            assert max(item['width'] for item in ordered_shortcuts)-min(item['width'] for item in ordered_shortcuts)<=2,('Banner actions not equal width',name)
            assert len({item['height'] for item in ordered_shortcuts})==1,('Banner actions not equal height',name)
            assert all(item['width']>0 and item['height']/density>=44 for item in ordered_shortcuts),('Banner action target undersized',name)
            assert all(left['x']+left['width']<=right['x'] for left,right in zip(ordered_shortcuts,ordered_shortcuts[1:])),('Overlapping banner actions',name)
            assert all(hero['x']<=item['x'] and item['x']+item['width']<=hero['x']+hero['width']+1 for item in shortcuts),('Banner action escaped banner',name)
            assert len({c['x'] for c in cards})==columns,('Incorrect score columns',name,cards)
            for i in [hero]+cards:
                for label in i['labels']:
                    assert label['textHeight']<=label['height']+1,('Clipped Home text',name,label)
            for label in hero['labels']:
                assert label['y']>=hero['y'] and label['y']+label['height']<=hero['y']+hero['height']+1,('Banner text outside banner',name,label)
                assert label['x']>=copy['x'] and label['x']+label['width']<=copy['x']+copy['width']+1,('Banner text clipped by copy container',name,label)
            titles=[t for t in hero['labels'] if t['text'] in ('Biblioteca Musical','Laboratório Musical')]
            assert len(titles)==2,('New banner actions missing',name,titles)
            assert not any(t['text'] in ('Histórico','▣  Como usar','▶  Começar pela Ode à Alegria') for t in hero['labels']),('Old banner actions returned',name)
            for i in cards:
                assert i['labels'][0]['sizeSp']>=17
                assert all(t['sizeSp']>=14 for t in i['labels'] if t['text'] in ('▶  Estudar','Partitura livre'))
            screenshot('piano-home-refined-'+name+'.png')
            image_width,image_height=struct.unpack('>II',Path('piano-home-refined-'+name+'.png').read_bytes()[16:24])
            if name in ('phone','tablet'):screenshot('piano-home-'+name+'.png')
            tap_text(['Opções e configurações'],exact=True)
            settings=[n.attrib.get('text','') for n in dump().iter('node')]
            assert all(option in settings for option in ('Histórico','Como usar','Calibrar acordes MIDI')),'Gear lost history/help or other settings'
            adb('shell','input','keyevent','4')
            tap_text(['Biblioteca Musical'],exact=True,scroll=True);time.sleep(.8)
            root=dump();labels=[n.attrib.get('text','')+' '+n.attrib.get('content-desc','') for n in root.iter('node')]
            for essential in ('Buscar por título ou nome de arquivo','Importar partitura','Coleções','10 de 10 partituras'):
                assert any(essential.casefold() in value.casefold() for value in labels),('Library control missing',name,essential)
            screenshot('piano-home-refined-'+name+'-library.png')
            scroll=next(n for n in root.iter('node') if n.attrib.get('scrollable')=='true' and 'Horizontal' not in n.attrib.get('class',''))
            left,top,right,bottom=map(int,re.findall(r'\d+',scroll.attrib['bounds']))
            edge=str(left+max(8,(right-left)//100))
            adb('shell','input','swipe',edge,str(top+int((bottom-top)*.82)),edge,str(top+int((bottom-top)*.3)),'350');time.sleep(.5)
            screenshot('piano-home-refined-'+name+'-grid.png')
            reports.append(dict(viewport=str(image_width)+'x'+str(image_height),physicalSize=size,density=density,betaBase='f91c27853be576a353628e220d4e716d02507bed',baselineBannerDp=old_height,baselineArtDp=old_art,banner=hero,art=art,bannerActionCount=len(shortcuts),scoreCount=len(cards),columns=columns,items=items))
    finally:
        adb('shell','am','force-stop',PACKAGE)
        adb('shell','wm','size','reset');adb('shell','wm','density','reset')
        adb('shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity')
        time.sleep(1);assert_running()
    Path('piano-home-refined-report.json').write_text(json.dumps(reports,ensure_ascii=False,indent=2),encoding='utf-8')
    print('HOME_REFINEMENTS_PASS '+json.dumps([dict(viewport=r['viewport'],bannerPx=r['banner']['height'],artPx=r['art']['width'],columns=r['columns'],scores=r['scoreCount']) for r in reports]),flush=True)
