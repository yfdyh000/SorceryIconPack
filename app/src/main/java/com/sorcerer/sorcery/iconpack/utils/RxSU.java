package com.sorcerer.sorcery.iconpack.utils;

import java.util.Arrays;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;
import io.reactivex.Observable;
import timber.log.Timber;

/**
 * @description:
 * @author: Sorcerer
 * @date: 2017/1/31
 */

public class RxSU {
    private static RxSU sRxSU;

    public static RxSU getInstance() {
        if (sRxSU == null) {
            sRxSU = new RxSU();
        }
        return new RxSU();
    }

    public Observable<Boolean> su() {
        return Observable.just(0).map(integer -> Shell.SU.available());
    }

    public Observable<List<String>> runAll(final String... commands) {
        return su().map(aBoolean -> {
            if (aBoolean) {
                Timber.d("run: " + Arrays.toString(commands));
                return Shell.SU.run(commands);
            }
            throw new Exception("Failed");
//            return new ArrayList<>();
        });
    }

    public Observable<String> run(final String... commands) {
        return su().map(aBoolean -> {
            if (aBoolean) {
                Timber.d("run: " + Arrays.toString(commands));
                return Shell.SU.run(commands);
            }
            return null;
        }).flatMap(Observable::fromIterable);
    }
}
