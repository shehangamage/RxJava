/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.SequentialDisposable;

public final class CompletableResumeNext extends Completable {

    final CompletableSource source;

    final Function<? super Throwable, ? extends CompletableSource> errorMapper;

    public CompletableResumeNext(CompletableSource source,
            Function<? super Throwable, ? extends CompletableSource> errorMapper) {
        this.source = source;
        this.errorMapper = errorMapper;
    }



    @Override
    protected void subscribeActual(final CompletableObserver observer) {

        final SequentialDisposable sd = new SequentialDisposable();
        observer.onSubscribe(sd);
        source.subscribe(new ResumeNext(observer, sd));
    }

    final class ResumeNext implements CompletableObserver {

        final CompletableObserver downstream;
        final SequentialDisposable sd;

        ResumeNext(CompletableObserver observer, SequentialDisposable sd) {
            this.downstream = observer;
            this.sd = sd;
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            CompletableSource c;

            try {
                c = errorMapper.apply(e);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(new CompositeException(ex, e));
                return;
            }

            if (c == null) {
                NullPointerException npe = new NullPointerException("The CompletableConsumable returned is null");
                npe.initCause(e);
                downstream.onError(npe);
                return;
            }

            c.subscribe(new OnErrorObserver());
        }

        @Override
        public void onSubscribe(Disposable d) {
            sd.update(d);
        }

        final class OnErrorObserver implements CompletableObserver {

            @Override
            public void onComplete() {
                downstream.onComplete();
            }

            @Override
            public void onError(Throwable e) {
                downstream.onError(e);
            }

            @Override
            public void onSubscribe(Disposable d) {
                sd.update(d);
            }

        }
    }
}
