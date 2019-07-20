package com.example.gosecury

import android.content.Intent
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.CompoundButtonCompat
import android.util.Log
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import kotlinx.android.synthetic.main.activity_gestion_materiel.*
import kotlin.collections.ArrayList

class GestionMateriel : AppCompatActivity() {

    val db = FirebaseFirestore.getInstance()
    var idUser = ""

    // liste permettant de stocker les id de firestore de la collection "materials"
    val listIdMaterial = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_materiel)

        //On récupère l'id de l'utilisateur
        idUser = intent.getStringExtra("idUser") as String

        getMaterial(idUser)

        //listener du bouton "Accueil" pour rediriger sur la page principale
        val btnHome = findViewById<Button>(R.id.btnHome)

        btnHome.setOnClickListener {
            redirectHome()
        }

        // On récupère l'image envoyée en ByteArray pour l'afficher
        val byteArrayBmUser =  intent.getByteArrayExtra("bmUser")
        val bmUser = BitmapFactory.decodeByteArray(byteArrayBmUser, 0, byteArrayBmUser.size)
        ivUser.setImageBitmap(bmUser)
    }

    // Redirection vers la page principale "MaintActivity"
    private fun redirectHome(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Affichage des checkBoxs et modifications lors d'un changement en BDD
    private fun getMaterial(idUser : String) {

        val linearLayoutForCheckBoxs = findViewById<LinearLayout>(R.id.llCheckBox)

        db.collection("materials")
            .addSnapshotListener(EventListener<QuerySnapshot> { value, e ->
                if (e != null) {
                    Log.w("error", "Listener error", e)
                }

                // On boucle sur tous les documents de la collection "materials" en vérifiant qu'ils ne soient pas null puis se lance à chaque modif en bdd
                for ((index, doc) in value!!.documentChanges.withIndex()) {
                    when (doc.type) {
                        // Déclenchement lorsqu'on accède à la page
                        DocumentChange.Type.ADDED -> {
                            // init du checkBox
                            val cb = CheckBox(this)
                            val usersMaterial = doc.document["Users"] as ArrayList<String>

                            listIdMaterial.add(index, doc.document.id)
                            cb.id = index
                            cb.text = doc.document["Nom"] as String
                            cb.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                            CompoundButtonCompat.setButtonTintList(
                                cb,
                                ContextCompat.getColorStateList(this, R.color.colorBtn)
                            )
                            cb.setTextColor(ContextCompat.getColor(this, R.color.colorTxt))

                            // On instancie un Listener
                            cb.setOnClickListener {
                                changeUser(cb, idUser)
                            }

                            // On vérifie si l'utilisateur a déjà le materiel et si la quantité est > à 0
                            if (usersMaterial.contains(idUser)) {
                                cb.isChecked = true
                            } else if ((doc.document["Quantite"] as Number).toInt() <= 0) {
                                cb.isEnabled = false
                            }

                            // On ajoute le chechBox au layout
                            linearLayoutForCheckBoxs.addView(cb)
                        }


                        // Déclenchement lorsqu'il y a une modification en BDD
                        DocumentChange.Type.MODIFIED -> {

                            val userMaterial = doc.document["Users"] as ArrayList<String>
                            if ((doc.document["Quantite"] as Number).toInt() <= 0 && !userMaterial.contains(idUser)) {
                                val cb = linearLayoutForCheckBoxs.findViewById<CheckBox>(index)
                                if (cb.text == doc.document["Nom"] as String) {
                                    cb.isEnabled = false
                                    linearLayoutForCheckBoxs.removeView(cb)
                                    linearLayoutForCheckBoxs.addView(cb)
                                }
                            }
                        }
                    }
                }
            })
    }

    //Fonction d'ajout/suppression de user sur un material
    private fun changeUser(cb : CheckBox, idUser : String){
        val idMaterial = listIdMaterial[cb.id]
        if (cb.isChecked){
            db.collection("materials").document(idMaterial).update("Users", FieldValue.arrayUnion(idUser))
        }
        else
        {
            db.collection("materials").document(idMaterial).update("Users", FieldValue.arrayRemove(idUser))
        }
    }
}
