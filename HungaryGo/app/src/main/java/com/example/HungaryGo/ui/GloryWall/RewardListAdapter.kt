package com.example.HungaryGo.ui.GloryWall

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.example.HungaryGo.R

class RewardListAdapter(private val context: Context, private var bitmapList: MutableList<Bitmap>)
    : BaseAdapter() {

        fun updateData(newList: MutableMap<String?, android.graphics.Bitmap>)
        {
            bitmapList.clear()
            for ( (key, bitmap) in newList)
            {
                bitmapList.add(bitmap)
            }

            notifyDataSetChanged()
        }

    override fun getCount(): Int = (bitmapList.size + 1) / 2

    override fun getItem(position: Int): Pair<Bitmap, Bitmap?> {
        val first = bitmapList[position * 2]
        val second = if ((position * 2 + 1) < bitmapList.size) bitmapList[position * 2 + 1] else null
        return Pair(first, second)
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?):  View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_reward, parent, false)
        val reward1: ImageView = view.findViewById(R.id.reward1)
        val reward2: ImageView = view.findViewById(R.id.reward2)
        val (bitmap1, bitmap2) = getItem(position) ?: Pair(null, null)
        reward1.setImageBitmap(bitmap1)
        if(bitmap2 != null)
        {
            reward2.setImageBitmap(bitmap2)
        }
        else
        {
            reward2.visibility = View.INVISIBLE
        }

        return view
    }
}