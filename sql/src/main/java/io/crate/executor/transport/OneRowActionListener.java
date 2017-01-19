/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.executor.transport;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import io.crate.operation.data.BatchConsumer;
import io.crate.operation.data.SingleRowCursor;
import org.elasticsearch.action.ActionListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OneRowActionListener<Response> implements ActionListener<Response>, FutureCallback<Response> {

    private final BatchConsumer rowReceiver;
    private final Function<? super Response, Object> toRowFunction;

    public OneRowActionListener(BatchConsumer rowReceiver, Function<? super Response, Object> toRowFunction) {
        this.rowReceiver = rowReceiver;
        this.toRowFunction = toRowFunction;
    }

    @Override
    public void onResponse(Response response) {
        Object row = toRowFunction.apply(response);
        rowReceiver.accept(SingleRowCursor.of(row), null);
    }

    @Override
    public void onSuccess(@Nullable Response result) {
        onResponse(result);
    }

    @Override
    public void onFailure(@Nonnull Throwable e) {
        rowReceiver.accept(null, e);
    }
}
