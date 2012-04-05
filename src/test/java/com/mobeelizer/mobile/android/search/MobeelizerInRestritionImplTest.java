// 
// MobeelizerInRestritionImplTest.java
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

package com.mobeelizer.mobile.android.search;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MobeelizerInRestritionImplTest {

    @Test
    public void shouldAddToQuery() throws Exception {
        // given
        List<Object> values = new ArrayList<Object>();
        values.add(1);
        values.add("ala");

        MobeelizerInRestritionImpl restrition = new MobeelizerInRestritionImpl("field", values);
        List<String> selectionArgs = new ArrayList<String>();

        // when
        String query = restrition.addToQuery(selectionArgs);

        // then
        assertEquals(1, selectionArgs.size());
        assertEquals("1, ala", selectionArgs.get(0));
        assertEquals("field in (?)", query);
    }

    @Test
    public void shouldAddToQueryWithEmpty() throws Exception {
        // given
        List<Object> values = new ArrayList<Object>();

        MobeelizerInRestritionImpl restrition = new MobeelizerInRestritionImpl("field", values);
        List<String> selectionArgs = new ArrayList<String>();

        // when
        String query = restrition.addToQuery(selectionArgs);

        // then
        assertEquals(0, selectionArgs.size());
        assertEquals("1 = 1", query);
    }

}