package com.learnmorse.app

import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.media.*
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import kotlin.math.*

data class PracticeText(val name: String, val text: String)
val DEFAULT_PRACTICE_TEXTS = listOf(
    PracticeText("Calling Practice", "CQ CQ DE LEARN MORSE"),
    PracticeText("The Alphabet", "a b c d e f g h i j k l m n o p q r s t u v w x y z"),
    PracticeText("Sentence Beginnings", "Several minutes before dawn, Mara reached a frozen junction where a brass weather vane creaked above a quiet stone lodge. Outside, a quick brown fox zigzagged past juniper bushes while violet clouds gathered over the western ridge. Under a cracked window, she found a wax-sealed envelope beside a quartz chip, an old key, and a faded map. Then the distant village bells rang twice, and she noticed a penciled note: “The route is hidden in plain sight.” Her final clue was simpler: “Read where each sentence begins.”"),
    PracticeText("Indexed Objects", "Inside the stationmaster’s desk, Felix found six labeled objects arranged on a velvet tray: brass, crane, ivory, blade, gate, cedar. Beneath them, in the same order, were the numbers 1–2–1–4–1–2. A yellowed card beside a zinc box said, “Use each number to choose one letter from the word directly above it, then read from left to right.” Outside, a quartz clock clicked while jays argued noisily on the platform roof."),
    PracticeText("Sentence Endings", "At two in the morning, Vera crossed the empty harbor and saw a pale halo around the moon. Beside an abandoned kiosk, she found a jigsaw piece, a zinc token, and one broken ski. The watchman’s notebook mentioned a quick vessel that had vanished beyond the fog. A jagged X was scratched beneath the words, “Look only at where each sentence finishes,” beside a cold brass torch. A penciled arrow on the final page led toward the sealed underground vault."),
    PracticeText("Every Fourth Word", "Jonah found a narrow strip of paper tucked inside a cracked compass case beside quartz dust and a tiny zinc buckle. The strip contained one carefully written sentence: “Quick foxes weave cautiously while bright jays advance beyond frozen ridges moving toward distant valleys patiently.” Below it, a second line read, “Take every fourth word, then keep only its first letter.” He checked the count twice before leaving the noisy market square."),
    PracticeText("Choose the Shorter Word", "In a disused signal cabin, Priya discovered five word-pairs painted across a wooden panel: lime–quartz, ivy–zebra, gate–jacket, hawk–violin, tin–copper. A brass plaque beneath them said, “In each pair, choose the shorter word and keep its first letter.” Wind rattled the cracked window while a quick fox crossed the snowy yard and a blue jay vanished behind the freight cars."),
    PracticeText("Center Letters", "Quinn opened a velvet pouch and found five small cards marked berry, spike, civic, spear, torch. Each word had exactly five letters, and a note beside a quartz lens said, “Take the letter at the exact center of every card, in order.” Outside the workshop, a noisy jackdaw hopped across a zinc gutter while fog drifted over the frozen canal. Quinn copied the result into his field journal."),
    PracticeText("Numbered Objects", "At the old observatory, Mei found five objects tagged with numbers: quartz–2, umbrella–4, ivory–6, compass–8, kite–10. A faded instruction beside a box of glass prisms said, “Arrange the objects from the smallest number to the largest, then take their initials.” Beyond the dome, a fox darted through juniper while a bright meteor flashed above the snowy ridge. Mei checked the sequence twice before touching the locked cabinet."),
    PracticeText("One Letter Back", "Victor found a scrap of paper wedged beneath a quartz specimen in the geology lab. Across it, someone had printed the strange six-letter word XJOUFS in thick black ink. A note underneath said, “Move every letter exactly one place backward in the alphabet.” The ventilation fan buzzed, a blue jacket hung from a hook, and frost glazed the window beside a small zinc box."),
    PracticeText("Word-Length Morse", "Zara discovered a coded line in the radio shack while a quick storm shook the glass windows and a fox barked beyond the jetty. The line read: “fox javelin vintage zephyrs / owl map quickly / jackets vintage / red zephyrs javelin”. Beneath it was the rule: “A three-letter word is a dot; a seven-letter word is a dash. Slashes separate Morse letters.” She copied the groups carefully beside a quartz dial and checked every word length twice."),
    PracticeText("Odd Positions", "In the baggage room of an old express train, Luca found a brass key stamped with the serial VXAYUZLQT. Beside it lay a note saying, “Count from the left and keep only letters in odd-numbered positions.” A cracked mirror reflected a violet jacket, a zinc toolbox, and a faded poster of Quebec. Outside, snow blew past the windows as the midnight train jolted forward.")
)
data class Settings(
    var charWpm: Int = 40, var wordWpm: Int = 20, var textWpm: Int = 10,
    var pitch: Int = 650, var fontSize: Int = 72, var symbolSize: Int = 25,
    var volume: Int = 55, var repeat: Boolean = false, var showSymbols: Boolean = false, var markerOffset: Int = 0, var foreground: Int = Color.rgb(244,247,251),
    var morseColor: Int = Color.rgb(143,163,188), var background: Int = Color.rgb(8,11,16),
    var accent: Int = Color.rgb(49,94,140), var marker: Int = Color.rgb(255,200,87)
)

