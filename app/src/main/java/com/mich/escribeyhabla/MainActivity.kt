package com.mich.escribeyhabla


import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechSpeedSeekBar: SeekBar? = null
    private var isSpeaking: Boolean = false
    private var partesTexto: MutableList<String> = mutableListOf()
    private var posicionActual: Int = 0

    private lateinit var scrollView: ScrollView
    private lateinit var etMessage: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inicializarTextToSpeech()
        configurarBotonClic()
        configurarSeekBarVelocidad()

        supportActionBar?.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "Escribe y Habla"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        scrollView = findViewById(R.id.scrollView)
        etMessage = findViewById(R.id.etMessage)
    }

    private fun inicializarTextToSpeech() {
        tts = TextToSpeech(this, this)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                avanzarParte()
            }

            override fun onError(utteranceId: String?) {}
        })
    }

    private fun configurarBotonClic() {
        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            if (!isSpeaking) {
                hablar()
            } else {
                detenerHabla()
            }
        }
    }

    private fun configurarSeekBarVelocidad() {
        speechSpeedSeekBar = findViewById(R.id.seekBarSpeed)

        val progresoPuntoMedio = 50
        speechSpeedSeekBar?.progress = progresoPuntoMedio

        speechSpeedSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val velocidad = (0.5f + (progress / 100f)).coerceIn(0.1f, 2.0f)
                establecerVelocidadHabla(velocidad)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun hablar() {
        val mensaje: String = etMessage.text.toString()
        if (mensaje.isNotEmpty()) {
            partirTexto(mensaje)
            reproducirPartes()
            isSpeaking = true
        } else {
            tts?.speak("Introduce un texto por favor", TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    private fun partirTexto(mensaje: String) {
        partesTexto.clear()

        val palabras = mensaje.split("\\s+".toRegex())
        val palabrasPorParte = 20

        for (i in 0 until palabras.size step palabrasPorParte) {
            val inicio = i
            val fin = minOf(i + palabrasPorParte, palabras.size)
            val parte = palabras.subList(inicio, fin).joinToString(" ")
            partesTexto.add(parte)
        }
    }

    private fun reproducirPartes() {
        posicionActual = 0
        resaltarParteActual()

        val params = HashMap<String, String>()
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "parte"
        tts?.speak(partesTexto[0], TextToSpeech.QUEUE_FLUSH, params)
    }

    private fun avanzarParte() {
        posicionActual++
        if (posicionActual < partesTexto.size) {
            resaltarParteActual()

            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "parte"
            if (posicionActual == 1) {
                tts?.speak(partesTexto[posicionActual], TextToSpeech.QUEUE_FLUSH, params)
            } else {
                tts?.speak(partesTexto[posicionActual], TextToSpeech.QUEUE_ADD, params)
            }
        } else {
            detenerHabla()
        }
    }

    private fun detenerHabla() {
        tts?.stop()
        isSpeaking = false
        partesTexto.clear()
        posicionActual = 0
    }

    private fun resaltarParteActual() {
        val textoCompleto = partesTexto.joinToString(" ")
        val posicion = textoCompleto.indexOf(partesTexto[posicionActual])

        if (posicion >= 0) {
            val resaltado = SpannableString(textoCompleto)
            resaltado.setSpan(
                BackgroundColorSpan(ContextCompat.getColor(this, android.R.color.darker_gray)),
                posicion,
                posicion + partesTexto[posicionActual].length,
                0
            )
            etMessage.setText(resaltado, TextView.BufferType.SPANNABLE)

            // Scroll hacia la parte actual
            scrollView.post {
                val offset = etMessage.bottom - (scrollView.height / 2)
                scrollView.smoothScrollTo(0, offset)
                etMessage.requestFocus() // Mantener el foco en el EditText
            }
        }
    }

    private fun establecerVelocidadHabla(velocidad: Float) {
        tts?.setSpeechRate(velocidad)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            establecerIdiomaTts(Locale("es"))
        } else {
            manejarFalloInicializacionTts()
        }
    }

    private fun establecerIdiomaTts(locale: Locale) {
        tts?.language = locale
    }

    private fun manejarFalloInicializacionTts() {
        findViewById<Button>(R.id.btnPlay).isEnabled = false
    }

    override fun onDestroy() {
        Log.d("MainActivity", "onDestroy")
        if (!isFinishing) {
            tts?.stop()
            tts?.shutdown()
        }
        super.onDestroy()
    }

    override fun onPause() {
        Log.d("MainActivity", "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d("MainActivity", "onStop")
        super.onStop()
    }

    override fun onResume() {
        Log.d("MainActivity", "onResume")
        super.onResume()

    }
}