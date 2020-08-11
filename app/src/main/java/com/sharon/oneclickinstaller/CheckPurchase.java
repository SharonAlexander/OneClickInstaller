package com.sharon.oneclickinstaller;

import android.content.Context;
import android.util.Log;

import com.sharon.oneclickinstaller.util.IabHelper;
import com.sharon.oneclickinstaller.util.IabResult;
import com.sharon.oneclickinstaller.util.Inventory;
import com.sharon.oneclickinstaller.util.Purchase;

import static com.sharon.oneclickinstaller.Constants.ITEM_SKU_SMALL;

public class CheckPurchase {

    public static final String TAG = "CheckPurchase";
    public static boolean isPremium = false;
    private static IabHelper mHelper;
    private static PrefManager prefManager;
    private static IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (mHelper == null) return;
            if (result.isFailure()) {
            } else {
                Purchase premiumPurchase = inventory.getPurchase(ITEM_SKU_SMALL);
                isPremium = premiumPurchase != null;
                Log.d(TAG, "isPremium: " + isPremium);
                prefManager.putPremiumInfo(isPremium); //always check this value before publishing.
            }
        }
    };

    static void checkpurchases(Context context) {
        prefManager = new PrefManager(context);
        String base64EncodedPublicKey = Constants.licensekey();
        mHelper = new IabHelper(context, base64EncodedPublicKey);
        mHelper.enableDebugLogging(true);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    return;
                }
                if (mHelper == null) return;
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void dispose() {
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }
}