class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private var settings = Settings()
    private val saved = mutableListOf<PracticeText>()
    private lateinit var practice: EditText
    private lateinit var name: EditText
    private lateinit var trainer: MorseView
    private lateinit var play: Button
    private lateinit var repeatButton: Button
    private lateinit var symbolsButton: Button
    private var pendingAudioText=""
    private var pendingAudioFormat="m4a"
    private var loading = true

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        prefs = getSharedPreferences("learnmorse", MODE_PRIVATE)
        loadState()
        window.statusBarColor = settings.background
        window.navigationBarColor = settings.background
        buildUi()
        loading = false
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;setBackgroundColor(settings.background)
            setOnApplyWindowInsetsListener { view,insets ->
                if(Build.VERSION.SDK_INT>=30){
                    val safe=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    view.setPadding(safe.left,safe.top,safe.right,safe.bottom)
                }else{
                    @Suppress("DEPRECATION")
                    view.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom)
                }
                insets
            }
        }
        val landscape=resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val portraitBody=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(28))}
        val leftPane=if(landscape)LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(8),dp(8),dp(18))}else portraitBody
        val rightPane=if(landscape)LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(8),dp(12),dp(16),dp(24))}else portraitBody

        val header = LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL; setPadding(dp(4),dp(8),dp(4),dp(16)) }
        header.addView(TextView(this).apply { text="·−"; textSize=20f; gravity=Gravity.CENTER; setTextColor(Color.WHITE); background=round(settings.accent,14f); layoutParams=LinearLayout.LayoutParams(dp(48),dp(48)) })
        header.addView(LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setPadding(dp(12),0,0,0)
            addView(label("Learn Morse",20f,settings.foreground,true)); addView(label("RHYTHM BECOMES LANGUAGE",10f,settings.morseColor,false))
        }, LinearLayout.LayoutParams(0,WRAP,1f))
        header.addView(Button(this).apply { text="⚙"; textSize=20f; setTextColor(settings.foreground); background=round(Color.rgb(36,42,50),12f); setOnClickListener { showSettings() } }, LinearLayout.LayoutParams(dp(52),dp(48)))
        leftPane.addView(header)

        leftPane.addView(sectionHeader("LIVE PRACTICE", ""))
        trainer = MorseView(this, settings).apply { onMarkerOffsetChanged={offset->settings.markerOffset=offset;saveState()};text=prefs.getString("draft","CQ CQ DE LEARN MORSE") ?: "" }
        leftPane.addView(trainer, LinearLayout.LayoutParams(MATCH,dp(if(landscape)190 else 180)))
        // Five buttons have to sit side by side on a 360dp-wide phone, so the row is kept compact.
        val transport = LinearLayout(this).apply { gravity=Gravity.CENTER; setPadding(0,dp(8),0,dp(18)) }
        transport.addView(Button(this).apply { text="◀";textSize=20f;setTextColor(settings.foreground);background=transportButtonBackground(false);elevation=0f;stateListAnimator=null;contentDescription="Restart from beginning";setOnClickListener { trainer.restart() } }, LinearLayout.LayoutParams(dp(54),dp(52)))
        play = Button(this).apply { text="▶"; textSize=20f; setTextColor(Color.WHITE); background=round(settings.accent,30f); setOnClickListener { val active=trainer.toggle(); text=if(active) "Ⅱ" else "▶" } }
        transport.addView(play, LinearLayout.LayoutParams(dp(68),dp(60)).apply { marginStart=dp(10); marginEnd=dp(10) })
        repeatButton=Button(this).apply{text="↻";textSize=20f;elevation=0f;stateListAnimator=null;contentDescription="Repeat continuously";setOnClickListener{settings.repeat=!settings.repeat;trainer.repeat=settings.repeat;updateRepeatButton();saveState()}}
        transport.addView(repeatButton,LinearLayout.LayoutParams(dp(54),dp(52)));updateRepeatButton()
        transport.addView(Button(this).apply{text="⇩";textSize=20f;setTextColor(settings.foreground);background=transportButtonBackground(false);elevation=0f;stateListAnimator=null;contentDescription="Save audio file";setOnClickListener{chooseAudioExport()}},LinearLayout.LayoutParams(dp(54),dp(52)).apply{marginStart=dp(6)})
        symbolsButton=Button(this).apply{text="·−";textSize=18f;setPadding(0,0,0,0);elevation=0f;stateListAnimator=null;setOnClickListener{settings.showSymbols=!settings.showSymbols;trainer.settings=settings;updateSymbolsButton();saveState()}}
        transport.addView(symbolsButton,LinearLayout.LayoutParams(dp(54),dp(52)).apply{marginStart=dp(6)});updateSymbolsButton()
        leftPane.addView(transport)

        rightPane.addView(card().apply {
            addView(sectionHeader("PRACTICE TEXT", ""))
            practice=EditText(this@MainActivity).apply {
                setText(trainer.text); setTextColor(settings.foreground); setHintTextColor(settings.morseColor); hint="Type a message, callsign, or phrase…"; minLines=3; maxLines=6; gravity=Gravity.TOP; setPadding(dp(14),dp(12),dp(14),dp(12)); background=outline(settings.background)
                addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){};override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){if(!loading){trainer.text=s.toString();saveState()}};override fun afterTextChanged(s:Editable?) {}})
            }
            addView(practice, LinearLayout.LayoutParams(MATCH,WRAP).apply { topMargin=dp(12) })
            val saveRow=LinearLayout(this@MainActivity).apply { gravity=Gravity.CENTER_VERTICAL; setPadding(0,dp(10),0,0) }
            name=EditText(this@MainActivity).apply { hint="Name this practice text";setTextColor(settings.foreground);setHintTextColor(settings.morseColor);isSingleLine=true;background=outline(settings.background);setPadding(dp(12),0,dp(12),0) }
            saveRow.addView(name,LinearLayout.LayoutParams(0,dp(48),1f))
            saveRow.addView(Button(this@MainActivity).apply { text="SAVE";setTextColor(Color.WHITE);background=round(settings.accent,10f);setOnClickListener{savePractice()} },LinearLayout.LayoutParams(dp(92),dp(48)).apply{marginStart=dp(8)})
            addView(saveRow)
        }, marginParams())

        rightPane.addView(card().apply {
            addView(sectionHeader("YOUR LIBRARY", ""))
            addView(Button(this@MainActivity).apply{text="OPEN PRACTICE LIBRARY   ›";textSize=14f;setTextColor(Color.WHITE);background=round(settings.accent,10f);setOnClickListener{startActivityForResult(Intent(this@MainActivity,LibraryActivity::class.java),LIBRARY_REQUEST)}},LinearLayout.LayoutParams(MATCH,dp(52)).apply{topMargin=dp(10)})
        },marginParams())

        rightPane.addView(card().apply {
            addView(sectionHeader("TIMING & TONE", ""))
            addView(speedControl("Character speed","Dots and dashes",settings.charWpm,settings.wordWpm){ value -> settings.charWpm=max(value,settings.wordWpm); trainer.settings=settings; saveState(); rebuildControls() })
            addView(speedControl("Word speed","Complete-word cadence",settings.wordWpm,settings.textWpm){ value -> settings.wordWpm=max(value,settings.textWpm); if(settings.wordWpm>settings.charWpm) settings.charWpm=settings.wordWpm; trainer.settings=settings; saveState(); rebuildControls() })
            addView(speedControl("Text speed","Overall reading pace",settings.textWpm,null){ value -> settings.textWpm=value; if(value>settings.wordWpm)settings.wordWpm=value;if(value>settings.charWpm)settings.charWpm=value;trainer.settings=settings;saveState();rebuildControls() })
            addView(valueControl("Tone pitch","Comfortable range: 300–1200 Hz",settings.pitch,300,1200," Hz"){settings.pitch=it;trainer.settings=settings;saveState()})
        }.also { it.tag="controls" },marginParams())

        if(landscape){
            val columns=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
            columns.addView(ScrollView(this).apply{isFillViewport=true;addView(leftPane)},LinearLayout.LayoutParams(0,MATCH,0.92f))
            columns.addView(ScrollView(this).apply{isFillViewport=true;addView(rightPane)},LinearLayout.LayoutParams(0,MATCH,1.08f))
            root.addView(columns,LinearLayout.LayoutParams(MATCH,0,1f))
        }else{
            root.addView(ScrollView(this).apply{isFillViewport=true;addView(portraitBody)},LinearLayout.LayoutParams(MATCH,0,1f))
        }
        setContentView(root);root.requestApplyInsets()
    }

    private fun rebuildControls() {
        window.decorView.post { if (!isFinishing) {
            val controls=findViewByTag(window.decorView,"controls") as? LinearLayout ?: return@post
            fun update(index:Int,value:Int,limit:Int?){
                val box=controls.getChildAt(index) as? LinearLayout ?: return
                val row=box.getChildAt(0) as? LinearLayout ?: return
                (row.getChildAt(1) as? TextView)?.text="$value WPM"
                (box.getChildAt(1) as? LimitSeekBar)?.apply{progress=value-5;this.limit=limit}
            }
            update(1,settings.charWpm,settings.wordWpm);update(2,settings.wordWpm,settings.textWpm);update(3,settings.textWpm,null)
        }}
    }

    private fun updateSymbolsButton(){symbolsButton.isSelected=settings.showSymbols;symbolsButton.setTextColor(if(settings.showSymbols)Color.WHITE else settings.foreground);symbolsButton.background=transportButtonBackground(settings.showSymbols);symbolsButton.contentDescription=if(settings.showSymbols)"Hide Morse symbols" else "Show Morse symbols"}
    private fun updateRepeatButton(){repeatButton.isSelected=settings.repeat;repeatButton.setTextColor(if(settings.repeat)Color.WHITE else settings.foreground);repeatButton.background=transportButtonBackground(settings.repeat);repeatButton.contentDescription=if(settings.repeat)"Repeat enabled" else "Repeat disabled"}

    private fun speedControl(title:String, hint:String, value:Int, limit:Int?, changed:(Int)->Unit)=valueControl(title,hint,value,5,100," WPM",limit,changed)
    private fun valueControl(title:String,hint:String,value:Int,min:Int,max:Int,suffix:String,limit:Int?=null,changed:(Int)->Unit):View {
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,dp(12),0,dp(6))}
        val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        row.addView(label(title,14f,settings.foreground,true),LinearLayout.LayoutParams(0,WRAP,1f))
        val output=label("$value$suffix",13f,settings.accent,true);row.addView(output);box.addView(row)
        box.addView(LimitSeekBar(this).apply{this.max=max-min;progress=value-min;this.limitMin=min;this.limit=limit;this.panelColor=Color.rgb(36,42,50);progressTintList=android.content.res.ColorStateList.valueOf(settings.accent);thumbTintList=android.content.res.ColorStateList.valueOf(settings.accent);setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){};override fun onProgressChanged(s:SeekBar?,p:Int,user:Boolean){if(user){val v=p+min;output.text="$v$suffix";changed(v)}}})})
        box.addView(label(hint,11f,settings.morseColor,false));return box
    }

    private fun showSettings(){
        val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(24),dp(16),dp(24),dp(8));setBackgroundColor(Color.rgb(36,42,50));addView(sectionHeader("APPEARANCE","Make it yours"));addView(valueControl("Character size","Scrolling text size",settings.fontSize,36,120," sp"){settings.fontSize=it;trainer.settings=settings;saveState()});addView(valueControl("Morse size","Symbol size",settings.symbolSize,14,48," sp"){settings.symbolSize=it;trainer.settings=settings;saveState()});addView(valueControl("Tone volume","Playback loudness",settings.volume,0,100,"%"){settings.volume=it;trainer.settings=settings;saveState()})}
        AlertDialog.Builder(this).setView(panel).setNeutralButton("RESET"){_,_->settings.fontSize=72;settings.symbolSize=25;settings.volume=55;trainer.settings=settings;saveState()}.setPositiveButton("DONE",null).show()
    }

    private fun savePractice(){val n=name.text.toString().trim();val t=practice.text.toString().trim();if(n.isEmpty()||t.isEmpty()){Toast.makeText(this,"Add a name and some practice text",Toast.LENGTH_SHORT).show();return};val old=saved.indexOfFirst{it.name.equals(n,true)};if(old>=0)saved[old]=PracticeText(n,t)else saved.add(0,PracticeText(n,t));saveState();Toast.makeText(this,"Practice text saved",Toast.LENGTH_SHORT).show();hideKeyboard()}
    private fun loadState(){try{val o=JSONObject(prefs.getString("settings","{}")!!);settings.charWpm=o.optInt("charWpm",40);settings.wordWpm=o.optInt("wordWpm",20);settings.textWpm=o.optInt("textWpm",10);settings.pitch=o.optInt("pitch",650);settings.fontSize=o.optInt("fontSize",72);settings.symbolSize=o.optInt("symbolSize",25);settings.volume=o.optInt("volume",55);settings.repeat=o.optBoolean("repeat",false);settings.showSymbols=o.optBoolean("showSymbols",false);settings.markerOffset=o.optInt("markerOffset",0);if(!prefs.contains("texts")){saved.addAll(DEFAULT_PRACTICE_TEXTS)}else{val a=JSONArray(prefs.getString("texts","[]"));for(i in 0 until a.length()){val p=a.getJSONObject(i);saved+=PracticeText(p.getString("name"),p.getString("text"))}};settings.textWpm=settings.textWpm.coerceIn(5,100);settings.wordWpm=settings.wordWpm.coerceIn(settings.textWpm,100);settings.charWpm=settings.charWpm.coerceIn(settings.wordWpm,100)}catch(_:Exception){if(!prefs.contains("texts"))saved.addAll(DEFAULT_PRACTICE_TEXTS)}}
    private fun saveState(){if(loading)return;val o=JSONObject().put("charWpm",settings.charWpm).put("wordWpm",settings.wordWpm).put("textWpm",settings.textWpm).put("pitch",settings.pitch).put("fontSize",settings.fontSize).put("symbolSize",settings.symbolSize).put("volume",settings.volume).put("repeat",settings.repeat).put("showSymbols",settings.showSymbols).put("markerOffset",settings.markerOffset);val a=JSONArray();saved.forEach{a.put(JSONObject().put("name",it.name).put("text",it.text))};prefs.edit().putString("settings",o.toString()).putString("texts",a.toString()).putString("draft",if(::practice.isInitialized)practice.text.toString() else "").apply()}
    private fun chooseAudioExport(){pendingAudioText=practice.text.toString().replace(Regex("\\s+")," ");if(pendingAudioText.isBlank()){Toast.makeText(this,"Enter some practice text first",Toast.LENGTH_SHORT).show();return};if(MorseWav.duration(pendingAudioText,settings)>1800){Toast.makeText(this,"Audio is longer than 30 minutes; increase the speed or shorten the text",Toast.LENGTH_LONG).show();return};AlertDialog.Builder(this).setTitle("Save audio as").setItems(arrayOf("M4A / AAC — smaller file","WAV — lossless")){_,which->requestAudioExport(if(which==0)"m4a" else "wav")}.show()}
    private fun requestAudioExport(format:String){pendingAudioFormat=format;val base=name.text.toString().trim().ifEmpty{"learn-morse"}.replace(Regex("[^A-Za-z0-9_-]+"),"-").trim('-');startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type=if(format=="m4a")"audio/mp4" else "audio/wav";putExtra(Intent.EXTRA_TITLE,"$base.$format")},AUDIO_EXPORT_REQUEST)}
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==LIBRARY_REQUEST){saved.clear();try{val a=JSONArray(prefs.getString("texts","[]"));for(i in 0 until a.length()){val p=a.getJSONObject(i);saved+=PracticeText(p.getString("name"),p.getString("text"))}}catch(_:Exception){};if(resultCode==RESULT_OK){practice.setText(data?.getStringExtra("text")?:"");name.setText(data?.getStringExtra("name")?:"");play.text="▶"}}else if(requestCode==AUDIO_EXPORT_REQUEST&&resultCode==RESULT_OK){val uri=data?.data?:return;val text=pendingAudioText;val format=pendingAudioFormat;val snapshot=settings.copy();Thread{try{if(format=="m4a"){contentResolver.openFileDescriptor(uri,"rw")?.use{MorseM4a.write(it.fileDescriptor,text,snapshot)}?:error("Unable to open destination")}else{contentResolver.openOutputStream(uri)?.use{MorseWav.write(it,text,snapshot)}?:error("Unable to open destination")};runOnUiThread{Toast.makeText(this,"${format.uppercase()} audio saved",Toast.LENGTH_SHORT).show()}}catch(e:Exception){runOnUiThread{Toast.makeText(this,"Could not save audio: ${e.message}",Toast.LENGTH_LONG).show()}}}.start()}}
    private fun card()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(18),dp(18),dp(18));background=round(Color.rgb(36,42,50),16f)}
    private fun sectionHeader(kicker:String,title:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;addView(label(kicker,10f,settings.accent,true));if(title.isNotEmpty())addView(label(title,19f,settings.foreground,true))}
    private fun label(value:String,size:Float,color:Int,bold:Boolean)=TextView(this).apply{text=value;textSize=size;setTextColor(color);if(bold)setTypeface(typeface,Typeface.BOLD)}
    private fun marginParams()=LinearLayout.LayoutParams(MATCH,WRAP).apply{bottomMargin=dp(14)}
    private fun round(color:Int,radius:Float)=GradientDrawable().apply{setColor(color);cornerRadius=dp(radius.toInt()).toFloat()}
    private fun outline(color:Int)=GradientDrawable().apply{setColor(color);cornerRadius=dp(10).toFloat();setStroke(dp(1),Color.rgb(38,49,65))}
    private fun transportButtonBackground(active:Boolean)=GradientDrawable().apply{setColor(if(active)settings.accent else Color.rgb(36,42,50));cornerRadius=dp(26).toFloat();if(!active)setStroke(dp(1),Color.rgb(55,65,78))}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).roundToInt()
    private fun hideKeyboard(){(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(practice.windowToken,0)}
    private fun findViewByTag(v:View,tag:Any):View?{if(v.tag==tag)return v;if(v is ViewGroup)for(i in 0 until v.childCount)findViewByTag(v.getChildAt(i),tag)?.let{return it};return null}
    companion object { const val MATCH=-1;const val WRAP=-2;const val LIBRARY_REQUEST=42;const val AUDIO_EXPORT_REQUEST=43 }
}

