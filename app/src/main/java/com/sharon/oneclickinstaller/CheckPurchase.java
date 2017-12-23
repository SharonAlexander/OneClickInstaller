package com.sharon.oneclickinstaller;

import android.content.Context;
import android.util.Log;

import com.sharon.oneclickinstaller.util.IabHelper;
import com.sharon.oneclickinstaller.util.IabResult;
import com.sharon.oneclickinstaller.util.Inventory;
import com.sharon.oneclickinstaller.util.Purchase;

public class CheckPurchase {

    private static final String ITEM_SKU_SMALL = "com.sharon.donate_small";
    public static boolean isPremium = false;
    private static IabHelper mHelper;
    private static PrefManager prefManager;
    private static IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (mHelper == null) return;
            if (result.isFailure()) {
            } else {
                Purchase premiumPurchase = inventory.getPurchase(ITEM_SKU_SMALL);
                if (inventory.hasPurchase(ITEM_SKU_SMALL)) {
                    boolean pre = true;
                }
                boolean premium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
                prefManager.putPremiumInfo(premium);
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

    private static boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        return true;
    }

    public static void dispose() {
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }
}
