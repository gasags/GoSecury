package com.example.gosecury

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.provider.MediaStore
import android.widget.ImageView
import com.microsoft.projectoxford.face.*
import com.microsoft.projectoxford.face.contract.*
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import com.microsoft.projectoxford.face.contract.IdentifyResult
import android.app.AlertDialog
import com.microsoft.projectoxford.face.contract.TrainingStatus
import android.R.attr.name

class MainActivity : AppCompatActivity() {

    val REQUEST_IMAGE_CAPTURE = 1

    enum class buttonType{
        AUTH, CONTROL, NONE
    }
    var btnEnum = buttonType.NONE

    var apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0"
    var subscriptionKey = "c0ffd6af1bbf49eab25cea25f593ff60"
    var faceServiceClient = FaceServiceRestClient(apiEndpoint, subscriptionKey)
    var personGroupId = "gosecury"
    lateinit var personIdentify : Person



    val faceIdList : ArrayList<UUID> = arrayListOf()
    val personIdIdentify : ArrayList<UUID> = arrayListOf()

    //region fonction async

    val faceListSend = object : AsyncTask<InputStream, String, Array<Face>>() {

        override fun doInBackground(vararg params: InputStream): Array<Face>? {
            try {
                val result = faceServiceClient.detect(
                    params[0],
                    true,
                    false, null
                )
                if(result != null)
                {
                    for (face in result) {
                        println(" faceId : " + face.faceId)
                        faceIdList.add(face.faceId)
                    }
                }
                return result
            } catch (e: Exception) {
                println("erreur envoie image " + e.message)
                return null
            }
        }
    }

    val faceIdentify = object : AsyncTask<UUID, String, Array<IdentifyResult>>()
    {
        override fun doInBackground(vararg params: UUID): Array<IdentifyResult>?
        {
            try
            {
                println("faceIdentify " + personGroupId + " " + params)
                var result = faceServiceClient.identity(
                    personGroupId, // person group id
                    params // face ids
                    , 5
                )
                if (result != null)
                {
                    for (identifyResult in result) {
                        println(identifyResult.candidates[0].personId)
                        personIdIdentify.add(identifyResult.candidates[0].personId)
                    }
                }
                return result
            }
            catch (e: Exception)
            {
                println("erreur identification " + e.message)
                return null
            }
        }
    }

    val person = object : AsyncTask<UUID, String, Person>()
    {
        override fun doInBackground(vararg params: UUID): Person?
        {
            try
            {
                personIdentify = faceServiceClient.getPerson(personGroupId, params[0])
                println(" nom : " + personIdentify.name)
                return personIdentify
                //return faceServiceClient.getPerson(personGroupId, params[0])
            }
            catch (e: Exception)
            {
                println("erreur getPerson" + e.message)
                return null
            }
        }
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Listener du bouton "s'authentifier" pour prendre une photo
        val btnAuth = findViewById<Button>(R.id.btnAuthentication)
        btnAuth.setOnClickListener {
            btnEnum = buttonType.AUTH
            pictureAppIntent()
        }

        // Listener du bouton "Effectuer un controle" pour prendre une photo
        val btnControl = findViewById<Button>(R.id.btnControl)
        btnControl.setOnClickListener {
            btnEnum = buttonType.CONTROL
            pictureAppIntent()
        }
    }

    // On récupère la photo prise pour la traiter
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val bmUser = data?.extras?.get("data") as Bitmap

            val dialogConnexion = AlertDialog.Builder(this)
            var alert = dialogConnexion.create()

            val outputStream = ByteArrayOutputStream()
            bmUser.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val inputStream = ByteArrayInputStream(outputStream.toByteArray())

            // Permet de vérifier la page de redirection
            if(btnEnum == buttonType.AUTH)
            {
                faceListSend.execute(inputStream)

                if(faceIdList.count() > 0)
                {
                    faceIdentify.execute(faceIdList[0])

                    if(personIdIdentify.count() > 0)
                    {
                        person.execute(personIdIdentify[0])

                        if(personIdentify != null)
                        {
                            val intent = Intent(this, GestionMateriel::class.java)
                            intent.putExtra("bmUser", outputStream.toByteArray())
                            intent.putExtra("idUser", "X8PZxh8S3ZiVHBruKDBC")
                            startActivity(intent)
                            finish()
                        }
                    }
                    else
                        alert.setMessage("Vous n'êtes pas autorisé à accéder au stock")
                }
                else
                    alert.setMessage("Aucun visage détecté, veuillez réessayer")
            }
            else if(btnEnum == buttonType.CONTROL)
            {
                val intent = Intent(this, Controle::class.java)
                intent.putExtra("bmUser", outputStream.toByteArray())
                startActivity(intent)
                finish()
            }
        }
    }

    // On ouvre l'appli d'appareil photo
    private fun pictureAppIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also {
                takePictureIntent -> takePictureIntent.resolveActivity(packageManager)?.also {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
        }
    }
}
