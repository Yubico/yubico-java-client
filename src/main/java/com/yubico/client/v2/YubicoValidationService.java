package com.yubico.client.v2;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;

import com.yubico.client.v2.impl.YubicoResponseImpl;

public class YubicoValidationService {

final ResponseLatch<YubicoResponse> result = new ResponseLatch<YubicoResponse>();
	
	public YubicoResponse fetch(List<String> urls) throws InterruptedException {
		for (int i = 0, len = urls.size(); i < len; i++) {
			Runnable task = new VerifyTask(urls.get(i));
			new Thread(task).start();
		}
		return result.getValue();
	}
	
	class VerifyTask implements Runnable {
		private final String uri;
		
		public VerifyTask(String uri) {
			this.uri = uri;
		}
		
		public void run() {
			try {
				URL url = new URL(uri);
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				conn.setConnectTimeout(15000); // 15 second timeout
				YubicoResponse resp = new YubicoResponseImpl(conn.getInputStream());
				// @see http://forum.yubico.com/viewtopic.php?f=3&t=701
				if (!YubicoResponseStatus.REPLAYED_REQUEST.equals(resp.getStatus())) {
					result.setValue(resp);
				}
			} catch (SocketTimeoutException e) {
				System.out.println("Connection timed out");
			} catch (IOException e) {
				System.out.println(e);
			}
		}
		
	}
}
