package com.example.gamecollector

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {

    @Suppress("DEPRECATION")
    private inner class DataDownloader: AsyncTask<String, Int, String>(){

        var u: String = ""
        var name: String = ""
        var main: Boolean = false

        fun setData(adress: String, name: String, m:Boolean){
            this.u = adress
            this.name = name
            this.main = m
        }

        override fun onPreExecute() {
            super.onPreExecute()
            downloadFinished = false
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
        }

        override fun doInBackground(vararg p0: String?): String {

            try {
                val url = URL(u)
                val connection = url.openConnection()
                connection.connect()
                val istream = connection.getInputStream()
                val content = istream.bufferedReader().use { it.readText() }
                val lenghtOfFile = content.length
                val isStream = url.openStream()
                val testDirectory = File("$filesDir/XML")
                if(!testDirectory.exists()){
                    testDirectory.mkdir()
                }
                val fos = FileOutputStream("$testDirectory/$name.xml")
                val data = ByteArray(1024)
                var count = 0
                var total:Long = 0
                var progress = 0
                count = isStream.read(data)
                while(count != -1){
                    total += count.toLong()
                    val progress_tmp = total.toInt()*100/lenghtOfFile
                    if(progress_tmp%10 == 0 && progress != progress_tmp){
                        progress = progress_tmp
                        publishProgress(progress)
                    }
                    fos.write(data, 0, count)
                    count = isStream.read(data)
                }
                isStream.close()
                fos.close()
            }catch (e: MalformedURLException){
                return "Zły URL"
            }catch (e: FileNotFoundException){
                return "Brak pilku"
            }catch (e: IOException){
                return "wyjątek IO"
            }
            while(!saveRetry(name, main)){
                if(!expired){
                    sleepyHead()
                }
                else{
                    val builder = AlertDialog.Builder(this@MainActivity)
                    builder.setMessage("Synchronisation failure.\nDo you want to continue?")
                        .setCancelable(false)
                        .setPositiveButton("Yes") { dialog, id ->
                            dialog.dismiss()
                            expired = false
                        }
                        .setNegativeButton("No") { dialog, id ->
                            dialog.dismiss()
                            finish()
                        }
                    val alert = builder.create()
                    alert.show()
                }
            }
            downloadFinished = true
            return "success"
        }

    }


    var saveFinished: Boolean = false
    var downloadFinished: Boolean = false
    var userName: String = "Karol"
    var lastSync: String = "2022-01-01"
    var games: Int = 0
    var addons: Int = 0
    var globalAddonsCheck: Int = 1
    var expired: Boolean = false
    val sdf = SimpleDateFormat("yyyy-MM-dd")
    val toCheck: MutableList<String> = ArrayList()

    lateinit var progressDialog: AlertDialog
    lateinit var userButton: Button
    lateinit var syncButton: Button
    lateinit var gamesButton: Button
    lateinit var addsButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        userButton = findViewById(R.id.User)
        syncButton = findViewById(R.id.Sync)
        gamesButton = findViewById(R.id.Games)
        addsButton = findViewById(R.id.Adds)
        checkLogIn()
        userButton.text = "Hello $userName\nClear data"
        setTexts()
    }

    fun checkLogIn(){
        if(checkForInternet(this)){
            Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show()
            val filename =  "$filesDir/data.txt"
            val file = File(filename)
            if(file.exists())
            {
                var datas: List<String> = File(filename).bufferedReader().readLines()
                userName = datas[0]
                lastSync = datas[1]
                games = Integer.parseInt(datas[2])
                addons = Integer.parseInt(datas[3])
            }
            else{
                println("File does not exist.")
                logIn(file)
            }
        }
        else{
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setMessage("This app requires internet connection.\nTry again.")
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, id ->
                    dialog.dismiss()
                    finish()
                }
            val alert = builder.create()
            alert.show()
        }
    }


    fun logIn(f: File) {
        val popupView: View = layoutInflater.inflate(R.layout.popup, null)
        val userNameField: EditText = popupView.findViewById(R.id.editUsername)
        val accept: Button = popupView.findViewById(R.id.okButton)
        val dialogBuilder = AlertDialog.Builder(this).setView(popupView)
        val dialog: AlertDialog = dialogBuilder.create()
        dialog.show()
        accept.setOnClickListener() {
            if (userNameField.text.toString() == "") {
                Toast.makeText(this, "Empty username", Toast.LENGTH_SHORT).show() // nie działa
            }else{
                userName = userNameField.text.toString()
                userButton.text = "Hello $userName\nClear data"
                dialog.dismiss()
                sync(userName)
            }
        }
    }
    fun clearData(v: View){
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setMessage("Are you sure you want to clear your data?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                var path = Paths.get("$filesDir/data.txt")
                try {
                    val result = Files.deleteIfExists(path)
                    if (result) {
                        println("Deletion succeeded.")
                    } else {
                        println("Deletion failed.")
                    }
                } catch (e: IOException) {
                    println("Deletion failed.")
                    e.printStackTrace()
                }
                val dbHandler = DBHandlerMain(this, null, null,  1)
                dbHandler.clear()
                val dbStat = DBHandlerStat(this, null,null, 1)
                dbStat.clear()
                finish()
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()

    }
    fun finishSync(){
        val dbHandler = DBHandlerMain(this, null, null,  1)
        for(i in 0 .. toCheck.size-1){
            val res = checkIfAddOn(toCheck[i])
            if(res == 1){
                dbHandler.updateAddOn(toCheck[i], res)
            }
        }
        games = dbHandler.countGames()
        addons = dbHandler.countAddons()
        val currentDate = sdf.format(Date())
        lastSync = currentDate
        setTexts()
        val filename =  "$filesDir/data.txt"
        val file = File(filename)
        file.bufferedWriter().use { out ->
            out.write("$userName\n")
            out.write("$lastSync\n")
            out.write("$games\n")
            out.write("$addons\n")
        }
        Toast.makeText(this, "Finished", Toast.LENGTH_LONG)
        progressDialog.dismiss()
    }

    fun setTexts(){
        syncButton.text = "Last synchronized:\n $lastSync"
        gamesButton.text = "Games owned:\n $games"
        addsButton.text = "Add-ons owned:\n $addons"
    }
    fun dateDiff(a: String, b: String): Long {
        var spt = a.split('-')
        val a_year = (spt[0].toLong())
        val a_month = (spt[1].toLong())
        val a_day = (spt[2].toLong())
        spt = b.split('-')
        val b_year = (spt[0].toLong())
        val b_month = (spt[1].toLong())
        val b_day = (spt[2].toLong())
        val year_diff = b_year - a_year
        val month_diff = b_month - a_month
        val day_diff = b_day - a_day
        val diff = year_diff * 365 + month_diff * 30 + day_diff
        return diff
    }
    fun sync(user: String) {
        val currentDate = sdf.format(Date())
        lastSync = currentDate
        userName = user
        val q = "https://boardgamegeek.com/xmlapi2/collection?username=$user&stats=1"
        downloadData(q, user+"_collection", true)
        while(!saveFinished){}
        finishSync()

    }
    fun preparePopup(){
        val progressView: View = layoutInflater.inflate(R.layout.progress_popup, null)
        val dialogBuilder = AlertDialog.Builder(this@MainActivity)
        dialogBuilder.setView(progressView)
        progressDialog = dialogBuilder.create()
        progressDialog.show()
    }
    suspend fun sleep(): Int {
        delay(20000L) // pretend we are doing something useful here
        return 1
    }
    fun downloadData(q: String, filename: String, main:Boolean) {
        downloadFinished = false
        val cd = DataDownloader()
        cd.setData(q, filename, main)
        cd.execute()
    }
    fun sleepyHead() = runBlocking<Unit> {
        val time = measureTimeMillis {
            val one = async { sleep() }
            one.await()
        }
    }

    private fun saveRetry(f: String, main: Boolean):Boolean {
        val testDirectory = File("$filesDir/XML")
        val filename =  "$testDirectory/$f.xml"
        val file = File(filename)
        var xmlDoc: Document
        var cor: Boolean = false
        var c = 0
        while(!cor){
            if(c == 10)
            {
                expired = true
                return false
            }
            cor = true
            try{
                xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            }catch (e: Exception){
                e.printStackTrace()
                cor = false
                c++
            }
        }

        xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        xmlDoc.documentElement.normalize()
        val items: NodeList = xmlDoc.getElementsByTagName("item")
        if(items.length == 0) {
            Files.deleteIfExists(Paths.get(filename))
            return false
        }else{
            if(main){
                saveDataMain(items)
            }
            else {
                smallParser(items)
            }
            Files.deleteIfExists(Paths.get(filename))
            return true
        }
    }
    fun smallParser(items: NodeList){
        for(i in 0..items.length-1){
            val itemNode: Node = items.item(i)
            if(itemNode.nodeType == Node.ELEMENT_NODE){
                val tags = itemNode.attributes
                for(j in 0..tags.length-1){
                    val node = tags.item(j)
                    when (node.nodeName){
                        "type" -> {
                            if(node.nodeValue == "boardgame"){
                                globalAddonsCheck = 0
                            }
                            else{
                                globalAddonsCheck = 1
                            }
                            return
                        }
                    }
                }
            }
        }
    }

    private fun saveDataMain(items: NodeList) {
        val dbHandler = DBHandlerMain(this, null, null,  1)
        val dbStat = DBHandlerStat(this, null, null, 1)
        dbHandler.clear()
        for(i in 0..items.length-1){
            val itemNode: Node = items.item(i)
            if(itemNode.nodeType == Node.ELEMENT_NODE) {
                val elem = itemNode as Element
                val children = elem.childNodes
                var id: String? = null
                var title: String? = null
                var org_title: String? = null
                var year_pub: String? = null
                var rank_pos: String? = null
                var pic: String? = null
                var tmp: String? = null
                var expansion: Int = 0
                val tags = itemNode.attributes
                for(j in 0..tags.length-1){
                    val node = tags.item(j)
                    when (node.nodeName){
                        "objectid" -> {id = node.nodeValue}
                    }
                }
                for(j in 0..children.length-1) {
                    val node = children.item(j)
                    if (node is Element) {
                        when (node.nodeName) {
                            "name" -> {title = node.textContent}
                            "yearpublished"->{year_pub = node.textContent}
                            "thumbnail"->{pic = node.textContent}
                            "stats"->{
                                val n = node.childNodes
                                for(j1 in 0..n.length-1){
                                    val node = n.item(j1)
                                    if (node is Element){
                                        when (node.nodeName) {
                                            "rating"->{
                                                val n = node.childNodes
                                                for(j2 in 0..n.length-1){
                                                    val node = n.item(j2)
                                                    if (node is Element) {
                                                        when (node.nodeName) {
                                                            "ranks"->{
                                                                val n = node.childNodes
                                                                for(j3 in 0..n.length-1){
                                                                    val node = n.item(j3)
                                                                    if (node is Element) {
                                                                        val tags = node.attributes
                                                                        for(j4 in 0..tags.length-1){
                                                                            val node = tags.item(j4)
                                                                            when (node.nodeName){
                                                                                "id" -> {tmp = node.nodeValue}
                                                                                "value" -> {rank_pos = node.nodeValue}
                                                                            }
                                                                            if(tmp == "1" && rank_pos != null){
                                                                                break
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                var _expansion: Int = 0
                if(rank_pos == "Not Ranked" || rank_pos == null){
                    rank_pos = "0"
                    toCheck.add(id.toString())
                }
                var _id: Int = Integer.parseInt(id)
                var _title: String? = title
                var _org_title: String? = title
                var _year_pub: Int = 0
                if(year_pub == null){
                    _year_pub = 1900
                }
                else{
                    _year_pub = Integer.parseInt(year_pub)
                }
                var _rank_pos: Int = Integer.parseInt(rank_pos)
                var _pic: String? = pic

                val product = Record(_id, _title, _org_title, _year_pub,_rank_pos, _pic, _expansion)
                dbHandler.addRecord(product)
                if(_rank_pos!= 0){

                    val s = Stat(_id, _rank_pos, lastSync)
                    dbStat.addStat(s)
                }
            }
        }
        saveFinished = true
    }
    fun checkIfAddOn(id: String):Int{
        val q = "https://boardgamegeek.com/xmlapi2/thing?id=$id&stats=1"
        downloadData(q, "tmp", false)
        while(!downloadFinished){}
        return globalAddonsCheck
    }

    private fun checkForInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
    fun syncClicked(v:View){
        Toast.makeText(this, "Syncing...", Toast.LENGTH_LONG).show()
        val today = sdf.format(Date())
        if(dateDiff(lastSync, today) == 0L){
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setMessage("Data is up to date.\nAre you sure you want to synchronize?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    dialog.dismiss()
                    preparePopup()
                    sync(userName)
                }
                .setNegativeButton("No") { dialog, id ->
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }
        else{
            preparePopup()
            sync(userName)
        }

    }
    private fun showActivityGames(addons: Boolean) {
        val i = Intent(this, GameView::class.java)
        val b = Bundle()
        b.putBoolean("Addons", addons)
        i.putExtras(b)
        startActivityForResult(i, 0)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 0 && resultCode == Activity.RESULT_OK){
            progressDialog.dismiss()
        }
    }
    fun gamesClicked(v:View){
        preparePopup()
        showActivityGames(false)
    }
    fun addonsClicked(v:View){
        preparePopup()
        showActivityGames(true)
    }

}