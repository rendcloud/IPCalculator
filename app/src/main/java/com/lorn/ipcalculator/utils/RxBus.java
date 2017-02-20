package com.lorn.ipcalculator.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import rx.Observable;
import rx.Subscriber;
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
    private final Map<Class<?>, Object> mStickyEventMap;


    private RxBus() {
        // publishSubject 只会把订阅之后的来自原始 Observable 的数据发送给观察者
        bus = new SerializedSubject<>(PublishSubject.create());
        mStickyEventMap = new ConcurrentHashMap<>();
    }

    public static RxBus getDefault() {
        if (defaultInstance == null) {
            synchronized (RxBus.class) {
                if (defaultInstance == null) {
                    defaultInstance = new RxBus();
                }
            }
        }
        return defaultInstance;
    }

    /**
     * 发送事件
     */
    public void post(Object event) {
        bus.onNext(event);
    }

    /**
     * 根据传递的 eventType 类型返回特定类型的被观察者
     *
     * @param eventType
     * @param <T>
     * @return
     */
    public <T> Observable<T> toObservable(Class<T> eventType) {
        return bus.ofType(eventType);

//        这里感谢小鄧子的提醒: ofType = filter + cast
//        return bus.filter(new Func1<Object, Boolean>() {
//            @Override
//            public Boolean call(Object o) {
//                return eventType.isInstance(o);
//            }
//        }) .cast(eventType);
    }

    /**
     * 判断是否有订阅者
     *
     * @return
     */
    public boolean hasObservers() {
        return bus.hasObservers();
    }


    /**
     * 发送一个新的 Sticky 事件
     *
     * @param event
     */
    public void postSticky(Object event) {
        synchronized (mStickyEventMap) {
            mStickyEventMap.put(event.getClass(), event);
        }
        post(event);
    }

    /**
     * 根据传递的 eventType 类型返回特定类型(eventType)的 被观察者
     */
    public <T> Observable<T> toObservableSticky(final Class<T> eventType) {
        synchronized (mStickyEventMap) {
            Observable<T> observable = bus.ofType(eventType);
            final Object event = mStickyEventMap.get(eventType);

            if (event != null) {
                return observable.mergeWith(Observable.create(new Observable.OnSubscribe<T>() {
                    @Override
                    public void call(Subscriber<? super T> subscriber) {
                        subscriber.onNext(eventType.cast(event));
                    }
                }));
            } else {
                return observable;
            }
        }
    }

    public <T> T getStickyEvent(Class<T> eventTypt) {
        synchronized (mStickyEventMap) {
            return eventTypt.cast(mStickyEventMap.get(eventTypt));
        }
    }

    public <T> T removeStickyEvent(Class<T> eventType) {
        synchronized (mStickyEventMap) {
            return eventType.cast(mStickyEventMap.remove(eventType));
        }
    }

    public void removeAllStickyEvents() {
        synchronized (mStickyEventMap) {
            mStickyEventMap.clear();
        }
    }
}
