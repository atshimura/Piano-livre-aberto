/* GPL-3.0-only. OSMD engraving with a stable, discrete presentation camera. */
(function(){
 'use strict';
 var generation=0,current=null,report=null;
 var viewport=document.getElementById('viewport'),host=document.getElementById('score');
 function fail(e){document.getElementById('error').textContent='Não foi possível desenhar a pauta.';if(window.DiscoveryNotation)DiscoveryNotation.error(String(e));}
 function render(xml,tier,ordered,count){
  current=[xml,tier,ordered,count];var ticket=++generation;
  // Build offscreen: keep the previous complete frame while the next engraves.
  var stage=document.createElement('div');stage.style.cssText='position:absolute;left:0;top:0;visibility:hidden';host.appendChild(stage);
  var camera=[{top:95,bass:225,height:360},{top:170,bass:310,height:510},{top:255,bass:405,height:700}][tier];
  var inputHeight=viewport.clientHeight,inputWidth=viewport.clientWidth;
  var scale=Math.min(1.8,Math.max(1,inputHeight-16)/camera.height);
  var width=Math.max(inputWidth/scale,ordered?100+count*60:340);
  stage.style.width=width+'px';
  try{
   var osmd=new opensheetmusicdisplay.OpenSheetMusicDisplay(stage,{backend:'svg',autoResize:false,drawTitle:false,drawComposer:false,drawPartNames:false,drawingParameters:'compacttight',renderSingleHorizontalStaffline:true,disableCursor:true});
   Promise.resolve(osmd.load(new DOMParser().parseFromString(xml,'application/xml'))).then(function(){
    if(ticket!==generation){stage.remove();return;}
    if(viewport.clientHeight!==inputHeight||viewport.clientWidth!==inputWidth){stage.remove();render(xml,tier,ordered,count);return;}
    osmd.render();var svg=stage.querySelector('svg');if(!svg)throw Error('OSMD SVG missing');
    var staves=Array.from(svg.querySelectorAll('g.staffline'));if(staves.length!==2)throw Error('Expected two engraved staves');
    var steps=[];
    staves.forEach(function(staff,index){
     var measure=Array.from(staff.children).find(function(n){return n.classList&&n.classList.contains('vf-measure');});
     var paths=Array.from(measure.children).filter(function(n){return n.tagName.toLowerCase()==='path';}).slice(0,5);
     if(paths.length!==5)throw Error('Five staff rules missing');
     var top=paths[0].getBBox().y,step=Math.abs(paths[1].getBBox().y-top);steps.push(step);
     // Translate whole engraved staff (including ledger lines, glyphs and notes).
     // Never stretch notes, change pitches or infer a physical hand.
     var destination=index?camera.bass:camera.top;
     staff.setAttribute('transform','translate(0,'+(destination-top)+')');
     // OSMD compact measures end at their last event. Extend only the final
     // five engraved rules to the available canvas, leaving every glyph intact.
     var measures=Array.from(staff.children).filter(function(n){return n.classList&&n.classList.contains('vf-measure');});
     var last=measures[measures.length-1];
     Array.from(last.children).filter(function(n){return n.tagName.toLowerCase()==='path';}).slice(0,5).forEach(function(p){var b=p.getBBox();p.setAttribute('d','M '+b.x+' '+b.y+' L '+Math.max(b.x+b.width,width-20)+' '+b.y);});
     measures.forEach(function(m){Array.from(m.children).filter(function(n){return n.tagName.toLowerCase()==='rect'&&n.getBBox().height>=step*3.9;}).forEach(function(n){n.remove();});});
     if(!ordered&&count){
      var heads=Array.from(staff.querySelectorAll('.vf-stavenote'));
      var pitched=heads.filter(function(n){return !!n.querySelector('.vf-notehead');});
      if(pitched.length){var box=pitched[0].querySelector('.vf-notehead').getBBox(),dx=width*.46-(box.x+box.width/2);heads.forEach(function(n){n.setAttribute('transform','translate('+dx+',0)');});}
     }
    });
    // System-spanning brace/barlines depend on the original staff separation.
    // Descobrir has no metre: show two independent complete staves and clefs.
    Array.from(svg.querySelectorAll('.vf-brace,.vf-connector,.vf-timesignature')).forEach(function(n){n.remove();});
    var rawWidth=Math.max(width,parseFloat(svg.getAttribute('width'))||0);
    svg.setAttribute('viewBox','0 0 '+rawWidth+' '+camera.height);
    svg.setAttribute('width',rawWidth*scale);svg.setAttribute('height',camera.height*scale);
    svg.style.width=rawWidth*scale+'px';svg.style.height=camera.height*scale+'px';
    stage.style.cssText='width:'+rawWidth*scale+'px;height:'+camera.height*scale+'px;padding-top:'+Math.max(0,(viewport.clientHeight-camera.height*scale)/2)+'px';
    Array.from(host.children).forEach(function(n){if(n!==stage)n.remove();});viewport.scrollLeft=0;
    // A fixed crop of the same engraved staff keeps both clefs at the left
    // while a long sequence scrolls. No duplicate or substitute glyph drawing.
    var clefs=Array.from(svg.querySelectorAll('.vf-clef'));
    var railWidth=Math.max.apply(null,clefs.map(function(n){var b=n.getBBox();return b.x+b.width+8;}));
    var rail=document.getElementById('rail'),crop=svg.cloneNode(true);
    Array.from(crop.querySelectorAll('.vf-stavenote')).forEach(function(n){n.remove();});
    crop.setAttribute('viewBox','0 0 '+railWidth+' '+camera.height);
    crop.setAttribute('width',railWidth*scale);crop.style.width=railWidth*scale+'px';
    rail.style.top=Math.max(0,(viewport.clientHeight-camera.height*scale)/2)+'px';
    rail.innerHTML='';rail.appendChild(crop);
    var boxes=staves.map(function(s){var m=s.querySelector('.vf-measure');return Array.from(m.children).filter(function(n){return n.tagName.toLowerCase()==='path';}).slice(0,5).map(function(p){var b=p.getBoundingClientRect();return {x:b.x,y:b.y,width:b.width,height:b.height};});});
    report={tier:tier,scale:scale,staffSpace:steps[0]*scale,staves:boxes,notes:count,ordered:ordered,width:rawWidth*scale,height:camera.height*scale,viewportWidth:inputWidth,viewportHeight:inputHeight};
    document.getElementById('error').textContent='';
    if(window.DiscoveryNotation)DiscoveryNotation.rendered(JSON.stringify(report));
   }).catch(fail);
  }catch(e){fail(e);}
 }
 window.Discovery={render:render,inspect:function(){return JSON.stringify(report);},clefs:function(){return JSON.stringify(Array.from(host.querySelectorAll('.vf-clef')).map(function(n){return n.outerHTML;}));}};
 var resize;window.addEventListener('resize',function(){clearTimeout(resize);resize=setTimeout(function(){if(current)render.apply(null,current);},100);});
 if(window.DiscoveryNotation)DiscoveryNotation.ready();
})();
