#!/usr/bin/env python3
"""GPL-3.0-only. Full UI state transitions on a SHORT and a LONG public score.
The emulator cannot replace a physical USB-MIDI piano or physical legibility.
"""
import json,re,time,xml.etree.ElementTree as ET
from pathlib import Path
from emulator_ui import adb,dump,tap_text,screenshot,assert_running,dismiss_keyboard

PACKAGE="br.org.pianolivre.opensource/br.org.pianolivre.MainActivity"

def labels():
    return [n.attrib.get("text","") for n in dump().iter("node") if n.attrib.get("text")]

def expect(label,reason):
    assert any(label in v for v in labels()),reason

def log_count(marker):
    return adb("logcat","-d","-v","brief").count(marker)

def wait_new(marker,before,seconds=25):
    for i in range(int(seconds*2)):
        if log_count(marker)>before:return
        time.sleep(.5)
    raise AssertionError(marker+" did not arrive within "+str(seconds)+" s")

def _bounds(node):
    values=[int(value) for value in re.findall(r"\d+",node.attrib.get("bounds",""))]
    return values if len(values)==4 else None

def library_filter_spinner(root):
    """Find the catalog filter by its row, never by Spinner order.

    The fixed header also contains a profile Spinner. Upgrade fixtures make
    that Spinner display `Upgrade A/B`, so selecting the first Spinner opens
    the profile menu instead of changing the catalog filter.
    """
    search=next((node for node in root.iter("node")
      if "EditText" in node.attrib.get("class","")
      and "Buscar título" in (node.attrib.get("hint","")+node.attrib.get("text",""))),None)
    search_bounds=_bounds(search) if search is not None else None
    if search_bounds is None:
        return None
    search_y=(search_bounds[1]+search_bounds[3])//2
    candidates=[]
    for node in root.iter("node"):
        if node.attrib.get("class")!="android.widget.Spinner":continue
        bounds=_bounds(node)
        if bounds is not None:
            candidates.append((abs((bounds[1]+bounds[3])//2-search_y),node))
    if not candidates:return None
    distance,node=min(candidates,key=lambda pair:pair[0])
    return node if distance<=max(24,(search_bounds[3]-search_bounds[1])//2+8) else None

def library_result_count(text):
    match=re.fullmatch(r"\s*(\d+)\s+de\s+(\d+)\s+partituras\s*",text)
    return (int(match.group(1)),int(match.group(2))) if match else None

def adb_search_text(text):
    return text.replace(" ","%s")

def verify_library_filter_locator():
    assert adb_search_text("Estudo livre 01")=="Estudo%slivre%s01", \
        "LIBRARY_SEARCH_ENCODING_REGRESSION: every space must use Android input %s token"
    # Regression fixture: profile Spinner intentionally comes first in the
    # Android hierarchy; the search-row Spinner is the catalog filter.
    fixture='''<hierarchy>
      <node class="android.widget.Spinner" text="Upgrade B" bounds="[20,10][120,44]"/>
      <node class="android.widget.EditText" hint="Buscar título ou arquivo" bounds="[20,100][300,140]"/>
      <node class="android.widget.Spinner" text="Todas" bounds="[310,102][455,138]"/>
    </hierarchy>'''
    found=library_filter_spinner(ET.fromstring(fixture))
    assert found is not None and found.attrib.get("text")=="Todas", \
        "FILTER_CONTROL_MISSING: locator did not distinguish profile Spinner from catalog filter"
    blank=library_filter_spinner(ET.fromstring(fixture.replace('text="Todas"','text=""')))
    assert blank is not None and blank.attrib.get("text")=="", \
        "FILTER_CONTROL_MISSING: locator must still identify the filter when Android exposes no selected text"
    assert library_result_count("10 de 10 partituras")== (10,10), \
        "LIBRARY_COUNT_PARSER_REGRESSION: positive two-digit counts must not become zero"
    assert library_result_count("0 de 10 partituras")== (0,10), \
        "LIBRARY_COUNT_PARSER_REGRESSION: exact empty counts must remain detectable"

def wait_for_library(expected="Estudo livre 01",seconds=20):
    """Wait for a stable library screen and classify common UI-state failures."""
    catalog=json.loads(Path("app/src/main/assets/catalog.json").read_text(encoding="utf-8"))
    titles=[item["name"] for item in catalog]
    assert expected in titles, "SCORE_MISSING: expected score is absent from the packaged catalog: "+expected
    last=[]
    for _ in range(seconds*2):
        assert_running()
        last=labels()
        if any("← Biblioteca" in value for value in last):
            tap_text(["← Biblioteca"],exact=True)
            time.sleep(.35)
            last=labels()
        if (any("BIBLIOTECA MUSICAL" in value for value in last)
                or any("Buscar título" in value for value in last)
                or any(title in value for title in titles for value in last)):
            break
        # Dismiss only a positively identified profile/filter popup. While the
        # Activity is still rendering, a Back key could exit the app and mask
        # the asynchronous load as a navigation failure.
        profile_popup=bool(last) and all("Upgrade A" in v or "Upgrade B" in v for v in last)
        filter_popup=bool(last) and all(v.strip() in ("Todas","Favoritos") or v.startswith("Coleção:") for v in last)
        if profile_popup or filter_popup:
            adb("shell","input","keyevent","4")
        time.sleep(.35)
    else:
        raise AssertionError("NAVIGATION_FAILED: library header did not appear; labels="+repr(last[:40]))

    # Reopen at the catalog's first rows so title lookup does not inherit a
    # previous screen's ScrollView offset.
    for _ in range(6):
        if any("BIBLIOTECA MUSICAL" in value for value in labels()):break
        root=dump()
        scroll=next((n for n in root.iter("node") if n.attrib.get("scrollable")=="true"
                     and "Horizontal" not in n.attrib.get("class","")),None)
        if scroll is None:break
        bounds=[int(x) for x in re.findall(r"\d+",scroll.attrib.get("bounds",""))]
        if len(bounds)!=4:break
        x=str((bounds[0]+bounds[2])//2)
        adb("shell","input","swipe",x,str(bounds[1]+20),x,str(bounds[3]-20),"350")
        time.sleep(.2)

    # Distinguish a zero result count from positive counts such as "10 de 10".
    # Do not infer that an item is missing just because its card is below the
    # viewport; the short exercise is eighth in this catalog.
    for _ in range(40):
        last=labels()
        if not any("BIBLIOTECA MUSICAL" in value for value in last):
            raise AssertionError("NAVIGATION_FAILED: left the library while waiting; labels="+repr(last[:40]))
        result=next((library_result_count(value) for value in last if library_result_count(value) is not None),None)
        if result is not None:break
        time.sleep(.25)
    if not result:
        raise AssertionError("LIBRARY_NOT_LOADED: library shell appeared without a valid result count; labels="+repr(last[:40]))

    selected=library_filter_spinner(dump())
    if selected is None:
        raise AssertionError("FILTER_CONTROL_MISSING: catalog filter beside the search row is not accessible")
    if selected.attrib.get("text","").strip()!="Todas":
        bounds=_bounds(selected)
        adb("shell","input","tap",str((bounds[0]+bounds[2])//2),str((bounds[1]+bounds[3])//2))
        time.sleep(.2)
        options=labels()
        if "Todas" not in options:
            raise AssertionError("FILTER_CONTROL_MISMATCH: search-row Spinner did not open catalog options; labels="+
                                 repr(options[:40]))
        tap_text(["Todas"],exact=True)

    # Search is the library's supported navigation for scores outside the
    # first viewport. It also proves that positive results are not mislabeled
    # as a filter failure when the card is merely below the fold.
    root=dump()
    search=next((node for node in root.iter("node") if "EditText" in node.attrib.get("class","")
                 and "Buscar título" in (node.attrib.get("hint","")+node.attrib.get("text",""))),None)
    search_bounds=_bounds(search) if search is not None else None
    if search_bounds is None:
        raise AssertionError("LIBRARY_SEARCH_MISSING: loaded library has no title search field")
    query=adb_search_text("Estudo livre 01" if expected=="Estudo livre 01" else "Gymnop")
    current=search.attrib.get("text","")
    if current=="Buscar título ou arquivo":current=""
    adb("shell","input","tap",str((search_bounds[0]+search_bounds[2])//2),
        str((search_bounds[1]+search_bounds[3])//2))
    adb("shell","input","keyevent","123")
    for _ in range(len(current)):
        adb("shell","input","keyevent","67")
    dismiss_keyboard()
    for _ in range(40):
        last=labels()
        if not any("BIBLIOTECA MUSICAL" in value for value in last):
            raise AssertionError("NAVIGATION_FAILED: left the library while clearing search; labels="+repr(last[:40]))
        count=next((library_result_count(value) for value in last if library_result_count(value) is not None),None)
        if count is not None:break
        time.sleep(.25)
    if count is None:
        raise AssertionError("LIBRARY_NOT_LOADED: all-title count did not appear after clearing search; labels="+repr(last[:40]))
    if count[0]<len(titles) or count[1]<len(titles):
        raise AssertionError("SCORE_HIDDEN_BY_FILTER: selecting Todas did not reveal the packaged catalog; count="+
                             repr(count)+" packaged="+str(len(titles)))

    adb("shell","input","tap",str((search_bounds[0]+search_bounds[2])//2),
        str((search_bounds[1]+search_bounds[3])//2))
    adb("shell","input","text",query)
    dismiss_keyboard()
    for _ in range(40):
        last=labels()
        if any(expected in value for value in last):return
        if not any("BIBLIOTECA MUSICAL" in value for value in last):
            raise AssertionError("NAVIGATION_FAILED: left the library while searching; labels="+repr(last[:40]))
        searched=next((library_result_count(value) for value in last if library_result_count(value) is not None),None)
        if searched is not None and searched[0]==0:
            raise AssertionError("SCORE_NOT_VISIBLE: filter is Todas and title search found no match; score="+
                                 repr(expected)+" result="+repr(searched)+" labels="+repr(last[:40]))
        time.sleep(.25)
    raise AssertionError("LIBRARY_NOT_LOADED: title search did not settle for "+repr(expected)+"; labels="+repr(last[:40]))

def library(title):
    wait_for_library(title)

def exercise(title,slug):
    library(title)
    adb("logcat","-c")
    before=log_count("POC_OSMD_READY {")
    tap_text([title],scroll=True)
    wait_new("POC_OSMD_READY {",before)
    expect(title,"Study lost its selected score name")
    for text in ["Espera","Contínuo","Demonstração","Teclado guia"]:
        expect(text,"Study missing essential control: "+text)
    screenshot("piano-entrega-b-"+slug+"-study.png")
    # Hand selection must not filter the entire two-staff visual guide.
    for hand in ["Direita","Esquerda","Ambas"]:
        tap_text([hand],exact=True,scroll=True)
        assert_running()
    tap_text(["Teclado guia"],scroll=True)
    time.sleep(.7)
    opened=labels()
    assert not any("Contínuo"==v for v in opened),"guide should hide top Study controls"
    expect("Ocultar teclado","guide did not open")
    screenshot("piano-entrega-b-"+slug+"-guide.png")
    tap_text(["Ocultar teclado"],scroll=True)
    time.sleep(.5)
    expect("Contínuo","Study controls did not return when closing guide")
    tap_text(["Contínuo"],exact=True,scroll=True)
    base=log_count("play=true mode=1")
    tap_text(["Iniciar"],scroll=True)
    wait_new("play=true mode=1",base)
    time.sleep(.7)
    tap_text(["Pausar"],scroll=True)
    expect("Iniciar","pause did not restore the continuation command")
    base=log_count("play=true mode=1")
    tap_text(["Iniciar"],scroll=True)
    wait_new("play=true mode=1",base)
    tap_text(["Parar"],scroll=True)
    expect("Iniciar","stop left the Study session marked running")
    tap_text(["Início"],scroll=True)
    assert_running()
    # Switch OSMD/temporal/OSMD without changing the MusicXML source.
    tap_text(["Anterior · temporal"],exact=True,scroll=True)
    time.sleep(.7)
    tap_text(["OSMD · nova"],exact=True,scroll=True)
    time.sleep(.7)
    expect(title,"renderer switch lost the current piece")
    # Unsupported demonstration without an output port must refuse playback,
    # rather than silently claiming sound from the visual keyboard.
    tap_text(["Demonstração"],exact=True,scroll=True)
    before=log_count("play=true mode=2")
    tap_text(["Iniciar"],scroll=True)
    time.sleep(.6)
    assert log_count("play=true mode=2")==before,"Demonstration claimed MIDI without an output device"
    # Wait preparation must finish and not require a timeout to play afterward.
    tap_text(["Espera"],exact=True,scroll=True)
    before=log_count("POC_WAIT_COMPLETE")
    tap_text(["Iniciar"],scroll=True)
    wait_new("POC_WAIT_COMPLETE",before,15)
    expect("Esperando","Wait did not arm after gradual preparation")
    tap_text(["Parar"],scroll=True)
    # A warm background/foreground cycle may not create a second playback clock.
    adb("shell","input","keyevent","3")
    time.sleep(.6)
    adb("shell","am","start","-W","-n",PACKAGE)
    time.sleep(1)
    assert_running()
    expect(title,"return from background lost the selected study")
    screenshot("piano-entrega-b-"+slug+"-resumed.png")
    library(title)
    return {"score":title,"modes":["Espera","Contínuo","Demonstração (without output)"],
            "hands":["Direita","Esquerda","Ambas"],
            "guide":"opened and restored","renderer":"OSMD-temporal-OSMD",
            "pause":"stopped and restarted","background":"returned"}

def main():
    verify_library_filter_locator()
    # A force-stop clears transient Android popups/windows while preserving all
    # app preferences and files. This makes this UI journey order-independent
    # after upgrade/profile/library scenarios on the same emulator.
    adb("shell","am","force-stop","br.org.pianolivre.opensource")
    adb("shell","am","start","-S","-W","-n",PACKAGE)
    wait_for_library("Estudo livre 01")
    result=[]
    for title,slug in [("Estudo livre 01","short"),("Gymnopédie nº 1","long")]:
        result.append(exercise(title,slug))
    Path("piano-entrega-b-session.json").write_text(json.dumps({
      "scope":"Android API 29 emulator with synthetic clock; not physical USB-MIDI or visual acceptance",
      "sessions":result},ensure_ascii=False,indent=2),encoding="utf-8")
    print("STUDY_B_ANDROID_SESSION_PASS "+json.dumps(result,ensure_ascii=False),flush=True)

if __name__=="__main__":main()
