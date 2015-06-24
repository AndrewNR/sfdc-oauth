package org.andrewnr.oauth.model;

import java.util.HashMap;
import java.util.Map;

public class CredentialsStorage {
	
	public static final String KEY_OAUTH_CREDENTIALS = "OAuthCredentials";

	private static CredentialsStorage instance;
	private static final Map<String, AccessCredentials> storage = new HashMap<String, AccessCredentials>();
	
	private CredentialsStorage() {
	}
	
	public static CredentialsStorage getInstance() {
		if (instance == null) {
			instance = new CredentialsStorage();
		}
		return instance;
	}
	
	public AccessCredentials getCredentials(String key) {
		return storage.get(key);
	}
	
	public void putCredentials(String key, AccessCredentials credentials) {
		storage.put(key, credentials);
	}
}