/** SeekBar that paints a red dot on the track marking the lowest value this slider may take. */
class LimitSeekBar(context:Context):SeekBar(context){
    private val dot=Paint(Paint.ANTI_ALIAS_FLAG)
    var limitMin=0
    var panelColor=Color.rgb(36,42,50)
    var limit:Int?=null;set(value){field=value;invalidate()}
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val mark=limit?:return
        val d=resources.displayMetrics.density
        val thumbWidth=thumb?.intrinsicWidth?:0
        val span=width-paddingLeft-paddingRight-thumbWidth
        val fraction=((mark-limitMin).toFloat()/kotlin.math.max(1,this.max)).coerceIn(0f,1f)
        val cx=paddingLeft+thumbWidth/2f+span*fraction
        val cy=height/2f
        val outer=kotlin.math.min(8f*d,cy)
        dot.style=Paint.Style.FILL
        dot.color=Color.argb(90,255,82,103);canvas.drawCircle(cx,cy,outer,dot)
        dot.color=panelColor;canvas.drawCircle(cx,cy,outer*0.81f,dot)
        dot.color=Color.rgb(255,82,103);canvas.drawCircle(cx,cy,outer*0.56f,dot)
    }
}

class LibraryActivity:Activity(){
    private lateinit var prefs:SharedPreferences;private val items=mutableListOf<PracticeText>();private lateinit var list:ListView;private lateinit var adapter:BaseAdapter
    private val bg=Color.rgb(8,11,16);private val panel=Color.rgb(36,42,50);private val textColor=Color.rgb(244,247,251);private val muted=Color.rgb(143,163,188);private val accent=Color.rgb(49,94,140)
    override fun onCreate(state:Bundle?){super.onCreate(state);prefs=getSharedPreferences("learnmorse",MODE_PRIVATE);load();buildUi()}
    private fun buildUi(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(16),dp(10),dp(16),dp(16));setOnApplyWindowInsetsListener{v,i->if(Build.VERSION.SDK_INT>=30){val s=i.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout());v.setPadding(dp(16)+s.left,dp(10)+s.top,dp(16)+s.right,dp(16)+s.bottom)};i}}
        val head=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;addView(Button(this@LibraryActivity).apply{text="‹";textSize=28f;setTextColor(textColor);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(52),dp(52)));addView(TextView(this@LibraryActivity).apply{text="Practice Library";textSize=21f;setTextColor(textColor);setTypeface(typeface,Typeface.BOLD)},LinearLayout.LayoutParams(0,WRAP,1f))};root.addView(head)
        root.addView(TextView(this).apply{text="Tap to practice  •  Swipe left to delete";textSize=11f;setTextColor(muted);setPadding(dp(8),0,0,dp(10))})
        list=ListView(this).apply{divider=android.graphics.drawable.ColorDrawable(Color.rgb(38,49,65));dividerHeight=dp(1);setBackgroundColor(panel)}
        adapter=object:BaseAdapter(){override fun getCount()=items.size;override fun getItem(p:Int)=items[p];override fun getItemId(p:Int)=p.toLong();override fun getView(p:Int,old:View?,parent:ViewGroup?):View{val row=(old as? LinearLayout)?:LinearLayout(this@LibraryActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(14));addView(TextView(this@LibraryActivity).apply{tag="name";textSize=15f;setTextColor(textColor);setTypeface(typeface,Typeface.BOLD)});addView(TextView(this@LibraryActivity).apply{tag="preview";textSize=11f;setTextColor(muted);maxLines=2;setPadding(0,dp(4),0,0)})};(row.findViewWithTag<TextView>("name")).text=items[p].name;(row.findViewWithTag<TextView>("preview")).text=items[p].text;return row}}
        list.adapter=adapter;list.setOnItemClickListener{_,_,p,_->setResult(RESULT_OK,Intent().putExtra("name",items[p].name).putExtra("text",items[p].text));finish()};installSwipeDelete();root.addView(list,LinearLayout.LayoutParams(MATCH,0,1f))
        root.addView(Button(this).apply{text="RESTORE DEFAULT TEXTS";setTextColor(textColor);background=outline();setOnClickListener{restore()}},LinearLayout.LayoutParams(MATCH,dp(50)).apply{topMargin=dp(10)});setContentView(root);root.requestApplyInsets()
    }
    private fun installSwipeDelete(){var downX=0f;var downY=0f;var position=-1;list.setOnTouchListener{_,e->when(e.actionMasked){MotionEvent.ACTION_DOWN->{downX=e.x;downY=e.y;position=list.pointToPosition(e.x.toInt(),e.y.toInt())};MotionEvent.ACTION_UP->{val dx=e.x-downX;if(position>=0&&dx < -dp(72).toFloat()&&abs(dx)>abs(e.y-downY)){confirmDelete(position);return@setOnTouchListener true}}};false}}
    private fun confirmDelete(position:Int){if(position !in items.indices)return;val item=items[position];AlertDialog.Builder(this).setTitle("Delete ${item.name}?").setMessage("This practice text will be removed from your library.").setNegativeButton("CANCEL",null).setPositiveButton("DELETE"){_,_->items.removeAt(position);save();adapter.notifyDataSetChanged()}.show()}
    private fun restore(){val names=items.map{it.name}.toSet();val missing=DEFAULT_PRACTICE_TEXTS.filter{it.name !in names};items.addAll(missing);save();adapter.notifyDataSetChanged();Toast.makeText(this,if(missing.isEmpty())"All default texts are present" else "${missing.size} restored",Toast.LENGTH_SHORT).show()}
    private fun load(){try{val a=JSONArray(prefs.getString("texts","[]"));for(i in 0 until a.length()){val o=a.getJSONObject(i);items+=PracticeText(o.getString("name"),o.getString("text"))}}catch(_:Exception){}}
    private fun save(){val a=JSONArray();items.forEach{a.put(JSONObject().put("name",it.name).put("text",it.text))};prefs.edit().putString("texts",a.toString()).apply()}
    private fun outline()=GradientDrawable().apply{setColor(Color.TRANSPARENT);cornerRadius=dp(10).toFloat();setStroke(dp(1),Color.rgb(38,49,65))};private fun dp(v:Int)=(v*resources.displayMetrics.density).roundToInt()
    companion object{const val MATCH=-1;const val WRAP=-2}
}

