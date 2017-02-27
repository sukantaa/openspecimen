package com.krishagni.catissueplus.core.common;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;

public class TransactionAwareInterceptor extends EmptyInterceptor implements Interceptor {
	private ThreadLocal<Integer> activeTxns = new ThreadLocal<Integer>() {
		@Override
		protected Integer initialValue() {
			return 0;
		}
	};

	@Override
	public void afterTransactionBegin(Transaction tx) {
		activeTxns.set(activeTxns.get() + 1);
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		int txns = activeTxns.get();
		if (txns <= 1) {
			TransactionalThreadLocals.getInstance().cleanup();
			activeTxns.remove();
		} else {
			activeTxns.set(txns - 1);
		}
	}
}
