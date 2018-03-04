package net.dublin.bus.ui.view.favourite

import android.graphics.drawable.AnimationDrawable
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.item_list_favorite.view.*
import kotlinx.android.synthetic.main.item_list_real_time.view.*
import net.dublin.bus.R
import net.dublin.bus.data.realtime.repository.RealTimeRepository
import net.dublin.bus.model.Favourite
import net.dublin.bus.model.StopData
import net.dublin.bus.ui.utilities.inflate
import java.util.*

internal class FavouriteAdapter(private val mListener: ItemClickListener) : RecyclerView.Adapter<FavouriteAdapter.LocalViewHolder>() {
    private val mDataSet: MutableList<Favourite> = ArrayList()

    internal interface ItemClickListener {
        fun onItemClick(item: Favourite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalViewHolder {
        return LocalViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: LocalViewHolder, position: Int) {
        holder.bind(mDataSet[position])
    }

    override fun getItemCount(): Int {
        return mDataSet.size
    }

    fun replaceData(dataSet: List<Favourite>) {
        setList(dataSet)
        notifyDataSetChanged()
    }

    private fun setList(dataSet: List<Favourite>) {
        mDataSet.clear()
        mDataSet.addAll(dataSet)
    }

    internal inner class LocalViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_list_favorite)) {
        private var mItem: Favourite? = null
        private var realTimes: ArrayList<RealTime> = arrayListOf()

        init {
            realTimes.add(realTime(itemView.findViewById<View>(R.id.favorite_real_time_1_view)))
            realTimes.add(realTime(itemView.findViewById<View>(R.id.favorite_real_time_2_view)))
            realTimes.add(realTime(itemView.findViewById<View>(R.id.favorite_real_time_3_view)))
        }

        private fun realTime(view: View): RealTime {
            return RealTime(
                    view,
                    view.findViewById(R.id.real_route_view),
                    view.findViewById(R.id.real_route_description_view),
                    view.findViewById(R.id.real_route_time_view),
                    view.findViewById(R.id.live_blip)
            )
        }

        fun bind(item: Favourite) = with(itemView) {
            mItem = item

            favorite_description_view.text = item.description
            favorite_description_aux_view.text = item.stopNumber
            itemView.setOnClickListener {
                mItem?.let { it1 -> mListener.onItemClick(it1) }
            }


            hideData()
            hideNoData()
            showProgress()
            val repository = RealTimeRepository()
            repository.getData(item.stopNumber)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ data ->
                        onNextData(data)
                    }, {
                        onError()
                    })

        }

        private fun onNextData(data: List<StopData>) {
            hideProgress()
            if (data.isNotEmpty() && data.filter { !TextUtils.isEmpty(it.destinationName) }.any()) {
                showData(data)
            } else {
                showNoData()
            }
        }

        private fun showData(data: List<StopData>) {
            for ((i, d) in data.withIndex()) {
                realTimes[i].container.visibility = View.VISIBLE
                realTimes[i].description.text = d.destinationName
                realTimes[i].route.text = d.publishedLineName
                realTimes[i].time.text = d.timeRemainingFormatted()

                realTimes[i].liveBlip.setBackgroundResource(R.drawable.live)
                val progressAnimation = realTimes[i].liveBlip.background as AnimationDrawable?
                progressAnimation?.start()

                if (i + 1 == 3) {
                    return
                }
            }
        }

        private fun hideData() {
            for (realTime in realTimes) {
                realTime.container.visibility = View.GONE
            }
        }

        private fun hideProgress() {
            itemView.real_progress_bar.visibility = View.GONE
        }

        private fun showProgress() {
            itemView.real_progress_bar.visibility = View.VISIBLE
        }

        private fun onError() {
            hideProgress()
            showNoData()
        }

        private fun showNoData() {
            itemView.real_message.visibility = View.VISIBLE
        }

        private fun hideNoData() {
            itemView.real_message.visibility = View.GONE
        }
    }

    class RealTime(val container: View,
                   val route: TextView,
                   val description: TextView,
                   val time: TextView,
                   val liveBlip: ImageView)
}