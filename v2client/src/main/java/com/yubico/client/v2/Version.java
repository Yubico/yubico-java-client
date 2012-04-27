package com.yubico.client.v2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Version {
	private static String version;
	
	public static String version() {
		if(version == null) {
			Properties properties = new Properties();
			try {
				InputStream stream = Version.class.getResourceAsStream("/version.properties");
				if(stream == null) {
					return null;
				}
				properties.load(stream);
				version = properties.getProperty("version");
			} catch (IOException e) {
				return null;
			}
		}
		return version;
	}
}
