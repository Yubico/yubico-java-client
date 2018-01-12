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

	Written by Simon Buckle (simon@webteq.eu), September 2011.
*/

import com.yubico.client.v2.exceptions.YubicoVerificationException;
import com.yubico.client.v2.impl.VerificationResponseImpl;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.yubico.client.v2.ResponseStatus.REPLAYED_REQUEST;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Fires off a number of validation requests to each specified URL 
 * in parallel.
 * 
 * @author Simon Buckle (simon@webteq.eu)
 */
public class VerificationRequester {
	private final ExecutorCompletionService<VerificationResponse> completionService;
	
	/**
	 * Sets up thread pool for validation requests.
	 */
	public VerificationRequester() {
		ThreadPoolExecutor pool = new ThreadPoolExecutor(0, 100, 250L,
				MILLISECONDS, new SynchronousQueue<Runnable>());
	    completionService = new ExecutorCompletionService<VerificationResponse>(pool);
	}

	/**
	 * Fires off a validation request to each url in the list, returning the first one
	 * that is not {@link ResponseStatus#REPLAYED_REQUEST}
	 * 
	 * @param urls a list of validation urls to be contacted
	 * @param userAgent userAgent to send in request, if null one will be generated
	 * @return {@link VerificationResponse} object from the first server response that is not
	 * {@link ResponseStatus#REPLAYED_REQUEST}
	 * @throws com.yubico.client.v2.exceptions.YubicoVerificationException if validation fails on all urls
	 */
	public VerificationResponse fetch(List<String> urls, String userAgent) throws YubicoVerificationException {
	    List<Future<VerificationResponse>> tasks = new ArrayList<Future<VerificationResponse>>();
	    for(String url : urls) {
			tasks.add(completionService.submit(createTask(userAgent, url)));
	    }
	    VerificationResponse response = null;
		try {
			int tasksDone = 0;
			Throwable savedException = null;
			Future<VerificationResponse> futureResponse = completionService.poll(1L, MINUTES);
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
					if(!response.getStatus().equals(REPLAYED_REQUEST)) {
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
				futureResponse = completionService.poll(1L, MINUTES);
			}
			if(futureResponse == null || response == null) {
				if(savedException != null) {
					throw new YubicoVerificationException(
							"Exception while executing validation.", savedException);
				} else {
					throw new YubicoVerificationException("Validation timeout.");
				}
			}
		} catch (InterruptedException e) {
			throw new YubicoVerificationException("Validation interrupted.", e);
		}
	    
		for(Future<VerificationResponse> task : tasks) {
			task.cancel(true);
		}
		
	    return response;
	}

	protected VerifyTask createTask(String userAgent, String url) {
		return new VerifyTask(url, userAgent);
	}

	/**
	 * Inner class for doing requests to validation server.
	 */
	static class VerifyTask implements Callable<VerificationResponse> {

		private final Logger log = LoggerFactory.getLogger(VerifyTask.class);

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
		public VerificationResponse call() throws Exception {
			URL url = new URL(this.url);
			try {
				return new VerificationResponseImpl(getResponseStream(url));
			} catch (IOException e) {
				log.warn("Exception when requesting {}: {}", url.getHost(), e.getMessage());
				throw e;
			}
		}

		protected InputStream getResponseStream(URL url) throws IOException {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("User-Agent", userAgent);
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(15000);
			return conn.getInputStream();
		}
	}
}
