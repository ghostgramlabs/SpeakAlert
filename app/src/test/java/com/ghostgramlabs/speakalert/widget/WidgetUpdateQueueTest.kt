package com.ghostgramlabs.speakalert.widget

import android.content.Context
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WidgetUpdateQueueTest {
    private val context = mock<Context>()

    @Test fun `blocked widget service does not block callers or accumulate refreshes`() {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
            val scope = CoroutineScope(SupervisorJob() + dispatcher)
            val entered = CountDownLatch(1)
            val unblock = CountDownLatch(1)
            val refreshedAgain = CountDownLatch(1)
            val calls = java.util.concurrent.atomic.AtomicInteger()
            val caller = Thread.currentThread()
            val worker = java.util.concurrent.atomic.AtomicReference<Thread>()
            val queue = WidgetUpdateQueue(scope, {
                worker.set(Thread.currentThread())
                if (calls.incrementAndGet() == 1) {
                    entered.countDown()
                    unblock.await(5, TimeUnit.SECONDS)
                } else refreshedAgain.countDown()
            }, { throw AssertionError(it) })
            try {
                queue.request(context)
                assertTrue(entered.await(2, TimeUnit.SECONDS))
                assertNotSame(caller, worker.get())
                repeat(100) { queue.request(context) }
                assertEquals(1, calls.get())
                unblock.countDown()
                assertTrue(refreshedAgain.await(2, TimeUnit.SECONDS))
                assertEquals(2, calls.get())
            } finally {
                unblock.countDown()
                scope.cancel()
            }
        }
    }

    @Test fun `bursts are deferred and coalesced before work starts`() = runTest {
        var calls = 0
        val queue = WidgetUpdateQueue(backgroundScope, { calls++ }, { throw AssertionError(it) })
        repeat(100) { queue.request(context) }
        assertEquals(0, calls)
        runCurrent()
        assertEquals(1, calls)
        queue.request(context)
        runCurrent()
        assertEquals(2, calls)
    }

    @Test fun `service failure does not prevent later refreshes`() = runTest {
        var calls = 0
        val errors = mutableListOf<Exception>()
        val queue = WidgetUpdateQueue(backgroundScope, {
            if (++calls == 1) throw IllegalStateException("Widget service unavailable")
        }, { errors.add(it) })
        queue.request(context)
        runCurrent()
        queue.request(context)
        runCurrent()
        assertEquals(2, calls)
        assertEquals(1, errors.size)
    }
}
