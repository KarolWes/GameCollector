package com.example.gamecollector

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.security.KeyStore
import java.util.concurrent.Executors

class StatsView : AppCompatActivity() {
    var Id: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats_view)
        val extras = intent.extras
        Id = extras!!.getInt("Id")
        val dbm = DBHandlerMain(this, null, null, 1)
        val rec = dbm.findRecord(Id)
        setCaptions(rec!!)
        populate()
    }

    private fun populate() {
        val dbs = DBHandlerStat(this, null, null, 1)
        val table: TableLayout = findViewById(R.id.tblStats)
        val theList = dbs.getStats(Id)
        prepareChart(theList)
        for (i in 0..theList.size - 1) {
            val row = TableRow(this)
            val lp = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
            row.layoutParams = lp
            row.gravity = Gravity.CENTER_HORIZONTAL
            row.setPadding(0, 10, 0, 10)
            val textViewDate = TextView(this)
            textViewDate.text = theList[i].date_of_sync
            textViewDate.setPadding(10, 0, 10, 0)
            textViewDate.setTextColor(resources.getColor(R.color.purple_500))
            textViewDate.textSize = 16F
            textViewDate.textAlignment = View.TEXT_ALIGNMENT_CENTER
            if (i == 0) {
                textViewDate.setTypeface(null, Typeface.BOLD)
            }
            val textViewPos = TextView(this)
            textViewPos.text = theList[i].position.toString()
            textViewPos.setPadding(10, 0, 10, 0)
            textViewPos.setTextColor(resources.getColor(R.color.purple_500))
            textViewPos.textSize = 16F
            textViewPos.textAlignment = View.TEXT_ALIGNMENT_CENTER
            if (i == 0) {
                textViewPos.setTypeface(null, Typeface.BOLD)
            }
            row.addView(textViewDate)
            row.addView(textViewPos)

            if (i % 2 == 0) {
                row.setBackgroundColor(resources.getColor(R.color.table_even))
            } else {
                row.setBackgroundColor(resources.getColor(R.color.table_odd))
            }
            table.addView(row, i + 1)
        }
    }

    private fun prepareChart(theList: List<Stat>) {
        val xvalue = ArrayList<String>()
        val lineentry = ArrayList<Entry>();
        for(i in theList.lastIndex downTo 0){
            xvalue.add(theList[i].date_of_sync.toString())
            lineentry.add(Entry(theList[i].position.toFloat(), theList.lastIndex-i))
        }
        val lineDataSet = LineDataSet(lineentry, "$Id")
        lineDataSet.color = resources.getColor(R.color.purple_500)
        val data = LineData(xvalue, lineDataSet)
        val ch:LineChart = findViewById(R.id.chart)
        ch.data = data
        ch.legend.isEnabled = false
        ch.getAxis(YAxis.AxisDependency.LEFT).isInverted = true
        ch.getAxis(YAxis.AxisDependency.RIGHT).isInverted = true
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

    private fun setCaptions(rec: Record) {
        val statYear: TextView = findViewById(R.id.statYear)
        statYear.text = "Pub: "+ rec.year_pub.toString()
        val statID: TextView = findViewById(R.id.statId)
        statID.text = rec.id.toString()
        val statName: TextView = findViewById(R.id.statName)
        statName.text = stringCutter(rec.title!!)
        val im: ImageView = findViewById(R.id.statPic)
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        var image: Bitmap? = null
        executor.execute {
            val imageURL = rec.pic
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
    }


}