class MorseView(context:Context, initial:Settings):View(context) {
    var repeat=initial.repeat
    var onMarkerOffsetChanged:((Int)->Unit)?=null
    var settings=initial;set(value){field=value;repeat=value.repeat;audio.pitch=value.pitch;audio.volume=value.volume;rebuild();invalidate()}
    var text="";set(value){field=value.replace(Regex("\\s+")," ");stop();elapsed=0.0;lastSound=-1;rebuild()}
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG);private val cells=mutableListOf<Cell>();private val audio=MorseAudio();private var playing=false;private var elapsed=0.0;private var started=0L;private var lastSound=-1;private var total=0.0;private var draggingMarker=false;private var dragStartX=0f;private var dragStartOffset=0;private var markerMoved=false;private var lastMarkerTap=0L
    private val scroller=OverScroller(context);private var velocityTracker:VelocityTracker?=null;private var scrubbing=false;private var scrubMoved=false;private var scrubLastX=0f;private var scrubStartX=0f;private var scrubStartY=0f
    private var overscroll=0.0 // Visible pixels the track is pulled past an end; 0 while inside the practice.
    private var overpull=0.0 // The undamped finger travel behind that pull.
    private val touchSlop=ViewConfiguration.get(context).scaledTouchSlop;private val minFling=ViewConfiguration.get(context).scaledMinimumFlingVelocity;private val maxFling=ViewConfiguration.get(context).scaledMaximumFlingVelocity
    data class Cell(val char:Char,val code:String,val start:Double,val duration:Double)
    init{setBackgroundColor(initial.background);scroller.setFriction(.01f)}
    private fun rebuild(){releaseOverscroll();cells.clear();var at=0.0;text.forEach{ch->val code=MORSE[ch.uppercaseChar()]?:"";val duration=duration(ch,code);cells+=Cell(ch,code,at,duration);at+=duration};total=at;invalidate()}
    private fun duration(ch:Char,code:String):Double{if(ch==' ')return 12.0/settings.textWpm;if(code.isEmpty())return 12.0/settings.wordWpm;val units=code.fold(0){sum,mark->sum+if(mark=='-')3 else 1}+max(0,code.length-1);return max(units*1.2/settings.charWpm+3*1.2/settings.wordWpm,12.0/settings.wordWpm)}
    fun toggle():Boolean{releaseOverscroll();playing=!playing;if(playing){if(elapsed>=total)elapsed=0.0;started=SystemClock.elapsedRealtimeNanos()-(elapsed*1e9).toLong();postInvalidateOnAnimation()}else audio.stop();return playing}
    fun restart(){releaseOverscroll();elapsed=0.0;lastSound=-1;if(playing)started=SystemClock.elapsedRealtimeNanos();invalidate()}
    fun stop(){releaseOverscroll();playing=false;audio.stop();invalidate()}
    override fun onDetachedFromWindow(){super.onDetachedFromWindow();audio.close()}
    override fun onDraw(c:Canvas){
        super.onDraw(c);c.drawColor(settings.background)
        if(!playing&&scroller.computeScrollOffset()){
            val position=scroller.currX.toDouble();val span=distanceAt(total)
            elapsed=timeAt(position).coerceIn(0.0,total)
            overscroll=if(position<0)position else if(position>span)position-span else 0.0
            lastSound=-1;postInvalidateOnAnimation()
        }else if(!playing&&!scrubbing&&scroller.isFinished)overscroll=0.0
        val baseMarker=width*.15f;val marker=(baseMarker+dp(settings.markerOffset.toFloat())).coerceIn(dp(20f),width-dp(20f))
        paint.shader=LinearGradient(marker-dp(52f),0f,marker+dp(52f),0f,intArrayOf(Color.TRANSPARENT,withAlpha(settings.marker,42),Color.TRANSPARENT),null,Shader.TileMode.CLAMP)
        c.drawRect(marker-dp(52f),0f,marker+dp(52f),height.toFloat(),paint);paint.shader=null
        paint.color=settings.marker;paint.strokeWidth=dp(2f);c.drawLine(marker,dp(14f),marker,height-dp(14f),paint)
        val diamond=Path().apply{moveTo(marker,dp(6f));lineTo(marker+dp(7f),dp(14f));lineTo(marker,dp(22f));lineTo(marker-dp(7f),dp(14f));close()};c.drawPath(diamond,paint)
        if(settings.markerOffset!=0){paint.textAlign=Paint.Align.LEFT;paint.typeface=Typeface.DEFAULT_BOLD;paint.textSize=sp(9f);paint.color=withAlpha(settings.marker,220);c.drawText("CAL ${if(settings.markerOffset>0)"+" else ""}${settings.markerOffset} dp",dp(8f),height-dp(8f),paint)}
        var x=baseMarker.toDouble();var rem=elapsed
        for(cell in cells){val w=cellWidth(cell);if(rem<=cell.duration){x-=w*(rem/cell.duration);break};rem-=cell.duration;x-=w}
        x-=overscroll
        paint.textAlign=Paint.Align.CENTER;paint.typeface=Typeface.MONOSPACE;paint.textSize=sp(settings.fontSize.toFloat())
        // With the code hidden the character is the only thing on the track, so centre it on the baseline.
        val centerY=if(settings.showSymbols)height*.43f else height/2f-(paint.fontMetrics.ascent+paint.fontMetrics.descent)/2f
        cells.forEach{cell->val w=cellWidth(cell);val cx=(x+w/2).toFloat();if(cx>-w&&cx<width+w){paint.color=if(cell.start+cell.duration<elapsed)withAlpha(settings.foreground,70)else settings.foreground;paint.textSize=sp(settings.fontSize.toFloat());paint.typeface=Typeface.MONOSPACE;c.drawText(cell.char.toString(),cx,centerY,paint);if(settings.showSymbols)drawCode(c,cell.code,cx,centerY+dp(42f),cell.start+cell.duration<elapsed)};x+=w}
        if(playing){elapsed=((SystemClock.elapsedRealtimeNanos()-started)/1e9).coerceAtMost(total);val idx=cells.indexOfLast{elapsed+AUDIO_LOOKAHEAD>=it.start+it.duration/2};if(idx!=lastSound&&idx>=0){lastSound=idx;if(cells[idx].code.isNotEmpty()){val center=cells[idx].start+cells[idx].duration/2;audio.playAt(cells[idx].code,settings.charWpm,started+(center*1e9).toLong())}};if(elapsed>=total){if(repeat){elapsed=0.0;lastSound=-1;started=SystemClock.elapsedRealtimeNanos();postInvalidateOnAnimation()}else playing=false}else postInvalidateOnAnimation()}
    }
    override fun onTouchEvent(event:MotionEvent):Boolean{
        val marker=width*.15f+dp(settings.markerOffset.toFloat())
        when(event.actionMasked){
            MotionEvent.ACTION_DOWN->{
                if(abs(event.x-marker)<=dp(30f)){draggingMarker=true;markerMoved=false;dragStartX=event.x;dragStartOffset=settings.markerOffset;parent.requestDisallowInterceptTouchEvent(true);return true}
                if(!playing&&cells.isNotEmpty()){beginScrub(event);return true}
            }
            MotionEvent.ACTION_MOVE->{
                if(draggingMarker){if(abs(event.x-dragStartX)>dp(2f))markerMoved=true;val minOffset=(dp(20f)-width*.15f)/resources.displayMetrics.density;val maxOffset=(width-dp(20f)-width*.15f)/resources.displayMetrics.density;settings.markerOffset=(dragStartOffset+(event.x-dragStartX)/resources.displayMetrics.density).roundToInt().coerceIn(minOffset.roundToInt(),maxOffset.roundToInt());invalidate();return true}
                if(scrubbing){
                    velocityTracker?.addMovement(event)
                    // Claim the gesture only once it is clearly horizontal, so a vertical drag still scrolls the page.
                    if(!scrubMoved){
                        val dx=abs(event.x-scrubStartX);val dy=abs(event.y-scrubStartY)
                        if(dx>touchSlop&&dx>dy){scrubMoved=true;scrubLastX=event.x;parent.requestDisallowInterceptTouchEvent(true)}
                        return true
                    }
                    val step=event.x-scrubLastX;scrubLastX=event.x;scrubBy(step.toDouble());invalidate();return true
                }
            }
            MotionEvent.ACTION_UP->{
                if(draggingMarker){draggingMarker=false;parent.requestDisallowInterceptTouchEvent(false);val now=SystemClock.uptimeMillis();if(!markerMoved&&now-lastMarkerTap<350){settings.markerOffset=0;Toast.makeText(context,"Marker calibration reset",Toast.LENGTH_SHORT).show()};lastMarkerTap=now;onMarkerOffsetChanged?.invoke(settings.markerOffset);performClick();invalidate();return true}
                if(scrubbing){endScrub(event,true);return true}
            }
            MotionEvent.ACTION_CANCEL->{
                if(draggingMarker){draggingMarker=false;parent.requestDisallowInterceptTouchEvent(false);onMarkerOffsetChanged?.invoke(settings.markerOffset);return true}
                if(scrubbing){endScrub(event,false);return true}
            }
        };return super.onTouchEvent(event)
    }
    override fun performClick():Boolean{super.performClick();return true}
    private fun beginScrub(event:MotionEvent){
        scroller.abortAnimation();overpull=undamp(overscroll);scrubbing=true;scrubMoved=false
        scrubStartX=event.x;scrubStartY=event.y;scrubLastX=event.x
        velocityTracker=VelocityTracker.obtain().apply{addMovement(event)}
    }
    private fun endScrub(event:MotionEvent,allowFling:Boolean){
        val tracker=velocityTracker
        if(allowFling&&scrubMoved&&tracker!=null){
            tracker.addMovement(event);tracker.computeCurrentVelocity(1000,maxFling.toFloat())
            // The track moves opposite the finger, so a leftward flick runs the practice forward.
            val velocity=-tracker.xVelocity;val span=distanceAt(total)
            // The fling may overshoot an end by overfling(); OverScroller springs it back on its own.
            if(overscroll==0.0&&abs(velocity)>minFling&&span>0)scroller.fling(distanceAt(elapsed).roundToInt(),0,velocity.roundToInt(),0,0,span.roundToInt(),0,0,overfling(),0)
        }
        tracker?.recycle();velocityTracker=null
        if(!scrubMoved)performClick()
        scrubbing=false;scrubMoved=false;overpull=0.0
        if(scroller.isFinished&&overscroll!=0.0)scroller.springBack((distanceAt(elapsed)+overscroll).roundToInt(),0,0,distanceAt(total).roundToInt(),0,0)
        parent.requestDisallowInterceptTouchEvent(false);invalidate()
    }
    private fun scrubBy(dx:Double){
        val span=distanceAt(total);val target=distanceAt(elapsed)+overpull-dx
        when{
            target<0->{elapsed=0.0;overpull=target}
            target>span->{elapsed=total;overpull=target-span}
            else->{elapsed=timeAt(target);overpull=0.0}
        }
        overscroll=damp(overpull);lastSound=-1
    }
    private fun releaseOverscroll(){scroller.abortAnimation();overscroll=0.0;overpull=0.0}
    private fun overfling()=(width*.22f).roundToInt()
    /** How far a rubber band stretches: resistance grows with the pull and never passes [rubberLimit]. */
    private fun rubberLimit()=max(dp(80f).toDouble(),width*.5)
    private fun damp(pull:Double):Double{val limit=rubberLimit();return sign(pull)*(1-1/(abs(pull)*RUBBER_TENSION/limit+1))*limit}
    private fun undamp(stretch:Double):Double{val limit=rubberLimit();val s=abs(stretch).coerceAtMost(limit*.98);return sign(stretch)*limit/RUBBER_TENSION*(1/(1-s/limit)-1)}
    /** Pixels from the start of the track to [time]; cells are not uniformly wide, so this walks them. */
    private fun distanceAt(time:Double):Double{
        var remaining=time;var distance=0.0
        for(cell in cells){val w=cellWidth(cell);if(remaining<=cell.duration)return distance+w*(remaining/cell.duration);remaining-=cell.duration;distance+=w}
        return distance
    }
    /** Inverse of [distanceAt]. */
    private fun timeAt(distance:Double):Double{
        if(distance<=0)return 0.0
        var remaining=distance;var time=0.0
        for(cell in cells){val w=cellWidth(cell);if(remaining<=w)return time+cell.duration*(remaining/w);remaining-=w;time+=cell.duration}
        return total
    }
    private fun cellWidth(cell:Cell):Double{paint.typeface=Typeface.MONOSPACE;paint.textSize=sp(settings.fontSize.toFloat());val glyphWidth=paint.measureText(cell.char.toString());val h=sp(settings.symbolSize*.22f);val codeWidth=if(!settings.showSymbols)0f else cell.code.sumOf{if(it=='-')(h*3.1f).toDouble() else h.toDouble()}.toFloat()+max(0,cell.code.length-1)*h*1.3f;val contentWidth=max(glyphWidth,codeWidth)+dp(32f);return max(max(dp(70f).toDouble(),cell.duration*dp(130f)),contentWidth.toDouble())}
    private fun drawCode(c:Canvas,code:String,cx:Float,y:Float,past:Boolean){val h=sp(settings.symbolSize*.22f);val dot=h;val dash=h*3.1f;val gap=h*1.3f;val totalW=code.sumOf{if(it=='-')dash.toDouble() else dot.toDouble()}.toFloat()+max(0,code.length-1)*gap;var x=cx-totalW/2;paint.color=if(past)withAlpha(settings.morseColor,70)else settings.morseColor;code.forEach{val w=if(it=='-')dash else dot;c.drawRoundRect(x,y-h/2,x+w,y+h/2,h/2,h/2,paint);x+=w+gap}}
    private fun dp(v:Float)=v*resources.displayMetrics.density;private fun sp(v:Float)=v*resources.displayMetrics.scaledDensity;private fun withAlpha(color:Int,a:Int)=Color.argb(a,Color.red(color),Color.green(color),Color.blue(color))
    companion object { const val AUDIO_LOOKAHEAD=.14;const val RUBBER_TENSION=.55;val MORSE=mapOf('A' to ".-",'B' to "-...",'C' to "-.-.",'D' to "-..",'E' to ".",'F' to "..-.",'G' to "--.",'H' to "....",'I' to "..",'J' to ".---",'K' to "-.-",'L' to ".-..",'M' to "--",'N' to "-.",'O' to "---",'P' to ".--.",'Q' to "--.-",'R' to ".-.",'S' to "...",'T' to "-",'U' to "..-",'V' to "...-",'W' to ".--",'X' to "-..-",'Y' to "-.--",'Z' to "--..",'0' to "-----",'1' to ".----",'2' to "..---",'3' to "...--",'4' to "....-",'5' to ".....",'6' to "-....",'7' to "--...",'8' to "---..",'9' to "----.") }
}

