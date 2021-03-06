// 
// MobeelizerDevelopmentConnectionManager.java
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

import java.io.File;
import java.util.List;
import java.util.Map;

import com.mobeelizer.java.api.MobeelizerOperationError;
import com.mobeelizer.java.errors.MobeelizerOperationStatus;

class MobeelizerDevelopmentConnectionManager implements MobeelizerConnectionManager {

    private static final String SYNC_IS_NOT_SUPPORTED_IN_DEVELOPMENT_MODE = "Sync is not supported in development mode.";

    private static final String PUSH_IS_NOT_SUPPORTED_IN_DEVELOPMENT_MODE = "Push is not supported in development mode.";

    private final String developmentRole;

    public MobeelizerDevelopmentConnectionManager(final String developmentRole) {
        this.developmentRole = developmentRole;
    }

    @Override
    public boolean isNetworkAvailable() {
        return false;
    }

    @Override
    public MobeelizerLoginResponse login() {
        return new MobeelizerLoginResponse(null, "00000000-0000-0000-0000-000000000000", developmentRole, false);
    }

    @Override
    public MobeelizerOperationStatus<String> sendSyncAllRequest() {
        throw new UnsupportedOperationException(SYNC_IS_NOT_SUPPORTED_IN_DEVELOPMENT_MODE);
    }

    @Override
    public MobeelizerOperationStatus<String> sendSyncDiffRequest(final File outputFile) {
        throw new UnsupportedOperationException(SYNC_IS_NOT_SUPPORTED_IN_DEVELOPMENT_MODE);
    }

    @Override
    public MobeelizerOperationError waitUntilSyncRequestComplete(final String ticket) {
        throw new UnsupportedOperationException(SYNC_IS_NOT_SUPPORTED_IN_DEVELOPMENT_MODE);
    }

    @Override
    public File getSyncData(final String ticket) {
        throw new UnsupportedOperationException(SYNC_IS_NOT_SUPPORTED_IN_DEVELOPMENT_MODE);
    }

    @Override
    public MobeelizerOperationError confirmTask(final String ticket) {
        throw new UnsupportedOperationException(SYNC_IS_NOT_SUPPORTED_IN_DEVELOPMENT_MODE);
    }

    @Override
    public MobeelizerOperationError registerForRemoteNotifications(final String registrationId) {
        throw new UnsupportedOperationException(PUSH_IS_NOT_SUPPORTED_IN_DEVELOPMENT_MODE);
    }

    @Override
    public MobeelizerOperationError unregisterForRemoteNotifications(final String registrationId) {
        throw new UnsupportedOperationException(PUSH_IS_NOT_SUPPORTED_IN_DEVELOPMENT_MODE);
    }

    @Override
    public MobeelizerOperationError sendRemoteNotification(final String device, final String group, final List<String> users,
            final Map<String, String> notification) {
        throw new UnsupportedOperationException(PUSH_IS_NOT_SUPPORTED_IN_DEVELOPMENT_MODE);
    }

}
