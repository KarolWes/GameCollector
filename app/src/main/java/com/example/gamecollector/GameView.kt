package com.example.gamecollector

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors


class GameView : AppCompatActivity() {
    var addons: Boolean = false
    var desc = false
    var actO = Orders._ID

    fun Boolean.toInt() = if (this) 1 else 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_view)
        val extras = intent.extras
        addons = extras!!.getBoolean("Addons")
        var textView: TextView = findViewById(R.id.textView)
        if(addons){
            textView.text = "Add-Ons"
        }
        else{
            textView.text = "Games"
        }
        val dbHandler = DBHandlerMain(this, null, null,  1)
        val theList = dbHandler.getVals(addons.toInt(), actO, desc)
        populate(theList)
    }
    fun stringCutter(src: String):String{
        var res: String = ""
        var lim = 10
        for(i in 0..src.length-1){
            if(src[i] != ' '){
                res += src[i]
            }
            else{
                if(i >= lim){
                    res+= '\n'
                    lim+=i
                }
                else{
                    res+=' '
                }
            }
        }
        return res
    }

    private fun populate(l: List<Record>) {
        val table: TableLayout = findViewById(R.id.tblLayout)
        for (i in 0..l.lastIndex) {
            val row = TableRow(this)
            val lp = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
            row.layoutParams = lp
            val textViewId = TextView(this)
            textViewId.text = l[i].id.toString()
            textViewId.setPadding(20, 15, 20, 15)
            textViewId.setTextColor(resources.getColor(R.color.purple_500))
            val textViewName = TextView(this)
            textViewName.text = stringCutter(l[i].title!!)
            textViewName.setPadding(20, 15, 20, 15)
            textViewName.setTextColor(resources.getColor(R.color.purple_500))
            val textViewYear = TextView(this)
            textViewYear.text = l[i].year_pub.toString()
            textViewYear.setPadding(20, 15, 20, 15)
            textViewYear.setTextColor(resources.getColor(R.color.purple_500))
            val im = ImageView(this)
            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            var image: Bitmap? = null
            executor.execute {
                val imageURL = l[i].pic
                try {
                    val `in` = java.net.URL(imageURL).openStream()
                    image = BitmapFactory.decodeStream(`in`)
                    handler.post {
                        im.setImageBitmap(image)
                    }
                } catch (e: Exception) {
                    image = BitmapFactory.decodeResource(
                        this.resources,
                        R.drawable.ic_rook_chess_svgrepo_com
                    )
                    im.setImageBitmap(image)
                    im.setColorFilter(R.color.purple_500)
                    e.printStackTrace()
                }
            }
            row.addView(textViewId)
            row.addView(textViewName)
            row.addView(textViewYear)
            row.addView(im)
            if(!addons){
                val textViewRank = TextView(this)
                if(l[i].rank_pos == 0){
                    textViewRank.text = "N/A"
                }
                else{
                    textViewRank.text = l[i].rank_pos.toString()
                    row.setOnClickListener() {
                        callStats(l[i].id)
                    }
                }
                textViewRank.setPadding(20, 15, 20, 15)
                textViewRank.setTextColor(resources.getColor(R.color.purple_500))
                textViewRank.gravity = Gravity.CENTER
                row.addView(textViewRank)
            }
            else{
                val rankCap: TextView = findViewById(R.id.RANKtv)
                rankCap.visibility = View.INVISIBLE
            }
            if (i % 2 == 0) {
                row.setBackgroundColor(resources.getColor(R.color.table_even))
            } else {
                row.setBackgroundColor(resources.getColor(R.color.table_odd))
            }
            table.addView(row, i)
        }
    }
    fun callStats(id: Int) {
        val i = Intent(this, StatsView::class.java)
        val b = Bundle()
        b.putInt("Id", id)
        i.putExtras(b)
        startActivity(i)
    }

    fun clear(){
        val table: TableLayout = findViewById(R.id.tblLayout)
        val childCount = table.childCount
        if (childCount > 0) {
            table.removeViews(0, childCount)
        }
    }
    fun checkSet(new: Orders){
        if(actO == new){
            desc = desc xor true
        }
        else{
            desc = false
            actO = new
        }
    }

    fun Resort(v: View){
        clear()
        val i = v.id

        when(i){
            R.id.IDtv->{
                checkSet(Orders._ID)
            }
            R.id.TITLEtv->{
                checkSet(Orders.TITLE)
            }
            R.id.YEARtv->{
                checkSet(Orders.YEAR_PUB)
            }
            R.id.RANKtv->{
                if(!addons){
                    checkSet(Orders.RANK_POS)
                }
            }
        }
        val dbHandler = DBHandlerMain(this, null, null,  1)
        val theList = dbHandler.getVals(addons.toInt(), actO, desc)
        populate(theList)
    }
    override fun finish() {
        setResult(Activity.RESULT_OK, null)
        super.finish()
    }
}