package com.example.calculimc


import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var editTextPoids: EditText
    private lateinit var editTextTaille: EditText
    private lateinit var textViewImc: TextView
    private lateinit var textViewCategorie: TextView
    private lateinit var textViewHistorique: TextView
    private lateinit var progressBarIMC: ProgressBar
    private lateinit var buttonPartager: Button

    private var dernierResultat = ""
    private var dernierIMC = ""
    private var derniereCategorie = ""
    private var derniereCouleur = Color.BLACK
    private var derniereValeurIMC = 0

    private val historique = mutableListOf<String>()

    private val preferences by lazy {
        getSharedPreferences("CalculIMC", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextPoids = findViewById(R.id.editTextPoids)
        editTextTaille = findViewById(R.id.editTextTaille)
        textViewImc = findViewById(R.id.textViewImc)
        textViewCategorie = findViewById(R.id.textViewCategorie)
        textViewHistorique = findViewById(R.id.textViewHistorique)
        progressBarIMC = findViewById(R.id.progressBarIMC)

        val buttonCalculer: Button =
            findViewById(R.id.buttonCalculer)

        val buttonEffacer: Button =
            findViewById(R.id.buttonEffacer)

        buttonPartager = findViewById(R.id.buttonPartager)

        buttonCalculer.setOnClickListener {
            calculerIMC()
        }

        buttonEffacer.setOnClickListener {
            effacer()
        }

        buttonPartager.setOnClickListener {
            partagerResultat()
        }

        chargerHistorique()

        if (savedInstanceState != null) {
            restaurerResultat(savedInstanceState)
        } else {
            afficherHistorique()
        }
    }

    private fun calculerIMC() {
        val poidsTexte = editTextPoids.text.toString()
            .trim().replace(',', '.')

        val tailleTexte = editTextTaille.text.toString()
            .trim().replace(',', '.')

        if (poidsTexte.isEmpty() || tailleTexte.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.error_empty),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val poids = poidsTexte.toDoubleOrNull()
        val tailleCm = tailleTexte.toDoubleOrNull()

        if (poids == null || tailleCm == null ||
            !poids.isFinite() || !tailleCm.isFinite()) {
            Toast.makeText(
                this,
                getString(R.string.error_number),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (poids <= 0 || tailleCm <= 0) {
            Toast.makeText(
                this,
                getString(R.string.error_invalid),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Convertir la taille de centimetres en metres
        val tailleM = tailleCm / 100.0

        // Calculer l'IMC
        val imc = poids / (tailleM * tailleM)

        val resultat = String.format(
            Locale.getDefault(), "%.2f", imc
        )

        val categorie: String
        val couleur: Int

        when {
            imc < 18.5 -> {
                categorie = "Insuffisance ponderale"
                couleur = Color.rgb(230, 126, 34)
            }

            imc < 25 -> {
                categorie = "Corpulence normale"
                couleur = Color.rgb(39, 174, 96)
            }

            imc < 30 -> {
                categorie = "Surpoids"
                couleur = Color.rgb(230, 126, 34)
            }

            imc < 35 -> {
                categorie = "Obesite moderee"
                couleur = Color.RED
            }

            imc < 40 -> {
                categorie = "Obesite severe"
                couleur = Color.RED
            }

            else -> {
                categorie = "Obesite tres severe"
                couleur = Color.rgb(139, 0, 0)
            }
        }

        dernierIMC = resultat
        derniereCategorie = categorie
        derniereCouleur = couleur
        derniereValeurIMC = imc.toInt().coerceIn(0, 40)

        textViewImc.text = "Votre IMC : $resultat"
        textViewCategorie.text = "Categorie : $categorie"

        textViewImc.setTextColor(couleur)
        textViewCategorie.setTextColor(couleur)

        progressBarIMC.progress = derniereValeurIMC
        progressBarIMC.progressTintList =
            ColorStateList.valueOf(couleur)

        dernierResultat =
            "Mon IMC est de $resultat. Categorie : $categorie."

        buttonPartager.visibility = View.VISIBLE

        // Ajouter le resultat a l'historique
        historique.add(
            0,
            "$resultat - $categorie (Poids : $poids kg, Taille : $tailleCm cm)"
        )

        // Garder seulement les cinq derniers calculs
        while (historique.size > 5) {
            historique.removeAt(historique.lastIndex)
        }

        sauvegarderHistorique()
        afficherHistorique()
    }

    private fun effacer() {
        editTextPoids.text.clear()
        editTextTaille.text.clear()

        dernierIMC = ""
        derniereCategorie = ""
        derniereCouleur = Color.BLACK
        derniereValeurIMC = 0
        dernierResultat = ""

        textViewImc.text = getString(R.string.result_label)
        textViewCategorie.text = getString(R.string.category_label)

        textViewImc.setTextColor(Color.BLACK)
        textViewCategorie.setTextColor(Color.BLACK)

        progressBarIMC.progress = 0
        progressBarIMC.progressTintList =
            ColorStateList.valueOf(Color.rgb(39, 174, 96))

        buttonPartager.visibility = View.GONE

        editTextPoids.requestFocus()
    }

    private fun partagerResultat() {
        if (dernierResultat.isEmpty()) {
            return
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, dernierResultat)
        }

        startActivity(
            Intent.createChooser(
                intent,
                getString(R.string.share_title)
            )
        )
    }

    private fun sauvegarderHistorique() {
        preferences.edit()
            .putString("historique", historique.joinToString("\n"))
            .apply()
    }

    private fun chargerHistorique() {
        historique.clear()

        val texte = preferences.getString("historique", "") ?: ""

        if (texte.isNotEmpty()) {
            historique.addAll(texte.split("\n"))
        }

        afficherHistorique()
    }

    private fun afficherHistorique() {
        textViewHistorique.text =
            if (historique.isEmpty()) {
                "Historique des calculs\nAucun calcul pour le moment."
            } else {
                "Historique des calculs\n\n" +
                        historique.joinToString("\n\n")
            }
    }

    private fun restaurerResultat(state: Bundle) {
        dernierIMC = state.getString("imc", "")
        derniereCategorie = state.getString("categorie", "")
        derniereCouleur = state.getInt("couleur", Color.BLACK)
        derniereValeurIMC = state.getInt("valeurIMC", 0)
        dernierResultat = state.getString("partage", "")

        if (dernierIMC.isNotEmpty()) {
            textViewImc.text = "Votre IMC : $dernierIMC"
            textViewCategorie.text = "Categorie : $derniereCategorie"

            textViewImc.setTextColor(derniereCouleur)
            textViewCategorie.setTextColor(derniereCouleur)

            progressBarIMC.progress = derniereValeurIMC
            progressBarIMC.progressTintList =
                ColorStateList.valueOf(derniereCouleur)

            buttonPartager.visibility = View.VISIBLE
        }

        afficherHistorique()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("imc", dernierIMC)
        outState.putString("categorie", derniereCategorie)
        outState.putInt("couleur", derniereCouleur)
        outState.putInt("valeurIMC", derniereValeurIMC)
        outState.putString("partage", dernierResultat)

        super.onSaveInstanceState(outState)
    }
}
