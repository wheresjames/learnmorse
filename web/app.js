const MORSE={A:'.-',B:'-...',C:'-.-.',D:'-..',E:'.',F:'..-.',G:'--.',H:'....',I:'..',J:'.---',K:'-.-',L:'.-..',M:'--',N:'-.',O:'---',P:'.--.',Q:'--.-',R:'.-.',S:'...',T:'-',U:'..-',V:'...-',W:'.--',X:'-..-',Y:'-.--',Z:'--..','0':'-----','1':'.----','2':'..---','3':'...--','4':'....-','5':'.....','6':'-....','7':'--...','8':'---..','9':'----.','.':'.-.-.-',',':'--..--','?':'..--..','/':'-..-.','=':'-...-','+':'.-.-.','-':'-....-','@':'.--.-.'};
const DEFAULT_TEXTS=[
  ['default-calling','Calling Practice','CQ CQ DE LEARN MORSE'],
  ['default-alphabet','The Alphabet','a b c d e f g h i j k l m n o p q r s t u v w x y z'],
  ['default-beginnings','Sentence Beginnings','Several minutes before dawn, Mara reached a frozen junction where a brass weather vane creaked above a quiet stone lodge. Outside, a quick brown fox zigzagged past juniper bushes while violet clouds gathered over the western ridge. Under a cracked window, she found a wax-sealed envelope beside a quartz chip, an old key, and a faded map. Then the distant village bells rang twice, and she noticed a penciled note: “The route is hidden in plain sight.” Her final clue was simpler: “Read where each sentence begins.”'],
  ['default-indexed','Indexed Objects','Inside the stationmaster’s desk, Felix found six labeled objects arranged on a velvet tray: brass, crane, ivory, blade, gate, cedar. Beneath them, in the same order, were the numbers 1–2–1–4–1–2. A yellowed card beside a zinc box said, “Use each number to choose one letter from the word directly above it, then read from left to right.” Outside, a quartz clock clicked while jays argued noisily on the platform roof.'],
  ['default-endings','Sentence Endings','At two in the morning, Vera crossed the empty harbor and saw a pale halo around the moon. Beside an abandoned kiosk, she found a jigsaw piece, a zinc token, and one broken ski. The watchman’s notebook mentioned a quick vessel that had vanished beyond the fog. A jagged X was scratched beneath the words, “Look only at where each sentence finishes,” beside a cold brass torch. A penciled arrow on the final page led toward the sealed underground vault.'],
  ['default-fourth','Every Fourth Word','Jonah found a narrow strip of paper tucked inside a cracked compass case beside quartz dust and a tiny zinc buckle. The strip contained one carefully written sentence: “Quick foxes weave cautiously while bright jays advance beyond frozen ridges moving toward distant valleys patiently.” Below it, a second line read, “Take every fourth word, then keep only its first letter.” He checked the count twice before leaving the noisy market square.'],
  ['default-shorter','Choose the Shorter Word','In a disused signal cabin, Priya discovered five word-pairs painted across a wooden panel: lime–quartz, ivy–zebra, gate–jacket, hawk–violin, tin–copper. A brass plaque beneath them said, “In each pair, choose the shorter word and keep its first letter.” Wind rattled the cracked window while a quick fox crossed the snowy yard and a blue jay vanished behind the freight cars.'],
  ['default-center','Center Letters','Quinn opened a velvet pouch and found five small cards marked berry, spike, civic, spear, torch. Each word had exactly five letters, and a note beside a quartz lens said, “Take the letter at the exact center of every card, in order.” Outside the workshop, a noisy jackdaw hopped across a zinc gutter while fog drifted over the frozen canal. Quinn copied the result into his field journal.'],
  ['default-numbered','Numbered Objects','At the old observatory, Mei found five objects tagged with numbers: quartz–2, umbrella–4, ivory–6, compass–8, kite–10. A faded instruction beside a box of glass prisms said, “Arrange the objects from the smallest number to the largest, then take their initials.” Beyond the dome, a fox darted through juniper while a bright meteor flashed above the snowy ridge. Mei checked the sequence twice before touching the locked cabinet.'],
  ['default-caesar','One Letter Back','Victor found a scrap of paper wedged beneath a quartz specimen in the geology lab. Across it, someone had printed the strange six-letter word XJOUFS in thick black ink. A note underneath said, “Move every letter exactly one place backward in the alphabet.” The ventilation fan buzzed, a blue jacket hung from a hook, and frost glazed the window beside a small zinc box.'],
  ['default-word-morse','Word-Length Morse','Zara discovered a coded line in the radio shack while a quick storm shook the glass windows and a fox barked beyond the jetty. The line read: “fox javelin vintage zephyrs / owl map quickly / jackets vintage / red zephyrs javelin”. Beneath it was the rule: “A three-letter word is a dot; a seven-letter word is a dash. Slashes separate Morse letters.” She copied the groups carefully beside a quartz dial and checked every word length twice.'],
  ['default-odd','Odd Positions','In the baggage room of an old express train, Luca found a brass key stamped with the serial VXAYUZLQT. Beside it lay a note saying, “Count from the left and keep only letters in odd-numbered positions.” A cracked mirror reflected a violet jacket, a zinc toolbox, and a faded poster of Quebec. Outside, snow blew past the windows as the midnight train jolted forward.']
].map(([id,name,text])=>({id,name,text}));
const defaults={characterSpeed:40,wordSpeed:20,textSpeed:10,tonePitch:650,fontSize:72,symbolSize:25,foreground:'#f4f7fb',morseColor:'#8fa3bc',background:'#080b10',accent:'#315e8c',cursorColor:'#ffc857',markerOffset:0,repeat:false,volume:55,showSymbols:false};
let state={settings:{...defaults},draft:DEFAULT_TEXTS[0].text,selectedText:DEFAULT_TEXTS[0].id,practiceTexts:DEFAULT_TEXTS.map(item=>({...item}))};
let playing=false,elapsed=0,startAt=0,totalDuration=0,lastIndex=-1,raf=0,audio=null,audioOscillator=null,audioGain=null,audioWarmed=false,audioTimelineStart=0,nextAudioIndex=0,audioScheduler=null,saveTimer=null,toastTimer=null;
let storageMode='memory';
const LOCAL_STORAGE_KEY='learnmorse.state.v1';
const $=id=>document.getElementById(id);
const settingIds=['characterSpeed','wordSpeed','textSpeed','tonePitch','fontSize','symbolSize','foreground','morseColor','background','accent','cursorColor','volume'];
async function load(){
  let loaded=null;
  if(location.protocol==='http:'||location.protocol==='https:'){
    try{
      const response=await fetch('api/state',{headers:{Accept:'application/json'}});
      if(!response.ok||!response.headers.get('content-type')?.includes('application/json'))throw new Error('API unavailable');
      loaded=await response.json();storageMode='server';
    }catch(e){/* A static web server has no persistence API; use this browser. */}
  }
  if(!loaded){
    try{const saved=localStorage.getItem(LOCAL_STORAGE_KEY);loaded=saved?JSON.parse(saved):null;storageMode='local'}
    catch(e){storageMode='memory'}
  }
  if(loaded)state=loaded;
  state.settings={...defaults,...state.settings};if(state.settings.accent?.toLowerCase()==='#5de4c7')state.settings.accent=defaults.accent;state.practiceTexts=Array.isArray(state.practiceTexts)?state.practiceTexts:[];
  normalizeSavedSpeeds();hydrate();renderAll();
  saveStatus.textContent=storageMode==='server'?'Saved':storageMode==='local'?'Saved locally':'Session only';
}
function normalizeSavedSpeeds(){
  // Migrate combinations saved by versions that did not enforce ordering.
  const s=state.settings;
  s.characterSpeed=Math.max(5,Math.min(100,+s.characterSpeed||defaults.characterSpeed));
  s.textSpeed=Math.max(5,Math.min(s.characterSpeed,+s.textSpeed||defaults.textSpeed));
  s.wordSpeed=Math.max(s.textSpeed,Math.min(s.characterSpeed,+s.wordSpeed||defaults.wordSpeed));
}
function hydrate(){practiceText.value=state.draft||'';settingIds.forEach(id=>$(id).value=state.settings[id]);syncSpeedLimits();applyTheme();applyMarkerOffset();updateRepeatButton();applySymbols();updateLabels()}
function persist(){
  clearTimeout(saveTimer);saveStatus.textContent=storageMode==='memory'?'Session only':'Saving…';
  saveTimer=setTimeout(async()=>{
    if(storageMode==='server'){
      try{
        const response=await fetch('api/state',{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(state)});
        if(!response.ok)throw new Error('Save failed');saveStatus.textContent='Saved';return;
      }catch(e){storageMode='local'}
    }
    if(storageMode==='local'){
      try{localStorage.setItem(LOCAL_STORAGE_KEY,JSON.stringify(state));saveStatus.textContent='Saved locally';return}
      catch(e){storageMode='memory'}
    }
    saveStatus.textContent='Session only';
  },300);
}
function applyTheme(){const s=state.settings,r=document.documentElement.style;r.setProperty('--bg',s.background);r.setProperty('--text',s.foreground);r.setProperty('--muted',s.morseColor);r.setProperty('--accent',s.accent);r.setProperty('--cursor',s.cursorColor);r.setProperty('--font-size',s.fontSize+'px');r.setProperty('--symbol-size',s.symbolSize+'px')}
function applyMarkerOffset(){
  const offset=Math.round(+state.settings.markerOffset||0),position=`calc(15% + ${offset}px)`;
  scanLine.style.left=position;scanGlow.style.left=`calc(15% - 50px + ${offset}px)`;
  markerStatus.textContent=`Marker ${offset>0?'+':''}${offset}px · reset`;
  markerStatus.classList.toggle('visible',offset!==0);
}
function applySymbols(){
  const show=!!state.settings.showSymbols;
  viewport.classList.toggle('hide-symbols',!show);
  symbolsButton.classList.toggle('active',show);
  symbolsButton.setAttribute('aria-pressed',String(show));
  symbolsButton.textContent=show?'Hide Morse symbols':'Show Morse symbols';
}
function updateRepeatButton(){repeatButton.classList.toggle('active',!!state.settings.repeat);repeatButton.setAttribute('aria-pressed',String(!!state.settings.repeat));repeatButton.title=state.settings.repeat?'Repeat is on':'Repeat continuously'}
function updateLabels(){characterSpeedValue.value=state.settings.characterSpeed+' WPM';wordSpeedValue.value=state.settings.wordSpeed+' WPM';textSpeedValue.value=state.settings.textSpeed+' WPM';tonePitchValue.value=state.settings.tonePitch+' Hz';fontSizeValue.value=state.settings.fontSize+'px';symbolSizeValue.value=state.settings.symbolSize+'px';volumeValue.value=state.settings.volume+'%'}
function syncSpeedLimits(){
  const character=state.settings.characterSpeed,word=state.settings.wordSpeed,text=state.settings.textSpeed;
  // All tracks retain the same 5–100 scale. Red dots mark the boundaries.
  [characterSpeed,wordSpeed,textSpeed].forEach(slider=>{slider.min=5;slider.max=100});
  placeLimit(characterLowerLimit,word);
  placeLimit(wordLowerLimit,text);
  characterSpeedHint.textContent=`Dots and dashes · minimum ${word} WPM`;
  wordSpeedHint.textContent=`Character cadence · minimum ${text} WPM`;
  textSpeedHint.textContent='Overall pace · raises word and character speed when needed';
}
function placeLimit(marker,value){marker.style.left=`${(value-5)/95*100}%`}
function renderAll(){renderTrack();renderLibrary();charCount.textContent=[...state.draft].length+' characters';stageTitle.textContent=state.draft.trim()?'Listen. Recognize. Repeat.':'Ready when you are'}
function charDuration(ch){
  // Character WPM sets dot/dash length, word WPM sets character cadence,
  // and text WPM independently stretches the gaps between complete words.
  if(ch===' ')return 12/state.settings.textSpeed;
  const code=MORSE[ch.toUpperCase()];
  if(!code)return 12/state.settings.wordSpeed;
  const signalUnits=code.split('').reduce((n,x)=>n+(x==='-'?3:1),0)+(code.length-1);
  const sounded=signalUnits*1.2/state.settings.characterSpeed;
  return Math.max(sounded+3*1.2/state.settings.wordSpeed,12/state.settings.wordSpeed);
}
function renderTrack(){playing=false;cancelAnimationFrame(raf);stopAudioScheduler();silenceAudio();playButton.innerHTML='<span class="play-icon">▶</span>';track.innerHTML='';elapsed=0;lastIndex=-1;const chars=[...state.draft.replace(/\s+/g,' ')];let at=0;chars.forEach((ch,i)=>{const d=charDuration(ch),cell=document.createElement('div');cell.className='morse-cell';cell.dataset.start=at;cell.dataset.duration=d;cell.innerHTML=`<span class="glyph">${escapeHtml(ch)}</span><span class="symbols">${renderMorse(MORSE[ch.toUpperCase()]||'')}</span>`;track.appendChild(cell);sizeCell(cell);at+=d});totalDuration=at;emptyStage.style.display=chars.length?'none':'flex';positionTrack();updateTime()}
function sizeCell(cell){const contentWidth=Math.max(cell.querySelector('.glyph').scrollWidth,cell.querySelector('.symbols').scrollWidth)+32;cell.style.width=Math.max(70,(+cell.dataset.duration)*130,contentWidth)+'px'}
function sizeCells(){[...track.children].forEach(sizeCell)}
function renderMorse(code){return [...code].map(mark=>`<i class="morse-mark morse-${mark==='.'?'dot':'dash'}"></i>`).join('')}
function escapeHtml(s){return s.replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function positionTrack(){const cells=[...track.children];let x=viewport.clientWidth*.15;if(cells.length){let remaining=elapsed;for(const cell of cells){const d=+cell.dataset.duration,w=cell.offsetWidth;if(remaining<=d){x-=w*(remaining/d);break}remaining-=d;x-=w}}track.style.transform=`translate3d(${x}px,-50%,0)`;progress.value=totalDuration?elapsed/totalDuration*1000:0;cells.forEach((c,i)=>{c.classList.toggle('past',+c.dataset.start+(+c.dataset.duration)<=elapsed);c.classList.toggle('active',i===currentIndex())})}
function currentIndex(){return [...track.children].findIndex(c=>elapsed>=+c.dataset.start&&elapsed<+c.dataset.start+(+c.dataset.duration))}
function tick(now){if(!playing)return;elapsed=Math.max(0,Math.min(totalDuration,(now-startAt)/1000));positionTrack();updateTime();if(elapsed>=totalDuration){stopAudioScheduler();if(state.settings.repeat){elapsed=0;lastIndex=-1;positionTrack();updateTime();startAudioTimeline().then(()=>{if(playing)raf=requestAnimationFrame(tick)});return}playing=false;playButton.innerHTML='▶';return}raf=requestAnimationFrame(tick)}
function ensureAudio(){
  if(audio)return;
  audio=new (window.AudioContext||window.webkitAudioContext)();
  audioOscillator=audio.createOscillator();audioGain=audio.createGain();
  audioOscillator.type='sine';audioGain.gain.value=0;
  audioOscillator.connect(audioGain);audioGain.connect(audio.destination);
  audioOscillator.start();
}
async function togglePlay(){if(!state.draft.trim())return showToast('Enter some practice text first');if(elapsed>=totalDuration)elapsed=0;playing=!playing;playButton.innerHTML=playing?'❚❚':'▶';if(playing){await startAudioTimeline();if(playing)raf=requestAnimationFrame(tick)}else{cancelAnimationFrame(raf);stopAudioScheduler();silenceAudio()}}
async function startAudioTimeline(){
  ensureAudio();await audio.resume();
  if(!playing)return;
  stopAudioScheduler();
  // The first start needs extra runway while the browser opens and primes the
  // physical output stream. Once warm, the normal short scheduling lead wins.
  const lead=audioWarmed ? .06 : .2;audioWarmed=true;
  const now=audio.currentTime;
  audioGain.gain.cancelScheduledValues(now);audioGain.gain.setValueAtTime(0,now);
  audioTimelineStart=now+lead-elapsed;
  const outputLatency=audio.outputLatency||audio.baseLatency||0;
  startAt=performance.now()+(lead+outputLatency)*1000-elapsed*1000;
  const cells=[...track.children];
  nextAudioIndex=cells.findIndex(cell=>+cell.dataset.start+(+cell.dataset.duration/2)>elapsed+.001);
  if(nextAudioIndex<0)nextAudioIndex=cells.length;
  scheduleAudio();audioScheduler=setInterval(scheduleAudio,25);
}
function stopAudioScheduler(){if(audioScheduler){clearInterval(audioScheduler);audioScheduler=null}}
function scheduleAudio(){
  if(!audio)return;
  const cells=[...track.children],chars=[...state.draft.replace(/\s+/g,' ')],horizon=audio.currentTime+.18;
  while(nextAudioIndex<cells.length){
    const cell=cells[nextAudioIndex],center=+cell.dataset.start+(+cell.dataset.duration/2),when=audioTimelineStart+center;
    if(when>horizon)break;
    const ch=chars[nextAudioIndex++];
    if(ch&&ch!==' ')soundAt(MORSE[ch.toUpperCase()],when);
  }
}
function silenceAudio(){if(!audioGain)return;const now=audio.currentTime;audioGain.gain.cancelScheduledValues(now);audioGain.gain.setValueAtTime(audioGain.gain.value,now);audioGain.gain.linearRampToValueAtTime(0,now+.006)}
function soundAt(code,when){
  if(!audio||!code)return;
  const dot=1.2/state.settings.characterSpeed,level=state.settings.volume/500;
  audioOscillator.frequency.setValueAtTime(state.settings.tonePitch,when);
  let t=Math.max(when,audio.currentTime+.002);
  for(const mark of code){
    const duration=dot*(mark==='-'?3:1),edge=Math.min(.005,duration*.2);
    audioGain.gain.setValueAtTime(0,t);
    audioGain.gain.linearRampToValueAtTime(level,t+edge);
    audioGain.gain.setValueAtTime(level,t+duration-edge);
    audioGain.gain.linearRampToValueAtTime(0,t+duration);
    t+=duration+dot;
  }
}
function createWav(){
  const chars=[...state.draft.replace(/\s+/g,' ')],rate=44100,durations=chars.map(charDuration),seconds=durations.reduce((a,b)=>a+b,0);
  if(!chars.length)throw new Error('Enter some practice text first');
  if(seconds>1800)throw new Error('Audio is longer than 30 minutes; increase the speed or shorten the text');
  const samples=Math.ceil(seconds*rate),buffer=new ArrayBuffer(44+samples*2),view=new DataView(buffer);
  const ascii=(offset,value)=>[...value].forEach((c,i)=>view.setUint8(offset+i,c.charCodeAt(0)));
  ascii(0,'RIFF');view.setUint32(4,36+samples*2,true);ascii(8,'WAVE');ascii(12,'fmt ');view.setUint32(16,16,true);view.setUint16(20,1,true);view.setUint16(22,1,true);view.setUint32(24,rate,true);view.setUint32(28,rate*2,true);view.setUint16(32,2,true);view.setUint16(34,16,true);ascii(36,'data');view.setUint32(40,samples*2,true);
  let cellStart=0,phase=0;
  chars.forEach((ch,index)=>{const code=MORSE[ch.toUpperCase()]||'',dot=1.2/state.settings.characterSpeed,level=state.settings.volume/100*.32;let t=cellStart;for(const mark of code){const duration=dot*(mark==='-'?3:1),start=Math.floor(t*rate),count=Math.floor(duration*rate),edge=Math.max(1,Math.min(Math.floor(.005*rate),Math.floor(count/4)));for(let i=0;i<count&&start+i<samples;i++){const envelope=Math.min(1,i/edge,(count-i-1)/edge),sample=Math.sin(phase)*32767*level*envelope;view.setInt16(44+(start+i)*2,sample,true);phase+=2*Math.PI*state.settings.tonePitch/rate}t+=duration+dot}cellStart+=durations[index]});
  return new Blob([buffer],{type:'audio/wav'});
}
function downloadAudio(){try{const blob=createWav(),link=document.createElement('a'),base=(textName.value.trim()||'learn-morse').replace(/[^a-z0-9_-]+/gi,'-').replace(/^-|-$/g,'');link.href=URL.createObjectURL(blob);link.download=(base||'learn-morse')+'.wav';document.body.appendChild(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(link.href),1000);showToast('WAV audio downloaded')}catch(error){showToast(error.message)}}
function updateTime(){currentTime.textContent=format(elapsed);remainingTime.textContent='−'+format(Math.max(0,totalDuration-elapsed))}function format(s){return Math.floor(s/60)+':'+String(Math.floor(s%60)).padStart(2,'0')}
function renderLibrary(){library.innerHTML='';libraryCount.textContent=state.practiceTexts.length;if(!state.practiceTexts.length){library.innerHTML='<div class="library-empty">Saved texts will appear here</div>';return}state.practiceTexts.forEach(item=>{const row=document.createElement('div');row.className='library-item'+(state.selectedText===item.id?' selected':'');row.tabIndex=0;row.setAttribute('role','button');row.innerHTML=`<span><b>${escapeHtml(item.name)}</b><small>${escapeHtml(item.text)}</small></span><button class="delete-button" title="Delete" aria-label="Delete ${escapeHtml(item.name)}">×</button>`;const choose=e=>{if(e.target.closest('.delete-button')){e.stopPropagation();state.practiceTexts=state.practiceTexts.filter(x=>x.id!==item.id);if(state.selectedText===item.id)state.selectedText=null;persist();renderLibrary();showToast('Practice text deleted');return}state.selectedText=item.id;state.draft=item.text;practiceText.value=item.text;textName.value=item.name;persist();renderAll()};row.onclick=choose;row.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();choose(e)}};library.appendChild(row)})}
function showToast(message){toast.textContent=message;toast.classList.add('show');clearTimeout(toastTimer);toastTimer=setTimeout(()=>toast.classList.remove('show'),1800)}
practiceText.oninput=()=>{state.draft=practiceText.value;state.selectedText=null;persist();renderAll()};
saveTextButton.onclick=()=>{const name=textName.value.trim(),text=practiceText.value.trim();if(!name||!text)return showToast('Add a name and some practice text');const existing=state.practiceTexts.find(x=>x.name.toLowerCase()===name.toLowerCase());if(existing){existing.text=text;state.selectedText=existing.id}else{const item={id:Date.now().toString(36),name,text};state.practiceTexts.unshift(item);state.selectedText=item.id}state.draft=text;persist();renderAll();showToast(existing?'Practice text updated':'Practice text saved')};
restoreDefaultsButton.onclick=()=>{const ids=new Set(state.practiceTexts.map(item=>item.id));const missing=DEFAULT_TEXTS.filter(item=>!ids.has(item.id));state.practiceTexts.push(...missing.map(item=>({...item})));persist();renderLibrary();showToast(missing.length?`${missing.length} default text${missing.length===1?'':'s'} restored`:'All default texts are present')};
characterSpeed.oninput=wordSpeed.oninput=textSpeed.oninput=e=>{
  let value=+e.target.value;
  if(e.target===characterSpeed)value=Math.max(value,state.settings.wordSpeed);
  if(e.target===wordSpeed){
    value=Math.max(value,state.settings.textSpeed);
    if(value>state.settings.characterSpeed){state.settings.characterSpeed=value;characterSpeed.value=value}
  }
  if(e.target===textSpeed){
    if(value>state.settings.wordSpeed){state.settings.wordSpeed=value;wordSpeed.value=value}
    if(value>state.settings.characterSpeed){state.settings.characterSpeed=value;characterSpeed.value=value}
  }
  e.target.value=value;state.settings[e.target.id]=value;
  syncSpeedLimits();updateLabels();renderTrack();persist();
};
tonePitch.oninput=e=>{state.settings.tonePitch=+e.target.value;updateLabels();persist()};
['fontSize','symbolSize','volume'].forEach(id=>$(id).oninput=e=>{state.settings[id]=+e.target.value;updateLabels();applyTheme();positionTrack();persist()});
['foreground','morseColor','background','accent','cursorColor'].forEach(id=>$(id).oninput=e=>{state.settings[id]=e.target.value;applyTheme();persist()});
settingsButton.onclick=()=>settingsDialog.showModal();resetAppearance.onclick=()=>{Object.assign(state.settings,{fontSize:72,symbolSize:25,foreground:'#f4f7fb',morseColor:'#8fa3bc',background:'#080b10',accent:'#315e8c',cursorColor:'#ffc857',volume:55});hydrate();renderTrack();persist();showToast('Appearance reset')};
scanLine.onpointerdown=e=>{
  e.preventDefault();scanLine.setPointerCapture(e.pointerId);scanLine.classList.add('dragging');
  const startX=e.clientX,startOffset=+state.settings.markerOffset||0;
  scanLine.onpointermove=move=>{
    const base=viewport.clientWidth*.15;
    state.settings.markerOffset=Math.round(Math.max(20-base,Math.min(viewport.clientWidth-20-base,startOffset+move.clientX-startX)));
    applyMarkerOffset();
  };
  scanLine.onpointerup=scanLine.onpointercancel=()=>{
    scanLine.onpointermove=null;scanLine.classList.remove('dragging');persist();showToast('Marker calibration saved');
  };
};
markerStatus.onclick=()=>{state.settings.markerOffset=0;applyMarkerOffset();persist();showToast('Marker position reset')};
symbolsButton.onclick=()=>{state.settings.showSymbols=!state.settings.showSymbols;applySymbols();sizeCells();positionTrack();persist();showToast(state.settings.showSymbols?'Morse symbols shown':'Morse symbols hidden')};
playButton.onclick=togglePlay;restartButton.onclick=()=>{elapsed=0;lastIndex=-1;positionTrack();updateTime();if(playing)startAudioTimeline()};repeatButton.onclick=()=>{state.settings.repeat=!state.settings.repeat;updateRepeatButton();persist();showToast(state.settings.repeat?'Repeat enabled':'Repeat disabled')};progress.oninput=()=>{elapsed=totalDuration*(progress.value/1000);positionTrack();updateTime();if(playing)startAudioTimeline()};window.onresize=positionTrack;document.addEventListener('keydown',e=>{if(e.code==='Space'&&!['INPUT','TEXTAREA','BUTTON'].includes(document.activeElement.tagName)){e.preventDefault();togglePlay()}});
audioExportButton.onclick=downloadAudio;
load();
