// 
// MobeelizerApplication.java
// 
// Copyright (C) 2012 Mobeelizer Ltd. All Rights Reserved.
//
// Mobeelizer SDK is free software; you can redistribute it and/or modify it 
// under the terms of the GNU Affero General Public License as published by 
// the Free Software Foundation; either version 3 of the License, or (at your
// option) any later version.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
// FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
// for more details.
//
// You should have received a copy of the GNU Affero General Public License 
// along with this program; if not, write to the Free Software Foundation, Inc., 
// 51 Franklin St, Fifth Floor, Boston, MA  02110-1301 USA
// 

package com.mobeelizer.mobile.android;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;
import java.io.File;
import java.io.FileOutputStream;

import java.io.RandomAccessFile;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mobeelizer.java.api.MobeelizerMode;
import com.mobeelizer.java.api.MobeelizerModel;
import com.mobeelizer.java.api.MobeelizerOperationError;
import com.mobeelizer.java.definition.MobeelizerApplicationDefinition;
import com.mobeelizer.java.definition.MobeelizerDefinitionConverter;
import com.mobeelizer.java.definition.MobeelizerDefinitionParser;
import com.mobeelizer.java.errors.MobeelizerOperationErrorImpl;
import com.mobeelizer.java.model.MobeelizerModelImpl;
import com.mobeelizer.mobile.android.api.MobeelizerOperationCallback;
import com.mobeelizer.mobile.android.api.MobeelizerSyncListener;
import com.mobeelizer.mobile.android.api.MobeelizerSyncStatus;
import com.mobeelizer.mobile.android.model.MobeelizerAndroidModel;

public class MobeelizerApplication {

    private static final String TAG = "mobeelizer";

    private static final String META_DEVICE = "MOBEELIZER_DEVICE";

    private static final String META_URL = "MOBEELIZER_URL";

    private static final String META_PACKAGE = "MOBEELIZER_PACKAGE";

    private static final String META_DEFINITION_ASSET = "MOBEELIZER_DEFINITION_ASSET";

    private static final String META_DATABASE_VERSION = "MOBEELIZER_DB_VERSION";

    private static final String META_MODE = "MOBEELIZER_MODE";

    private static final String META_DEVELOPMENT_ROLE = "MOBEELIZER_DEVELOPMENT_ROLE";
    
    private static final String DEVICE_ID_FILE = "DEVICE_IDENTIFIER";

    private String vendor;

    private String application;

    private String versionDigest;

    private String device;

    private String deviceIdentifier;

    private String entityPackage;

    private Application mobeelizer;

    private int databaseVersion;

    private MobeelizerMode mode;

    private String url;

    private String instance;

    private String user;

    private String group;

    private String role;

    private String instanceGuid;

    private String password;

    private boolean loggedIn = false;

    private String remoteNotificationToken;

    private MobeelizerDatabaseImpl database;

    private MobeelizerInternalDatabase internalDatabase;

    private MobeelizerApplicationDefinition definition;

    private final MobeelizerDefinitionConverter definitionConverter = new MobeelizerDefinitionConverter();

    private MobeelizerConnectionManager connectionManager;

    private MobeelizerFileService fileService;

    private MobeelizerSyncStatus syncStatus = MobeelizerSyncStatus.NONE;

    private final List<MobeelizerSyncListener> syncListeners = new LinkedList<MobeelizerSyncListener>();

    private MobeelizerApplication() {
    }

    public static MobeelizerApplication createApplication(final Application mobeelizer) {
        MobeelizerApplication application = new MobeelizerApplication();
        Bundle metaData = application.getMetaData(mobeelizer);
        String device = metaData.getString(META_DEVICE);
        String entityPackage = metaData.getString(META_PACKAGE);
        String definitionXml = metaData.getString(META_DEFINITION_ASSET);
        String developmentRole = metaData.getString(META_DEVELOPMENT_ROLE);
        int databaseVersion = metaData.getInt(META_DATABASE_VERSION, 1);
        String url = metaData.getString(META_URL);
        String stringMode = metaData.getString(META_MODE);

        if (entityPackage == null) {
            throw new IllegalStateException(META_PACKAGE + " must be set in manifest file.");
        }

        if (definitionXml == null) {
            definitionXml = "application.xml";
        }

        application.initApplication(mobeelizer, device, entityPackage, developmentRole, definitionXml, databaseVersion, url,
                stringMode);

        return application;
    }

    public static MobeelizerApplication createApplicationForTitanium(final Application mobeelizer) {
        MobeelizerApplication application = new MobeelizerApplication();

        Properties properties = new Properties();
        try {
            properties.load(application.getDefinitionXmlAsset(mobeelizer, "Resources/mobeelizer.properties"));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("'mobeelizer.properties' file is required", e);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        String device = properties.getProperty("device");
        String developmentRole = properties.getProperty("role");
        String definitionXml = properties.getProperty("definitionXml");
        String url = properties.getProperty("url");
        String stringMode = properties.getProperty("mode");

        if (definitionXml == null) {
            definitionXml = "application.xml";
        }
        definitionXml = "Resources/" + definitionXml;

        application.initApplication(mobeelizer, device, null, developmentRole, definitionXml, 1, url, stringMode);

        return application;
    }

    private void initApplication(final Application mobeelizer, final String device, final String entityPackage,
            final String developmentRole, final String definitionXml, final int databaseVersion, final String url,
            final String stringMode) {
        Log.i(TAG, "Creating Mobeelizer SDK " + Mobeelizer.VERSION);

        this.mobeelizer = mobeelizer;
        Mobeelizer.setInstance(this);

        String state = Environment.getExternalStorageState();

        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                throw new IllegalStateException("External storage must be available and read-only.");
            } else {
                throw new IllegalStateException("External storage must be available.");
            }
        }