class MorseAudio {
    var pitch=650;var volume=55;private val executor=Executors.newSingleThreadExecutor();@Volatile private var generation=0
    fun playAt(code:String,wpm:Int,targetNanos:Long){val token=generation;val hz=pitch;val level=volume/100f;executor.execute{if(token!=generation)return@execute;val rate=44100;val dot=1.2/wpm;val samples=code.sumOf{((if(it=='-')dot*3 else dot)+dot)*rate}.toInt();val pcm=ShortArray(max(1,samples));var p=0;code.forEach{mark->val n=((if(mark=='-')dot*3 else dot)*rate).toInt();val edge=max(1,min((.005*rate).toInt(),n/4));for(i in 0 until n){val env=min(1f,min(i.toFloat()/edge,(n-i-1).toFloat()/edge));pcm[p++]=(sin(2*PI*hz*i/rate)*Short.MAX_VALUE*.32*level*env).toInt().toShort()};p+=(dot*rate).toInt().coerceAtMost(pcm.size-p)};if(token!=generation)return@execute;val track=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(pcm.size*2).setTransferMode(AudioTrack.MODE_STATIC).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY).build();track.write(pcm,0,pcm.size);waitUntil(targetNanos-OUTPUT_LATENCY_COMPENSATION_NS,token);if(token!=generation){track.release();return@execute};track.play();Thread.sleep((pcm.size*1000L/rate)+15);track.release()}}
    private fun waitUntil(target:Long,token:Int){while(token==generation){val remaining=target-SystemClock.elapsedRealtimeNanos();if(remaining<=0)return;if(remaining>2_000_000)Thread.sleep((remaining-1_000_000)/1_000_000)else Thread.yield()}}
    fun stop(){generation++};fun close(){stop();executor.shutdownNow()}
    companion object{const val OUTPUT_LATENCY_COMPENSATION_NS=25_000_000L}
}

