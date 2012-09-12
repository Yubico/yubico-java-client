package com.yubico.client.v2;

/* 	Copyright (c) 2011, Simon Buckle.  All rights reserved.
	Copyright (c) 2012, Yubico AB. All rights reserved.

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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.yubico.client.v2.exceptions.YubicoValidationException;
import com.yubico.client.v2.impl.YubicoResponseImpl;

/**
 * Fires off a number of validation requests to each specified URL 
 * in parallel.
 * 
 * @author Simon Buckle <simon@webteq.eu>
 */
public class YubicoValidationService {
	private ExecutorCompletionService<YubicoResponse> completionService;
	
	/**
	 * Sets up thread pool for validation requests.
	 */
	public YubicoValidationService() {
		ThreadPoolExecutor pool = new ThreadPoolExecutor(0, 100, 250L,
				TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());
	    completionService = new ExecutorCompletionService<YubicoResponse>(pool);
	}

	/**
	 * Fires off a validation request to each url in the list, returning the first one
	 * that is not {@link YubicoResponseStatus#REPLAYED_REQUEST}
	 * 
	 * @param urls a list of validation urls to be contacted
	 * @param userAgent userAgent to send in request, if null one will be generated
	 * @return {@link YubicoResponse} object from the first server response that's not
	 * {@link YubicoResponseStatus#REPLAYED_REQUEST}
	 * @throws YubicoValidationException if validation fails on all urls
	 */
	public YubicoResponse fetch(List<String> urls, String userAgent) throws YubicoValidationException {
	    List<Future<YubicoResponse>> tasks = new ArrayList<Future<YubicoResponse>>();
	    for(String url : urls) {
	    	tasks.add(completionService.submit(new VerifyTask(url, userAgent)));
	    }
	    YubicoResponse response = null;
		try {
			int tasksDone = 0;
			Throwable savedException = null;
			Future<YubicoResponse> futureResponse = completionService.poll(1L, TimeUnit.MINUTES);
			while(futureResponse != null) {
				try {
					tasksDone++;
					tasks.remove(futureResponse);
					response = futureResponse.get();
					/**
					 * If the response returned is REPLAYED_REQUEST keep looking at responses
					 * and hope we get something else. REPLAYED_REQUEST will be returned if a
					 * validation server got sync before it parsed our query (otp and nonce is
					 * the same).
					 * @see http://forum.yubico.com/viewtopic.php?f=3&t=701
					 */
					if(!response.getStatus().equals(YubicoResponseStatus.REPLAYED_REQUEST)) {
						break;
					}
				} catch (CancellationException ignored) {
					// this would be thrown by old cancelled calls.
					tasksDone--;
				} catch (ExecutionException e) {
					// tuck the real exception away and use it if we don't get any valid answers.
					savedException = e.getCause();
				}
				if(tasksDone >= urls.size()) {
					break;
				}
				futureResponse = completionService.poll(1L, TimeUnit.MINUTES);
			}
			if(futureResponse == null || response == null) {
				if(savedException != null) {
					throw new YubicoValidationException(
							"Exception while executing validation.", savedException);
				} else {
					throw new YubicoValidationException("Validation timeout.");
				}
			}
		} catch (InterruptedException e) {
			throw new YubicoValidationException("Validation interrupted.", e);
		}
	    
		for(Future<YubicoResponse> task : tasks) {
			task.cancel(true);
		}
		
	    return response;
	}
	
	/**
	 * Inner class for doing requests to validation server.
	 */
	class VerifyTask implements Callable<YubicoResponse> {
		private final String url;
		private final String userAgent;
		
		/**
		 * Set up a VerifyTask for the Yubico Validation protocol v2
		 * @param url the url to be used
		 * @param userAgent the userAgent to be sent to the server, or NULL and one is calculated
		 */
		public VerifyTask(String url, String userAgent) {
			this.url = url;
			this.userAgent = userAgent;
		}
		
		/**
		 * Do the validation query for previous URL.
		 * @throws Exception should not be anything but {@link IOException}
		 */
		public YubicoResponse call() throws Exception {
			URL url = new URL(this.url);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			if(userAgent == null) {
				conn.setRequestProperty("User-Agent", "yubico-java-client/" + Version.version());
			} else {
				conn.setRequestProperty("User-Agent", userAgent);
			}
			conn.setConnectTimeout(15000); // 15 second timeout
			conn.setReadTimeout(15000); // for both read and connect
			YubicoResponse resp = new YubicoResponseImpl(conn.getInputStream());
			return resp;
		}	
	}
}
