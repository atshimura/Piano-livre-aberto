#!/usr/bin/env python3
"""GPL-3.0-only. Full UI state transitions on a SHORT and a LONG public score.
The emulator cannot replace a physical USB-MIDI piano or physical legibility.
"""
import json,re,time,xml.etree.ElementTree as ET
from pathlib import Path
from emulator_ui import adb,dump,tap_text,screenshot,assert_running,dismiss_keyboard,home_scroll_to_top,focus_library_search_field,reveal_study_controls,assert_study_controls

PACKAGE="br.org.pianolivre.opensource/br.org.pianolivre.MainActivity"

def labels():
    return [value for n in dump().iter("node") for value in (n.attrib.get("text",""),n.attrib.get("content-desc","")) if value]

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

def library_search_field(root):
    """Find the title EditText even when residual text has hidden its hint."""
    fields=[node for node in root.iter("node")
      if node.attrib.get("class")=="android.widget.EditText" and _bounds(node) is not None]
    if len(fields)==1:return fields[0]
    return next((node for node in fields
      if "Buscar título" in (node.attrib.get("hint","")+node.attrib.get("content-desc",""))),None)

def library_filter_spinner(root):
    """Find the catalog filter by its row, never by Spinner order.

    The fixed header also contains a profile Spinner. Upgrade fixtures make
    that Spinner display `Upgrade A/B`, so selecting the first Spinner opens
    the profile menu instead of changing the catalog filter. A nonempty search
    field has no hint, so row matching uses the EditText position.
    """
    search=library_search_field(root)
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
    residual='''<hierarchy>
      <node class="android.widget.Spinner" text="Upgrade B" bounds="[20,10][120,44]"/>
      <node class="android.widget.EditText" text="Gymnopédie" bounds="[20,100][300,140]"/>
      <node class="android.widget.Spinner" text="Todas" bounds="[310,102][455,138]"/>
    </hierarchy>'''
    residual_root=ET.fromstring(residual)
    assert library_search_field(residual_root) is not None, \
        "LIBRARY_SEARCH_MISSING: populated EditText must remain discoverable without its hint"
    residual_filter=library_filter_spinner(residual_root)
    assert residual_filter is not None and residual_filter.attrib.get("text")=="Todas", \
        "FILTER_CONTROL_MISSING: populated title search must still locate its row filter"
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
    inline_home=False
    for _ in range(seconds*2):
        assert_running()
        last=labels()
        inline_home = inline_home or any("Começar pela Ode à Alegria" in value for value in last)
        # The new responsive home presents a library preview and search;
        # activate the real navigation card to load the full catalog list.
        if (any("Começar pela Ode à Alegria" in value for value in last)
                and not any("← Biblioteca" in value for value in last)):
            tap_text(["Biblioteca Musical"],exact=True)
            time.sleep(.35)
            last=labels()
        if any("← Biblioteca" in value for value in last):
            tap_text(["← Biblioteca"],exact=True)
            time.sleep(.35)
            last=labels()
            # Returning from Study lands on the home preview; its catalog
            # counter appears after activating the real Library navigation.
            if (any("Começar pela Ode à Alegria" in value for value in last)
                    and not any("← Biblioteca" in value for value in last)):
                home_scroll_to_top()
                # Tap the clickable card container by its accessible description.
                tap_text(["Biblioteca Musical. Suas partituras para estudar e tocar"],exact=True)
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

    # The return transition can reveal the home preview after the earlier
    # navigation tap; classify the final hierarchy before selecting assertions.
    inline_home = inline_home or any("Começar pela Ode à Alegria" in value for value in last)

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

    # Search text can remain in this Activity after an earlier UI scenario.
    # Clear only the temporary query so the journey starts from the full local
    # catalog; never clear profile, imported files, favorites, or collections.
    root=dump()
    search=library_search_field(root)
    if search is None:
        raise AssertionError("LIBRARY_SEARCH_MISSING: library has no accessible title EditText; labels="+
                             repr(labels()[:40]))
    current=search.attrib.get("text","")
    if current and current!="Buscar título ou arquivo":
        search=focus_library_search_field()
        bounds=_bounds(search)
        adb("shell","input","keyevent","123")
        for _ in range(len(current)):
            adb("shell","input","keyevent","67")
        dismiss_keyboard()
        time.sleep(.35)

    # Refresh the hierarchy after scrolling and clearing any prior search; the
    # preview can be exposed only after those actions finish.
    inline_home = inline_home or any("Começar pela Ode à Alegria" in value for value in labels())

    # A home-embedded library preview is validated through its real search result.
    # Standalone library mode still requires full count and filter checks.
    if not inline_home:
        # Distinguish a zero result count from positive counts such as "10 de 10".
        # Do not infer that an item is missing just because its card is below the
        # viewport; the short exercise is eighth in this catalog.
        for _ in range(40):
            last=labels()
            if any("Começar pela Ode à Alegria" in value for value in last):
                inline_home=True
                break
            if not (any("BIBLIOTECA MUSICAL" in value for value in last)
                    or any("Buscar título" in value for value in last)
                    or any("Começar pela Ode à Alegria" in value for value in last)):
                raise AssertionError("NAVIGATION_FAILED: left the library while waiting; labels="+repr(last[:40]))
            result=next((library_result_count(value) for value in last if library_result_count(value) is not None),None)
            if result is not None:break
            time.sleep(.25)
        if not inline_home:
            if result is None:
                raise AssertionError("LIBRARY_NOT_LOADED: library shell appeared without a valid result count; labels="+repr(last[:40]))
            if result[0]<len(titles):
                raise AssertionError("CATALOG_FILTER_ACTIVE: after clearing residual search the visible result count "+
                                     str(result[0])+" is below packaged catalog size "+str(len(titles))+
                                     "; labels="+repr(last[:40]))

            root=dump()
            selected=library_filter_spinner(root)
            if selected is None:
                raise AssertionError("FILTER_CONTROL_MISSING: catalog filter beside the search row is not accessible; labels="+
                                     repr(last[:40]))
            if selected.attrib.get("text","").strip()!="Todas":
                bounds=_bounds(selected)
                adb("shell","input","tap",str((bounds[0]+bounds[2])//2),str((bounds[1]+bounds[3])//2))
                time.sleep(.2)
                options=labels()
                if "Todas" not in options:
                    raise AssertionError("FILTER_CONTROL_MISMATCH: search-row Spinner did not open catalog options; labels="+
                                         repr(options[:40]))
                tap_text(["Todas"],exact=True)
                # Android may expose the Spinner popup as the whole hierarchy briefly
                # after selection. Wait for the search row to return before typing;
                # dismiss a still-open options popup explicitly, without sending Back
                # while a library/navigation transition is underway.
                root=None
                for _ in range(24):
                    candidate=dump()
                    candidate_search=library_search_field(candidate)
                    if candidate_search is not None:
                        root=candidate
                        break
                    popup_labels=labels()
                    if "Todas" in popup_labels and "Favoritos" in popup_labels:
                        adb("shell","input","keyevent","4")
                    time.sleep(.25)
                if root is None:
                    evidence=dump()
                    Path("piano-library-filter-failure.xml").write_bytes(ET.tostring(evidence,encoding="utf-8"))
                    screenshot("piano-library-filter-failure.png")
                    raise AssertionError("FILTER_CONTROL_MISSING: selected Todas but library search row did not return; labels="+
                                         repr(labels()[:40]))

    # Search is the library's supported navigation for scores outside the
    # first viewport. It also proves that positive results are not mislabeled
    # as a filter failure when the card is merely below the fold.
    search=library_search_field(root)
    if search is None:
        raise AssertionError("LIBRARY_SEARCH_MISSING: loaded library has no title search field")
    query=adb_search_text("Estudo livre 01" if expected=="Estudo livre 01" else "Gymnop")
    search=focus_library_search_field()
    search_bounds=_bounds(search)
    current=search.attrib.get("text","")
    if current=="Buscar título ou arquivo":current=""
    adb("shell","input","keyevent","123")
    for _ in range(len(current)):
        adb("shell","input","keyevent","67")
    dismiss_keyboard()
    if not inline_home:
        for _ in range(40):
            last=labels()
            if not (any("BIBLIOTECA MUSICAL" in value for value in last)
                or any("Buscar título" in value for value in last)
                or any("Começar pela Ode à Alegria" in value for value in last)):
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
        # The responsive home keeps its hero and navigation cards in the same
        # scrollable screen as the full catalog. The home banner therefore remains
        # in the accessibility tree while its embedded library search is active.
        if not (any("BIBLIOTECA MUSICAL" in value for value in last)
                or any("Buscar título" in value for value in last)
                or (inline_home and any("Começar pela Ode à Alegria" in value for value in last))):
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
    # Open through the explicit study action, rather than relying on a
    # noninteractive title label to bubble a touch to its parent card.
    tap_text(["▶  Estudar"],exact=True,scroll=True)
    try:
        wait_new("POC_OSMD_READY {",before)
    except AssertionError as exc:
        current=labels()
        screenshot("piano-"+slug+"-study-open-failure.png")
        if any("BIBLIOTECA MUSICAL" in value for value in current):
            cause="study action returned to or remained in the library"
        elif any("BIBLIOTECA" in value for value in current):
            cause="library navigation did not enter Study"
        else:
            cause="Study opened without OSMD readiness"
        raise AssertionError("STUDY_NAVIGATION_FAILED: "+cause+"; labels="+repr(current[:50])+
                             "; "+str(exc)) from exc
    expect(title,"Study lost its selected score name")
    for text in ["Espera","Contínuo","Demonstração"]:
        expect(text,"Study missing essential control: "+text)
    screenshot("piano-entrega-b-"+slug+"-study.png")
    # Hand selection must not filter the entire two-staff visual guide.
    for hand in ["Direita","Esquerda","Ambas"]:
        tap_text([hand],exact=True,scroll=True)
        assert_running()
    assert_study_controls()
    screenshot("piano-entrega-b-"+slug+"-guide.png") # Historical artifact slot now shows guide-free Study.
    tap_text(["Contínuo"],exact=True,scroll=True)
    tap_text(["Início"],scroll=True)
    rewound=False
    for _ in range(20):
        position_label=next((value for value in labels() if "posição" in value.casefold()),"")
        match=re.search(r"posição\s+(-?\d+(?:\.\d+)?)",position_label,flags=re.I)
        if match and abs(float(match.group(1)))<0.05:
            rewound=True
            break
        time.sleep(.25)
    assert rewound, "STUDY_POSITION_RESIDUAL: rewind did not return to beat 0; labels="+repr(labels()[:40])
    # The short fixture is only 16 beats; slow it through the visible tempo
    # SeekBar so the pause control remains available during emulator UI latency.
    root=dump()
    tempo=next((node for node in root.iter("node")
                if node.attrib.get("class")=="android.widget.SeekBar"),None)
    tempo_bounds=_bounds(tempo) if tempo is not None else None
    assert tempo_bounds is not None, "TEMPO_CONTROL_MISSING: visible tempo slider was not accessible"
    y=str((tempo_bounds[1]+tempo_bounds[3])//2)
    adb("shell","input","swipe",str((tempo_bounds[0]+tempo_bounds[2])//2),y,
        str(tempo_bounds[0]+3),y,"350")
    slow=False
    for _ in range(20):
        if any("25%" in value for value in labels()):
            slow=True
            break
        time.sleep(.2)
    assert slow, "TEMPO_CONTROL_FAILED: UI did not confirm 25% tempo; labels="+repr(labels()[:40])
    root=dump()
    playback_node=next((node for node in root.iter("node")
      if node.attrib.get("class")=="android.widget.Button"
      and "Iniciar" in node.attrib.get("text","")),None)
    playback_bounds=_bounds(playback_node) if playback_node is not None else None
    assert playback_bounds is not None, "PLAYBACK_CONTROL_MISSING: visible start/pause button was not accessible"
    playback_x=str((playback_bounds[0]+playback_bounds[2])//2)
    playback_y=str((playback_bounds[1]+playback_bounds[3])//2)
    base=log_count("play=true mode=1")
    adb("shell","input","tap",playback_x,playback_y)
    print("Tapped playback start",flush=True)
    wait_new("play=true mode=1",base)
    time.sleep(.2)
    adb("shell","input","tap",playback_x,playback_y)
    print("Tapped playback pause",flush=True)
    time.sleep(.2)
    expect("Iniciar","pause did not restore the continuation command")
    base=log_count("play=true mode=1")
    adb("shell","input","tap",playback_x,playback_y)
    print("Tapped playback resume",flush=True)
    wait_new("play=true mode=1",base)
    time.sleep(.2)
    adb("shell","input","tap",playback_x,playback_y)
    print("Tapped playback pause after resume",flush=True)
    time.sleep(.2)
    expect("Iniciar","resumed playback did not pause")
    tap_text(["Parar"],scroll=True)
    expect("Iniciar","stop left the Study session marked running")
    tap_text(["Início"],scroll=True)
    assert_running()
    assert_study_controls()
    expect(title,"automatic renderer lost the current piece")
    # Unsupported demonstration without an output port must refuse playback,
    # rather than silently claiming sound from the visual keyboard.
    demo_before=log_count("play=true mode=2")
    tap_text(["Demonstração"],exact=True,scroll=True)
    tap_text(["Iniciar"],scroll=True)
    warning=[]
    for _ in range(20):
        warning=labels()
        if any("Demonstração requer uma saída MIDI aberta" in value for value in warning):break
        time.sleep(.2)
    assert any("Demonstração requer uma saída MIDI aberta" in value for value in warning), \
        "DEMONSTRATION_OUTPUT_WARNING_MISSING: unavailable MIDI output was not explained after start"
    assert log_count("play=true mode=2")==demo_before, "Demonstration claimed MIDI without an output device"
    tap_text(["OK"],exact=True)
    time.sleep(.3)
    expect("Espera","output warning did not return to the Study controls")
    # Wait preparation must finish and not require a timeout to play afterward.
    tap_text(["Espera"],exact=True,scroll=True)
    before=log_count("POC_WAIT_COMPLETE")
    tap_text(["Iniciar"],scroll=True)
    wait_new("POC_WAIT_COMPLETE",before,15)
    expect("Esperando","Wait did not arm after gradual preparation")
    reveal_study_controls()
    expect(title,"revealing controls lost the Wait score")
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
            "guide":"removed; legacy preferences preserved","renderer":"automatic OSMD with tested fallback",
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
