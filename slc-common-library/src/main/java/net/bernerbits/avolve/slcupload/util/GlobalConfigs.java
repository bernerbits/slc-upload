package net.bernerbits.avolve.slcupload.util;

import java.util.concurrent.ThreadFactory;

public class GlobalConfigs {
	public static volatile ThreadFactory threadFactory = new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r);
		}
	};
}