        this.device = device;
        this.entityPackage = entityPackage;
        this.databaseVersion = databaseVersion;
        this.url = url;

        if (device == null) {
            throw new IllegalStateException(META_DEVICE + " must be set in manifest file.");
        }

        if (stringMode == null) {
            mode = MobeelizerMode.DEVELOPMENT;
        } else {
            mode = MobeelizerMode.valueOf(stringMode.toUpperCase(Locale.ENGLISH));
        }

        if (mode == MobeelizerMode.DEVELOPMENT && developmentRole == null) {
            throw new IllegalStateException(META_DEVELOPMENT_ROLE + " must be set in development MobeelizerMode.");
        }

        deviceIdentifier = ((TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();

        if (deviceIdentifier == null) {
        	deviceIdentifier = getUUIDAsDeviceId();
        }
        
        if (deviceIdentifier == null) {
            throw new IllegalStateException("Could to resolve device identifier.");
        }

        if (mode == MobeelizerMode.DEVELOPMENT) {
            connectionManager = new MobeelizerDevelopmentConnectionManager(developmentRole);
        } else {
            connectionManager = new MobeelizerRealConnectionManager(this);
        }

        try {
            definition = MobeelizerDefinitionParser.parse(getDefinitionXmlAsset(mobeelizer, definitionXml));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read definition from " + definitionXml + ".", e);
        }

        vendor = definition.getVendor();
        application = definition.getApplication();
        versionDigest = definition.getDigest();

        internalDatabase = new MobeelizerInternalDatabase(this);

        fileService = new MobeelizerFileService(this);
    }

    public void login(final String user, final String password, final MobeelizerOperationCallback callback) {
        login(mode == MobeelizerMode.PRODUCTION ? "production" : "test", user, password, callback);
    }

    public MobeelizerOperationError login(final String user, final String password) {
        return login(mode == MobeelizerMode.PRODUCTION ? "production" : "test", user, password);
    }

    public void login(final String instance, final String user, final String password, final MobeelizerOperationCallback callback) {
        new AsyncTask<Void, Void, MobeelizerOperationError>() {

            @Override
            protected MobeelizerOperationError doInBackground(final Void... params) {
                return login(instance, user, password);
            }

            @Override
            protected void onPostExecute(final MobeelizerOperationError error) {
                super.onPostExecute(error);
                if (error == null) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(error);
                }
            }

        }.execute();
    }

    public MobeelizerOperationError login(final String instance, final String user, final String password) {
        if (isLoggedIn()) {
            logout();
        }

        Log.i(TAG, "login: " + vendor + ", " + application + ", " + instance + ", " + user + ", " + password);

        this.instance = instance;
        this.user = user;
        this.password = password;

        MobeelizerLoginResponse status = connectionManager.login();

        Log.i(TAG, "Login result: " + status.getError() + ", " + status.getRole() + ", " + status.getInstanceGuid());

        if (status.getError() != null) {
            this.instance = null;
            this.user = null;
            this.password = null;
            return status.getError();
        }

        role = status.getRole();
        instanceGuid = status.getInstanceGuid();
        group = role.split("-")[0];

        loggedIn = true;

        Set<MobeelizerAndroidModel> androidModels = new HashSet<MobeelizerAndroidModel>();

        for (MobeelizerModel model : definitionConverter.convert(definition, entityPackage, role)) {
            androidModels.add(new MobeelizerAndroidModel((MobeelizerModelImpl) model, user, group));
        }

        database = new MobeelizerDatabaseImpl(this, androidModels);
        database.open();

        if (status.isInitialSyncRequired()) {
            sync(true);
        }

        return null;
    }

    public void logout() {
        if (!isLoggedIn()) {
            return; // ignore
        }

        if (checkSyncStatus().isRunning()) {
            throw new IllegalStateException("Cannot logout when sync is in progress.");
        }

        Log.i(TAG, "logout");

        this.instance = null;
        this.user = null;
        this.password = null;

        if (database != null) {
            database.close();
            database = null;
        }

        loggedIn = false;
    }

    public void sync(final MobeelizerOperationCallback callback) {
        Log.i(TAG, "Start sync service.");
        sync(false, callback);
    }

    public MobeelizerOperationError sync() {
        if (!isLoggedIn()) {
            return MobeelizerOperationErrorImpl.notLoggedError();
        }
        Log.i(TAG, "Truncate data and start sync service.");
        return sync(false);
    }

