package com.czy.timetable

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    private var rewardedAd: RewardedAd? = null
    private val AD_UNIT_ID = BuildConfig.ADMOB_REWARDED_UNIT_ID

    fun loadAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) { rewardedAd = null }
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
        })
    }

    fun showAd(activity: Activity) {
        if (rewardedAd != null) {
            rewardedAd?.show(activity) { _ ->
                Toast.makeText(activity, "感謝您的觀看與贊助！💖", Toast.LENGTH_LONG).show()
            }
            loadAd(activity) // 播完後立刻預載下一個
        } else {
            Toast.makeText(activity, "廣告還在準備中，請稍後再試！", Toast.LENGTH_SHORT).show()
            loadAd(activity)
        }
    }
}