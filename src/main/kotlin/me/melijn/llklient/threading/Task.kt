package me.melijn.llklient.threading

import kotlinx.coroutines.runBlocking


class RunnableTask(private val func: suspend () -> Unit) : Runnable {

    override fun run() {
        runBlocking {
            try {
                func()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}


@FunctionalInterface
interface KTRunnable {
    suspend fun run()
}