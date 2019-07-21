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
    lateinit var alert : AlertDialog

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

        val dialogConnexion = AlertDialog.Builder(this)
        alert = dialogConnexion.create()
    }

    // On récupère la photo prise pour la traiter
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val bmUser = data?.extras?.get("data") as Bitmap

            // Permet de vérifier la page de redirection
            if(btnEnum == buttonType.AUTH)
            {
                detectFaceByPhoto(bmUser)
            }
            else if(btnEnum == buttonType.CONTROL)
            {
                /*val intent = Intent(this, Controle::class.java)
                intent.putExtra("bmUser", outputStream.toByteArray())
                startActivity(intent)
                finish()*/
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

    private fun detectFaceByPhoto(bitMap: Bitmap)
    {
        val outputStream = ByteArrayOutputStream()
        bitMap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        val faceIdList : ArrayList<UUID> = arrayListOf()

        val faceListSend = object : AsyncTask<InputStream, String, Array<Face>>() {

            override fun doInBackground(vararg params: InputStream): Array<Face>? {
                try {
                    return faceServiceClient.detect(
                        params[0],
                        true,
                        false, null
                    )
                } catch (e: Exception) {
                    println("erreur envoie image " + e.message)
                    return null
                }
            }

            override fun onPostExecute(result: Array<Face>)
            {
                if(result.count() > 0)
                {
                    for (face in result)
                    {
                        faceIdList.add(face.faceId)
                    }
                    identifyPerson(faceIdList[0])
                }
                else
                {
                    alert.setMessage("Aucun visage détecté, veuillez réessayer")
                    alert.show()
                }
            }
        }
        faceListSend.execute(inputStream)
    }

    private fun identifyPerson(faceIdList: UUID)
    {
        val personIdIdentify : ArrayList<UUID> = arrayListOf()

        val faceIdentify = object : AsyncTask<UUID, String, Array<IdentifyResult>>()
        {
            override fun doInBackground(vararg params: UUID): Array<IdentifyResult>?
            {
                try
                {
                    return faceServiceClient.identity(
                        personGroupId, // person group id
                        params // face ids
                        , 5
                    )
                }
                catch (e: Exception)
                {
                    println("erreur identification " + e.message)
                    return null
                }
            }

            override fun onPostExecute(result: Array<IdentifyResult>)
            {
                if (result[0].candidates.count() > 0)
                {
                        personIdIdentify.add(result[0].candidates[0].personId)
                        getPerson(personIdIdentify[0])
                }
                else
                {
                    alert.setMessage("Vous n'avez pas accès au stock")
                    alert.show()
                }
            }
        }
        faceIdentify.execute(faceIdList)
    }

    private fun getPerson(personId: UUID)
    {
        val person = object : AsyncTask<UUID, String, Person>()
        {
            override fun doInBackground(vararg params: UUID): Person?
            {
                try
                {
                    return faceServiceClient.getPerson(personGroupId, params[0])
                }
                catch (e: Exception)
                {
                    println("erreur getPerson" + e.message)
                    return null
                }
            }

            override fun onPostExecute(result: Person)
            {
                alert.setMessage("Bonjour " + result.name)
                alert.show()
            }
        }
        person.execute(personId)
    }
}
