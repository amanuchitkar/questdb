/*******************************************************************************
 *    ___                  _   ____  ____
 *   / _ \ _   _  ___  ___| |_|  _ \| __ )
 *  | | | | | | |/ _ \/ __| __| | | |  _ \
 *  | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *   \__\_\\__,_|\___||___/\__|____/|____/
 *
 * Copyright (C) 2014-2017 Appsicle
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.questdb.net.ha.comsumer;

import com.questdb.net.ha.AbstractMutableObjectConsumer;
import com.questdb.net.ha.model.JournalClientState;

import java.nio.ByteBuffer;

public class JournalClientStateConsumer extends AbstractMutableObjectConsumer<JournalClientState> {

    @Override
    protected JournalClientState newInstance() {
        return new JournalClientState();
    }

    @Override
    protected void read(ByteBuffer buffer, JournalClientState status) {
        status.setJournalIndex(buffer.getInt());
        status.setTxn(buffer.getLong());
        status.setTxPin(buffer.getLong());
    }
}
