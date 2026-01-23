/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model

import kr.toxicity.model.api.BetterModelEventBus
import kr.toxicity.model.api.event.CancellableEvent
import kr.toxicity.model.api.event.ModelEvent
import kr.toxicity.model.api.event.ModelEventApplication
import kr.toxicity.model.api.event.ModelEventListener
import kr.toxicity.model.api.util.lock.DuplexLock
import kr.toxicity.model.util.handleFailure
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.function.Supplier

class BetterModelEventBusImpl(
    private val externalCallback: (Class<out ModelEvent>, Supplier<out ModelEvent>) -> BetterModelEventBus.Result = { _, _ -> BetterModelEventBus.Result.NO_EVENT_HANDLER }
) : BetterModelEventBus {

    private val subscribers = ConcurrentHashMap<Class<*>, BusManager>()

    override fun <T : ModelEvent> subscribe(application: ModelEventApplication, eventClass: Class<T>, consumer: Consumer<T>): ModelEventListener {
        return subscribers.computeIfAbsent(eventClass) { BusManager(it) }
            .register(application, consumer)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ModelEvent> call(eventClass: Class<out T>, eventSupplier: Supplier<T>): BetterModelEventBus.Result {
        return subscribers[eventClass]?.let { manager ->
            eventSupplier
                .get()
                .also(manager)
                .also { externalCallback(eventClass) { it } }
                .let { event ->
                    if (event !is CancellableEvent || !event.isCancelled()) BetterModelEventBus.Result.SUCCESS else BetterModelEventBus.Result.FAIL
                }
        } ?: run {
            externalCallback(eventClass) { eventSupplier.get() }
        }
    }

    private inner class BusManager(
        private val clazz: Class<*>
    ) : (ModelEvent) -> Unit {
        private val map = ConcurrentHashMap<ModelEventApplication, ListenerRegistry>()

        fun <T : ModelEvent> register(application: ModelEventApplication, consumer: Consumer<T>): ModelEventListener {
            if (!application.isEnabled) return ModelEventListener.NONE
            return map.compute(application) { k, v -> v?.takeIf { k.isEnabled } ?: ListenerRegistry() }?.add(consumer) ?: ModelEventListener.NONE
        }

        override fun invoke(p1: ModelEvent) {
            synchronized(this) {
                map.entries.removeIf { (application, registry) ->
                    !application.isEnabled || runCatching {
                        registry(p1)
                    }.handleFailure {
                        "Unable to pass this event: ${clazz.simpleName}"
                    }.isFailure
                }
                if (map.isEmpty()) subscribers.remove(clazz, this)
            }
        }
    }

    private class ListenerRegistry : (ModelEvent) -> Unit {
        val map = IdentityHashMap<ModelEventListener, Consumer<*>>()
        val lock = DuplexLock()

        fun add(consumer: Consumer<*>): ModelEventListener = ListenerImpl(this, consumer)

        @Suppress("UNCHECKED_CAST")
        override fun invoke(p1: ModelEvent) {
            lock.accessToReadLock {
                map.values.forEach { consumer ->
                    (consumer as Consumer<ModelEvent>).accept(p1)
                }
            }
        }
    }

    private class ListenerImpl(
        registry: ListenerRegistry,
        consumer: Consumer<*>
    ) : ModelEventListener {

        private val ref = WeakReference(registry)

        init {
            registry.lock.accessToWriteLock {
                registry.map[this] = consumer
            }
        }

        override fun unregister() {
            ref.get()?.let {
                it.lock.accessToWriteLock { it.map.remove(this) }
            }
        }
    }
}