    public void syncAll(final MobeelizerOperationCallback callback) {
        Log.i(TAG, "Truncate data and start sync service.");
        sync(true, callback);
    }

    public MobeelizerOperationError syncAll() {
        if (!isLoggedIn()) {
            return MobeelizerOperationErrorImpl.notLoggedError();
        }
        Log.i(TAG, "Truncate data and start sync service.");
        return sync(true);
    }

    private MobeelizerOperationError sync(final boolean syncAll) {
        if (mode == MobeelizerMode.DEVELOPMENT || checkSyncStatus().isRunning()) {
            Log.w(TAG, "Sync is already running - skipping.");
            return null;
        }
        if (!connectionManager.isNetworkAvailable()) {
            Log.w(TAG, "Sync cannot be performed - network is not available.");
            setSyncStatus(MobeelizerSyncStatus.FINISHED_WITH_FAILURE);
            return MobeelizerOperationErrorImpl.missingConnectionError();
        }
        setSyncStatus(MobeelizerSyncStatus.STARTED);
        return new MobeelizerSyncServicePerformer(Mobeelizer.getInstance(), syncAll).sync();
    }

    private void sync(final boolean syncAll, final MobeelizerOperationCallback callback) {
        new AsyncTask<Void, Void, MobeelizerOperationError>() {

            @Override
            protected MobeelizerOperationError doInBackground(final Void... params) {
                if (!isLoggedIn()) {
                    return MobeelizerOperationErrorImpl.notLoggedError();
                }
                return sync(syncAll);
            }

            @Override
            protected void onPostExecute(final MobeelizerOperationError error) {
                super.onPostExecute(error);
                if (error == null) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(error);
                }
            }

        }.execute();
    }

    MobeelizerConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public MobeelizerSyncStatus checkSyncStatus() {
        Log.i(TAG, "Check sync status.");
        if (mode == MobeelizerMode.DEVELOPMENT) {
            return MobeelizerSyncStatus.NONE;
        }
        return syncStatus;
    }

    public void registerSyncListener(final MobeelizerSyncListener listener) {
        syncListeners.add(listener);
    }

    void setSyncStatus(final MobeelizerSyncStatus status) {
        this.syncStatus = status;
        for (MobeelizerSyncListener listener : syncListeners) {
            listener.onSyncStatusChange(status);
        }
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public MobeelizerOperationError registerForRemoteNotifications(final String registrationId) {
        remoteNotificationToken = registrationId;
        if (isLoggedIn()) {
            return connectionManager.registerForRemoteNotifications(registrationId);
        }
        return null;
    }

    public MobeelizerOperationError unregisterForRemoteNotifications() {
        if (!isLoggedIn()) {
            return MobeelizerOperationErrorImpl.notLoggedError();
        }
        return connectionManager.unregisterForRemoteNotifications(remoteNotificationToken);
    }

    public MobeelizerOperationError sendRemoteNotification(final String device, final String group, final List<String> users,
            final Map<String, String> notification) {
        if (!isLoggedIn()) {
            return MobeelizerOperationErrorImpl.notLoggedError();
        }
        return connectionManager.sendRemoteNotification(device, group, users, notification);
    }

    int getDatabaseVersion() {
        return databaseVersion;
    }

    String getUser() {
        return user;
    }

    String getGroup() {
        return group;
    }

    String getInstance() {
        return instance;
    }

    public MobeelizerDatabaseImpl getDatabase() {
        if (!isLoggedIn()) {
            throw new IllegalStateException("User is not logged in");
        }
        return database;
    }

    MobeelizerInternalDatabase getInternalDatabase() {
        return internalDatabase;
    }

    String getVendor() {
        return vendor;
    }

    String getApplication() {
        return application;
    }

    String getVersionDigest() {
        return versionDigest;
    }

    Context getContext() {
        return mobeelizer;
    }

    String getPassword() {
        return password;
    }

    String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    String getDevice() {
        return device;
    }

    String getUrl() {
        return url;
    }

    MobeelizerMode getMode() {
        return mode;
    }

    String getInstanceGuid() {
        return instanceGuid;
    }

    String getRemoteNotificationToken() {
        return remoteNotificationToken;
    }

    MobeelizerFileService getFileService() {
        return fileService;
    }

    MobeelizerApplicationDefinition getDefinition() {
        return definition;
    }

    private Bundle getMetaData(final Application mobeelizer) {
        try {
            return mobeelizer.getPackageManager().getApplicationInfo(mobeelizer.getPackageName(), PackageManager.GET_META_DATA).metaData;
        } catch (NameNotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private InputStream getDefinitionXmlAsset(final Application mobeelizer, final String definitionXml) throws IOException {
        return mobeelizer.getAssets().open(definitionXml);
    }
    
    private String getUUIDAsDeviceId(){
    	String sID = null;
    	File device_id_file = new File(getContext().getFilesDir(), DEVICE_ID_FILE);
        try {
            if (!device_id_file.exists())
                writeInstallationFile(device_id_file);
            sID = readInstallationFile(device_id_file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    	return sID;
    }
    
    private String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    private void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }
}
