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
            shortcuts=[i for i in items if i['kind']=='home-shortcut']
            cards=[i for i in items if i['kind']=='home-score']
            filter_item=next(i for i in items if i['kind']=='home-filter')
            search_item=next(i for i in items if i['kind']=='home-library-search')
            toolbar_item=next(i for i in items if i['kind']=='home-library-toolbar')
            actions_item=next(i for i in items if i['kind']=='home-library-actions')
            assert toolbar_item['y']<=search_item['y'] and toolbar_item['y']<=filter_item['y'],('R toolbar alignment',name)
            assert search_item['y']==filter_item['y'],('R search and category not on the same row',name)
            assert search_item['x']+search_item['width']<=filter_item['x'],('R search overlaps category',name)
            assert filter_item['x']+filter_item['width']<=actions_item['x'],('R category overlaps actions',name)
            assert actions_item['x']+actions_item['width']<=toolbar_item['x']+toolbar_item['width']+1,('R actions overflow toolbar',name)
            assert search_item['width']>0 and filter_item['width']>0,('R empty search/category',name)
            if toolbar_item['width']/density>=650:
                assert search_item['width']>filter_item['width'] and search_item['width']>actions_item['width'],('R search must be largest on wide display',name)
            buttons=[label for label in actions_item['labels'] if label['text'] in ('+', '＋  Importar', '▱', '▱  Coleções')]
            assert len(buttons)==2,('R stacked actions missing',name,actions_item)
            assert buttons[0]['y']+buttons[0]['height']<=buttons[1]['y'],('R actions not stacked',name,buttons)
            assert all(button['height']/density>=44 for button in buttons),('R undersized touch targets',name,buttons)

            assert filter_item['labels'] and all(t['sizeSp']>=16 for t in filter_item['labels']),('Library filter is too small',name,filter_item)
            assert abs(hero['height']/density-old_height*.7)<=1,('Banner must shrink 30%',name,hero)
            assert art['width']/density>old_art,('Artwork did not grow',name,art)
            assert len(shortcuts)==3 and len(cards)==10,('Shortcut or score lost',name,len(shortcuts),len(cards))
            # Every viewport must display all three shortcuts side by side,
            # with equal widths and no clipping or horizontal overlap.
            ordered_shortcuts=sorted(shortcuts,key=lambda item:item['x'])
            assert len({item['y'] for item in ordered_shortcuts})==1,('Shortcuts not in one row',name,ordered_shortcuts)
            assert max(item['width'] for item in ordered_shortcuts)-min(item['width'] for item in ordered_shortcuts)<=2,('Shortcuts not equal width',name,ordered_shortcuts)
            assert all(item['width']>0 and item['height']>0 for item in ordered_shortcuts),('Shortcut has empty size',name)
            assert all(left['x']+left['width']<=right['x'] for left,right in zip(ordered_shortcuts,ordered_shortcuts[1:])),('Overlapping shortcuts',name,ordered_shortcuts)

            assert len({c['x'] for c in cards})==columns,('Incorrect score columns',name,cards)
            assert not any(t['text']=='Opções' for i in shortcuts for t in i['labels']),'Removed Options card returned'
            for i in [hero]+shortcuts+cards:
                for label in i['labels']:
                    assert label['textHeight']<=label['height']+1,('Clipped Home text',name,label)
            for label in hero['labels']:
                assert label['y']>=hero['y'] and label['y']+label['height']<=hero['y']+hero['height']+1,('Banner action outside banner',name,label)
                assert label['x']>=copy['x'] and label['x']+label['width']<=copy['x']+copy['width']+1,('Banner action clipped by copy container',name,label)
            for i in shortcuts:
                assert i['height']/density>=96
                title=next(t for t in i['labels'] if t['text'] in ('Biblioteca Musical','Laboratório Musical','Histórico','Biblioteca','Laboratório'))
                minimum=13 if i['width']/density<125 else 16
                assert title['sizeSp']>=minimum,('Shortcut title too small',name,title,minimum)
            for i in cards:
                assert i['labels'][0]['sizeSp']>=17
                assert all(t['sizeSp']>=14 for t in i['labels'] if t['text'] in ('▶  Estudar','Partitura livre'))
            screenshot('piano-home-refined-'+name+'.png')
            image_width,image_height=struct.unpack('>II',Path('piano-home-refined-'+name+'.png').read_bytes()[16:24])
            if name in ('phone','tablet'):screenshot('piano-home-'+name+'.png')
            tap_text(['Opções e configurações'],exact=True)
            assert any('Calibrar acordes MIDI' in n.attrib.get('text','') for n in dump().iter('node')),'Header gear did not open existing Options'
            adb('shell','input','keyevent','4')
            tap_text(['Biblioteca Musical'],exact=True,scroll=True);time.sleep(.8)
            root=dump();labels=[n.attrib.get('text','')+' '+n.attrib.get('content-desc','') for n in root.iter('node')]
            for essential in ('Buscar título ou arquivo','Categoria','Importar partitura','Coleções','10 de 10 partituras'):
                assert any(essential.casefold() in value.casefold() for value in labels),('Library control missing',name,essential)
            screenshot('piano-home-refined-'+name+'-library.png')
            scroll=next(n for n in root.iter('node') if n.attrib.get('scrollable')=='true' and 'Horizontal' not in n.attrib.get('class',''))
            left,top,right,bottom=map(int,re.findall(r'\d+',scroll.attrib['bounds']))
            edge=str(left+max(8,(right-left)//100))
            adb('shell','input','swipe',edge,str(top+int((bottom-top)*.82)),edge,str(top+int((bottom-top)*.3)),'350');time.sleep(.5)
            screenshot('piano-home-refined-'+name+'-grid.png')
            reports.append(dict(viewport=str(image_width)+'x'+str(image_height),physicalSize=size,density=density,betaBase='f91c27853be576a353628e220d4e716d02507bed',baselineBannerDp=old_height,baselineArtDp=old_art,banner=hero,art=art,shortcutCount=len(shortcuts),scoreCount=len(cards),columns=columns,items=items))
    finally:
        adb('shell','am','force-stop',PACKAGE)
        adb('shell','wm','size','reset');adb('shell','wm','density','reset')
        adb('shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity')
        time.sleep(1);assert_running()
    Path('piano-home-refined-report.json').write_text(json.dumps(reports,ensure_ascii=False,indent=2),encoding='utf-8')
    print('HOME_REFINEMENTS_PASS '+json.dumps([dict(viewport=r['viewport'],bannerPx=r['banner']['height'],artPx=r['art']['width'],columns=r['columns'],scores=r['scoreCount']) for r in reports]),flush=True)
