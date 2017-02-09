package com.lorn.ipcalculator.utils;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * @author Lornj
 * @time 2017/2/8
 * @des
 */
public class RxBus {

    private static volatile RxBus defaultInstance;

    private final Subject<Object, Object> bus;

    private RxBus() {
        // publishSubject 只会把订阅之后的来自原始 Observable 的数据发送给观察者
        bus = new SerializedSubject<>(PublishSubject.create());
    }

    public static RxBus getDefaultInstance(){
        if (defaultInstance == null){
            synchronized (RxBus.class){
                if (defaultInstance == null){
                    defaultInstance = new RxBus();
                }
            }
        }
        return defaultInstance;
    }

    public void post(Object object){
        bus.onNext(object);
    }

    public <T> Observable<T> toObservable(Class<T> eventType){
        return bus.ofType(eventType);

//        这里感谢小鄧子的提醒: ofType = filter + cast
//        return bus.filter(new Func1<Object, Boolean>() {
//            @Override
//            public Boolean call(Object o) {
//                return eventType.isInstance(o);
//            }
//        }) .cast(eventType);
    }
}
