package org.springsource.ide.eclipse.boot.maven.analyzer.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Function;

public class FutureUtil {
	
	private static class ImmeditateApplyFuture<V> implements Future<V> {

		private Throwable error;
		private V value;

		public <A> ImmeditateApplyFuture(Future<A> input, Function<A, V> function) {
			try {
				value = function.apply(input.get());
			} catch (Throwable e) {
				error = e;
			}
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public V get() throws InterruptedException, ExecutionException {
			if (error!=null) {
				throw new ExecutionException(error);
			} else {
				return value;
			}
		}

		@Override
		public V get(long timeout, TimeUnit unit) throws InterruptedException,
				ExecutionException, TimeoutException {
			return get();
		}

	}

	private ExecutorService executor;

	public FutureUtil(ExecutorService executor) {
		this.executor = executor;
	}

	/**
	 * Apply a function to the result of a Future and return a other Future.
	 * Take care to make sure the returned future is already 'isDone' if the input
	 * future 'isDone'.
	 * <p>
	 * The assumption is that the function is 'fast' and its ok to apply it immediately
	 * in the current thread.
	 * <p>
	 * The future is not yet 'isDone' then we use the executor to schedule a operation
	 * that waits for its result
	 */
	public <A, B> Future<B> map(final Future<A> input, final Function<A, B> function) {
		if (input.isDone()) {
			return new ImmeditateApplyFuture<B>(input, function);
		} else {
			return executor.submit(new Callable<B>() {
				@Override
				public B call() throws Exception {
					return function.apply(input.get());
				}
			});
		}
	}
	
	

}