object MorseWav{
    private const val RATE=44100
    fun duration(text:String,s:Settings)=text.sumOf{cellDuration(it,MorseView.MORSE[it.uppercaseChar()]?:"",s)}
    fun write(destination:OutputStream,text:String,s:Settings){
        val clean=text.replace(Regex("\\s+")," ");val totalSamples=ceil(duration(clean,s)*RATE).toLong();val dataBytes=totalSamples*2
        require(dataBytes<=0xffffffffL){"Audio is too large for WAV format"}
        BufferedOutputStream(destination).use{out->
            out.write("RIFF".toByteArray());writeInt(out,36+dataBytes);out.write("WAVEfmt ".toByteArray());writeInt(out,16);writeShort(out,1);writeShort(out,1);writeInt(out,RATE.toLong());writeInt(out,(RATE*2).toLong());writeShort(out,2);writeShort(out,16);out.write("data".toByteArray());writeInt(out,dataBytes)
            var written=0L
            pcmChunks(clean,s){bytes->val remaining=(totalSamples-written).coerceAtLeast(0).coerceAtMost((bytes.size/2).toLong()).toInt();out.write(bytes,0,remaining*2);written+=remaining}
            val zero=ByteArray(8192);while(written<totalSamples){val count=min((zero.size/2).toLong(),totalSamples-written).toInt();out.write(zero,0,count*2);written+=count}
        }
    }
    fun pcmChunks(text:String,s:Settings,consume:(ByteArray)->Unit){text.replace(Regex("\\s+")," ").forEach{ch->val code=MorseView.MORSE[ch.uppercaseChar()]?:"";val cellSamples=round(cellDuration(ch,code,s)*RATE).toInt();val bytes=ByteArray(cellSamples*2);val dot=1.2/s.charWpm;var cursor=0;code.forEach{mark->val count=((if(mark=='-')dot*3 else dot)*RATE).roundToInt();val edge=max(1,min((.005*RATE).roundToInt(),count/4));for(i in 0 until count){val at=cursor+i;if(at>=cellSamples)break;val envelope=min(1.0,min(i.toDouble()/edge,(count-i-1).toDouble()/edge));val sample=(sin(2*PI*s.pitch*i/RATE)*Short.MAX_VALUE*.32*(s.volume/100.0)*envelope).roundToInt().toShort().toInt();bytes[at*2]=(sample and 0xff).toByte();bytes[at*2+1]=((sample ushr 8) and 0xff).toByte()};cursor+=count+(dot*RATE).roundToInt()};consume(bytes)}}
    private fun cellDuration(ch:Char,code:String,s:Settings):Double{if(ch==' ')return 12.0/s.textWpm;if(code.isEmpty())return 12.0/s.wordWpm;val units=code.fold(0){sum,mark->sum+if(mark=='-')3 else 1}+max(0,code.length-1);return max(units*1.2/s.charWpm+3*1.2/s.wordWpm,12.0/s.wordWpm)}
    private fun writeShort(out:OutputStream,value:Int){out.write(value and 0xff);out.write(value ushr 8 and 0xff)}
    private fun writeInt(out:OutputStream,value:Long){repeat(4){out.write((value ushr (it*8) and 0xff).toInt())}}
}

