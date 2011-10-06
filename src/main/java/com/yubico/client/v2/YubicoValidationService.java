package com.yubico.client.v2;

/* 	Copyright (c) 2011, Simon Buckle.  All rights reserved.

	Redistribution and use in source and binary forms, with or without
	modification, are permitted provided that the following conditions
	are met:

	* Redistributions of source code must retain the above copyright
	notice, this list of conditions and the following disclaimer.

	* Redistributions in binary form must reproduce the above copyright
	notice, this list of conditions and the following
	disclaimer in the documentation and/or other materials provided
	with the distribution.

	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
	CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
	BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
	EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
	TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
	DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
	ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
	TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
	THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
	SUCH DAMAGE.

	Written by Simon Buckle <simon@webteq.eu>, September 2011.
*/
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;

import com.yubico.client.v2.impl.YubicoResponseImpl;

/*
 * Fires off a number of validation requests to each specified URL 
 * in parallel.
 * 
 * @author Simon Buckle <simon@webteq.eu>
 */
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
