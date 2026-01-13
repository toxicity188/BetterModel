package kr.toxicity.model

import kr.toxicity.model.api.BetterModelEventBus
import kr.toxicity.model.api.event.CancellableEvent
import kr.toxicity.model.api.event.ModelEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
import java.util.function.Supplier

class BetterModelEventBusImpl : BetterModelEventBus {

    private val subscribers = ConcurrentHashMap<Class<*>, MutableList<Consumer<*>>>()

    override fun <T : ModelEvent> subscribe(eventClass: Class<T>, consumer: Consumer<T>) {
        subscribers.computeIfAbsent(eventClass) { CopyOnWriteArrayList() }
            .add(consumer)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ModelEvent> call(eventClass: Class<out T>, eventSupplier: Supplier<T>): BetterModelEventBus.Result {
        return subscribers[eventClass]?.let { list ->
            val event = eventSupplier.get()
            list.forEach { consumer ->
                (consumer as Consumer<ModelEvent>).accept(event)
            }
            if (event !is CancellableEvent || !event.isCancelled()) BetterModelEventBus.Result.SUCCESS else BetterModelEventBus.Result.FAIL
        } ?: BetterModelEventBus.Result.NO_EVENT_HANDLER
    }
}