object MorseM4a{
    private const val RATE=44100
    fun write(file:java.io.FileDescriptor,text:String,s:Settings){
        val format=MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,RATE,1).apply{setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC);setInteger(MediaFormat.KEY_BIT_RATE,96000);setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,16384)}
        val codec=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);val muxer=MediaMuxer(file,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);val info=MediaCodec.BufferInfo();var track=-1;var muxerStarted=false;var submittedSamples=0L
        fun drain(end:Boolean){var waiting=true;while(waiting){val index=codec.dequeueOutputBuffer(info,if(end)10000 else 0);when{index==MediaCodec.INFO_TRY_AGAIN_LATER->waiting=end;index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED->{track=muxer.addTrack(codec.outputFormat);muxer.start();muxerStarted=true};index>=0->{val buffer=codec.getOutputBuffer(index)!!;if(info.size>0&&muxerStarted&&(info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){buffer.position(info.offset);buffer.limit(info.offset+info.size);muxer.writeSampleData(track,buffer,info)};codec.releaseOutputBuffer(index,false);if(info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0)waiting=false}}}}
        fun feed(bytes:ByteArray){var offset=0;while(offset<bytes.size){var index=codec.dequeueInputBuffer(10000);while(index<0){drain(false);index=codec.dequeueInputBuffer(10000)};val buffer=codec.getInputBuffer(index)!!;buffer.clear();val count=min(buffer.remaining(),bytes.size-offset);buffer.put(bytes,offset,count);codec.queueInputBuffer(index,0,count,submittedSamples*1_000_000L/RATE,0);submittedSamples+=count/2;offset+=count;drain(false)}}
        try{codec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);codec.start();MorseWav.pcmChunks(text,s){feed(it)};var eos=codec.dequeueInputBuffer(10000);while(eos<0){drain(false);eos=codec.dequeueInputBuffer(10000)};codec.queueInputBuffer(eos,0,0,submittedSamples*1_000_000L/RATE,MediaCodec.BUFFER_FLAG_END_OF_STREAM);drain(true)}finally{try{codec.stop()}catch(_:Exception){};codec.release();if(muxerStarted)try{muxer.stop()}catch(_:Exception){};muxer.release()}
    }
}
