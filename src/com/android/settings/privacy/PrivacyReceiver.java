package com.android.settings.privacy;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.ServiceManager;
import android.privacy.PrivacySettingsManager;
import android.privacy.utilities.PrivacyConstants;
import android.privacy.utilities.PrivacyDebugger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Provides Kill-Task-Handling
 * @author CollegeDev (Stefan T.)
 */
public class PrivacyReceiver extends BroadcastReceiver{

	private static final String TAG = "PrivacyReceiver|Tasks";
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equals(PrivacySettingsManager.ACTION_KILL_TASKS)){

			final int random = new Random().nextInt();
			final long random1 = new Random().nextLong();
			final int UID_INTENT = intent.getIntExtra(PrivacyConstants.TaskKiller.EXTRA_UID, random);
			final long unique_id = intent.getLongExtra(PrivacyConstants.CallerRegistry.EXTRA_UNIQUE_DATA_ACCESS_ID, random1);
			PrivacyDebugger.i(TAG,"got intent for killing packages. UID_INTENT: " + UID_INTENT);
			if(UID_INTENT == random) {
				PrivacyDebugger.e(TAG,"error while parsing UID -> return");
				return;
			}
			if(unique_id == random1) {
				PrivacyDebugger.e(TAG,"error while parsing Unique-UID -> return");
				return;
			}
			
			PrivacySettingsManager pSetMan = (PrivacySettingsManager) context.getSystemService("privacy");

			long UID_CALLER;
			try{
				UID_CALLER = pSetMan.getLastCallerId(unique_id);
			} catch (SecurityException e){
				PrivacyDebugger.e(TAG,"Nice try...bye!");
				return;
			}

			IPackageManager mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
			String[] expectedPackageName = null;

			try {
				expectedPackageName = mPm.getPackagesForUid(UID_INTENT);
			} catch (RemoteException e) {
				e.printStackTrace();
				return;
			}
			
			if(expectedPackageName == null || expectedPackageName.length == 0 || !expectedPackageName[0].equals("com.android.privacy.pdroid20")) { //add your manager here
				PrivacyDebugger.e(TAG,"Nice try...bye!");
				return;
			}
			
			if(UID_INTENT != UID_CALLER){
				PrivacyDebugger.e(TAG,"Nice try...bye!");
				return;
			}
			
			final String[] names = intent.getStringArrayExtra(PrivacyConstants.TaskKiller.EXTRA_PACKAGES);
			if(names == null){
				PrivacyDebugger.e(TAG,"No packages to kill -> return");
				return;
			}
			ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
			for(String app : names){
				try{
					am.forceStopPackage(app);
				} catch (Exception e){
					e.printStackTrace();
					PrivacyDebugger.e(TAG,"Got error while trying to kill package: " + app);
				}
			}
		} else if(intent.getAction().equals(PrivacySettingsManager.ACTION_DISABLE_ENABLE_APPLICATION)) {
			
			final int random = new Random().nextInt();
			final long random1 = new Random().nextLong();
			final int UID_INTENT = intent.getIntExtra(PrivacyConstants.AppDisabler.EXTRA_UID, random);
			final long unique_id = intent.getLongExtra(PrivacyConstants.CallerRegistry.EXTRA_UNIQUE_DATA_ACCESS_ID, random1);
			PrivacyDebugger.i(TAG,"got intent for disabling/enabling application. UID_INTENT: " + UID_INTENT);
			if(UID_INTENT == random) {
				PrivacyDebugger.e(TAG,"error while parsing UID -> return");
				return;
			}
			if(unique_id == random1) {
				PrivacyDebugger.e(TAG,"error while parsing Unique-UID -> return");
				return;
			}
			
			PrivacySettingsManager pSetMan = (PrivacySettingsManager) context.getSystemService("privacy");

			long UID_CALLER;
			try{
				UID_CALLER = pSetMan.getLastCallerId(unique_id);
			} catch (SecurityException e){
				PrivacyDebugger.e(TAG,"Nice try...bye!");
				return;
			}

			IPackageManager mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
			String[] expectedPackageName = null;

			try {
				expectedPackageName = mPm.getPackagesForUid(UID_INTENT);
			} catch (RemoteException e) {
				e.printStackTrace();
				return;
			}
			
			if(expectedPackageName == null || expectedPackageName.length == 0 || !expectedPackageName[0].equals("com.android.privacy.pdroid20")) { //add your manager here
				PrivacyDebugger.e(TAG,"Nice try...bye!");
				return;
			}
			
			if(UID_INTENT != UID_CALLER){
				PrivacyDebugger.e(TAG,"Nice try...bye!");
				return;
			}
			
			final String packageName = intent.getStringExtra(PrivacyConstants.AppDisabler.EXTRA_PACKAGE);
			
			if(packageName == null){
				PrivacyDebugger.e(TAG,"No package to disable -> return");
				return;
			}
			
			final boolean disable = intent.getBooleanExtra(PrivacyConstants.AppDisabler.EXTRA_DISABLE_OR_ENABLE, false);
			
			//if disable == true, disable, otherwise enable the application
			if(isThisASystemPackageOrLauncher(packageName, context)) {
				// Try to prevent the user from bricking their phone
	            // by not allowing disabling of apps signed with the
	            // system cert and any launcher app in the system
				PrivacyDebugger.e(TAG,"OnReceive - can't disable core apps -> return");
				return;
			}
			
			if(disable) {
				PrivacyDebugger.i(TAG, "onReceive - now going to disable application: " + packageName);
				new DisableChanger(context.getPackageManager(), packageName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, context).execute((Object)null);
			} else {
				PrivacyDebugger.i(TAG, "onReceive - now going to enable application: " + packageName);
				new DisableChanger(context.getPackageManager(), packageName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, context).execute((Object)null);
			}
			
		} else {
			PrivacyDebugger.w(TAG,"received Intent, but not our");
		}
		
	}
	
	/**
	 * helper class for disabling/enabling applications
	 * @author CollegeDev
	 */
	private static class DisableChanger extends AsyncTask<Object, Object, Object> {
        final PackageManager mPm;
        final String packageName;
        final int mState;
        final Context context;

        DisableChanger(PackageManager packageManager, String packageName, int state, Context context) {
            this.mPm = packageManager;
            this.packageName = packageName;
            this.mState = state;
            this.context = context;
        }

        @Override
        protected Object doInBackground(Object... params) {
        	//first trying to disable components
        	List<ComponentName> components = getAllComponents(packageName, context);
        	for(ComponentName component : components) {
        		try {
        			PrivacyDebugger.i(TAG,"DisableChanger - component: " + component.getClassName() + " in progress");
        			mPm.setComponentEnabledSetting(component, mState, PackageManager.DONT_KILL_APP);
        		} catch(Exception e) {
        		    //nothing here   
        		}
        	}
        	//now going to disable/enable all, just to be sure!
        	PrivacyDebugger.i(TAG, "DisableChanger - now make sure and call setApplicationEnabledSetting");
            mPm.setApplicationEnabledSetting(packageName, mState, 0);
            return null;
        }
    }
	
	/**
	 * 
	 * @param packageName
	 * @param context
	 * @return
	 */
	private boolean isThisASystemPackageOrLauncher(String packageName, Context context) {
        try {
        	boolean output = false;
        	PackageManager mPm = context.getPackageManager();
        	Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setPackage(packageName);
            List<ResolveInfo> homes = mPm.queryIntentActivities(intent, 0);
            PackageInfo sys = mPm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
            PackageInfo currentPackage = mPm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            if(currentPackage != null && currentPackage.signatures != null && sys.signatures[0].equals(currentPackage.signatures[0]))
            	output = true;
            if((homes != null && homes.size() > 0))
            	output = true;
            PrivacyDebugger.i(TAG,"isThisASystemPackageOrLauncher - return value: " + output);
            return output;
            
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
	
	private static List<ComponentName> getAllComponents(String packageName, Context context) {
		List<ComponentName> output = new ArrayList<ComponentName>();
		PackageManager pm = context.getPackageManager();
		try {
			PackageInfo pInfo = pm.getPackageInfo(packageName, 	PackageManager.GET_ACTIVITIES | 
																PackageManager.GET_RECEIVERS | 
																PackageManager.GET_PROVIDERS | 
																PackageManager.GET_SERVICES );
//			ActivityInfo[] aInfo = pInfo.activities;
//			if(aInfo != null) {
//				PrivacyDebugger.i(TAG, "getAllComponents - now adding activity components");
//				for(ActivityInfo info : aInfo) {
//					PrivacyDebugger.i(TAG, "getAllComponents - component name: " + info.name);
//					if(info.name.contains(packageName)) {
//						output.add(new ComponentName(packageName, info.name));
//						PrivacyDebugger.i(TAG,"getAllComponents - (1) added component name: " + info.name);
//					} else if(info.name.startsWith(".") && !info.name.contains(packageName)) {
//						output.add(new ComponentName(packageName, packageName + info.name));
//						PrivacyDebugger.i(TAG,"getAllComponents - (2) added component name: " + packageName + info.name);
//					} else if(!info.name.startsWith(".") && !info.name.contains(packageName)) {
//						output.add(new ComponentName(packageName, info.name));
//						PrivacyDebugger.i(TAG,"getAllComponents - (3) added component name: " + info.name);
//					} else {
//						PrivacyDebugger.i(TAG, "getAllComponents - can't detect pattern. Name: " + info.name);
//					}
//				}
//			} else {
//			    PrivacyDebugger.w(TAG,"getAllComponents - can't find any activity component for package: " + packageName);
//			}
			ActivityInfo[] rInfo = pInfo.receivers;
			if(rInfo != null) {
				PrivacyDebugger.i(TAG,"getAllComponents - now adding receiver components");
				for(ActivityInfo info : rInfo) {
					PrivacyDebugger.i(TAG, "getAllComponents - component name: " + info.name);
					if(info.name.contains(packageName)) {
						output.add(new ComponentName(packageName, info.name));
						PrivacyDebugger.i(TAG,"getAllComponents - (1) added component name: " + info.name);
					} else if(info.name.startsWith(".") && !info.name.contains(packageName)) {
						output.add(new ComponentName(packageName, packageName + info.name));
						PrivacyDebugger.i(TAG,"getAllComponents - (2) added component name: " + packageName + info.name);
					} else if(!info.name.startsWith(".") && !info.name.contains(packageName)) {
						output.add(new ComponentName(packageName, info.name));
						PrivacyDebugger.i(TAG,"getAllComponents - (3) added component name: " + info.name);
					} else {
						PrivacyDebugger.i(TAG, "getAllComponents - can't detect pattern. Name: " + info.name);
					}
				}
			} else {
				PrivacyDebugger.w(TAG,"getAllComponents - can't find any receiver component for package: " + packageName);
			}
			ProviderInfo[] mpInfo = pInfo.providers;
			if(mpInfo != null) {
				PrivacyDebugger.i(TAG,"getAllComponents - now adding provider components");
				for(ProviderInfo info : mpInfo) {
					PrivacyDebugger.i(TAG, "getAllComponents - component name: " + info.name);
					if(info.name.contains(packageName)) {
						output.add(new ComponentName(packageName, info.name));
						PrivacyDebugger.i(TAG,"getAllComponents - (1) added component name: " + info.name);
					} else if(info.name.startsWith(".") && !info.name.contains(packageName)) {
						output.add(new ComponentName(packageName, packageName + info.name));
						PrivacyDebugger.i(TAG,"getAllComponents - (2) added component name: " + packageName + info.name);
					} else if(!info.name.startsWith(".") && !info.name.contains(packageName)) {
						output.add(new ComponentName(packageName, info.name));
						PrivacyDebugger.i(TAG,"getAllComponents - (3) added component name: " + info.name);
					} else {
						PrivacyDebugger.i(TAG, "getAllComponents - can't detect pattern. Name: " + info.name);
					}
				}
			} else {
				PrivacyDebugger.w(TAG,"getAllComponents - can't find any provider component for package: " + packageName);
			}
			ServiceInfo[] sInfo = pInfo.services;
			if(sInfo != null) {
				PrivacyDebugger.i(TAG,"getAllComponents - now adding service components");
				for(ServiceInfo info : sInfo) {
					PrivacyDebugger.i(TAG, "getAllComponents - component name: " + info.name);
					if(info.name.contains(packageName)) {
						output.add(new ComponentName(packageName, info.name));
						PrivacyDebugger.i(TAG,"getAllComponents - (1) added component name: " + info.name);
					} else if(info.name.startsWith(".") && !info.name.contains(packageName)) {
						output.add(new ComponentName(packageName, packageName + info.name));
						PrivacyDebugger.i(TAG,"getAllComponents - (2) added component name: " + packageName + info.name);
					} else if(!info.name.startsWith(".") && !info.name.contains(packageName)) {
						output.add(new ComponentName(packageName, info.name));
						PrivacyDebugger.i(TAG,"getAllComponents - (3) added component name: " + info.name);
					} else {
						PrivacyDebugger.i(TAG, "getAllComponents - can't detect pattern. Name: " + info.name);
					}
				}
			} else {
				PrivacyDebugger.w(TAG,"getAllComponents - can't find any service component for package: " + packageName);
			}
			PrivacyDebugger.i(TAG, "getAllComponents - size of list before returning: " + output.size());
			
		} catch (NameNotFoundException e) {
			PrivacyDebugger.e(TAG, "getAllComponents - name of app not found", e);
		} catch (Exception e) {
			// nothing here
		}
		return output;
	}
} 
