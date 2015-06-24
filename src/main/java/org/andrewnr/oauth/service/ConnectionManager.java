package org.andrewnr.oauth.service;

import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;

import org.andrewnr.oauth.OauthSettings;
import org.andrewnr.oauth.SfdcCredentials;
import org.andrewnr.oauth.model.AccessCredentials;
import org.andrewnr.oauth.model.CredentialsStorage;
import org.andrewnr.oauth.utils.OauthHelperUtils;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.stdimpl.GCacheFactory;
import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

/**
 * Singleton to handle the web services api connection to salesforce
 * 
 * @author Jeff Douglas
 */
public class ConnectionManager {

	protected static final Logger log = Logger.getLogger(ConnectionManager.class.getName());
	private static final String KEY_AUTH_POINT = "authEndPoint";
	private static final String KEY_END_POINT = "serviceEndPoint";
	private static final String KEY_SESSION = "sessionId";
	private static ConnectionManager ref;
	private PartnerConnection connection;
	Cache cache = null;

	private ConnectionManager() {
	}

	public static ConnectionManager getConnectionManager() {
		if (ref == null)
			ref = new ConnectionManager();
		return ref;
	}

	// gets the current connection
	public PartnerConnection getConnection() {

		try {

			CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
			cache = cacheFactory.createCache(new HashMap<Object, Object>());

			/**
			 * See if a session exists in the cache and if so, create a new
			 * connection. Can't cache the request in app engine as the
			 * PartnerConnection is not serializable. Could extend but beyond
			 * the scope of this app.
			 */
			if (cache.containsKey(KEY_AUTH_POINT)) {
				String sessionId = (String) cache.get(KEY_SESSION);
				log.info("Connection via cached session.");
				ConnectorConfig config = buildConnectionConfigFromCache(sessionId);
				connection = new PartnerConnection(config);
				connection.setSessionHeader(sessionId);
			
			// if a session does not exist in the cache
			} else {
				// if the app has been authorized but no session. Request a new
				// session with tokens from db.
				if (hasBeenAuthorizeded()) {

					log.info("Authorized but no Salesforce session...");

					AccessCredentials record = getStoredAccessCredentials();

					OAuthAccessor accessor = new OAuthAccessor(
							new OAuthConsumer(OauthSettings.URL_CALLBACK,
									OauthSettings.CONSUMER_KEY,
									OauthSettings.CONSUMER_SECRET, null));
					accessor.accessToken = record.getAccessToken();
					accessor.tokenSecret = record.getAccessTokenSecret();
					log.info("accessToken set to=" + accessor.accessToken);
					log.info("tokenSecret set to=" + accessor.tokenSecret);

					String loginResponse = OauthHelperUtils.getNewSfdcSession(accessor, OauthSettings.URL_API_LOGIN);
					log.info("Returned session response=" + loginResponse);
					if (loginResponse.startsWith("<")) {
						OauthHelperUtils.XmlResponseHandler xmlHandler = null;
						try {
							xmlHandler = OauthHelperUtils.parseResponse(loginResponse);
						} catch (Exception e) {
							log.info("Error parsing response sent from Salesforce");
						}

						// cache the connection info
						String sessionId = xmlHandler.getSessionId();
						cacheSessionProps(OauthSettings.URL_AUTH_ENDPOINT,
								xmlHandler.getServerUrl(),
								sessionId);
						
						// create a new session
						ConnectorConfig config = buildConnectionConfigFromCache(sessionId);
						config.setConnectionTimeout(7200); // timeout for 2 hours
						connection = new PartnerConnection(config);
						connection.setSessionHeader(sessionId);
					}
				// convenience for developing locally
				} else {
					log.info("TESTING -- create connection for Salesforce with u/p");

					// use the hard coded u/p of the cache doesn't exist
					ConnectorConfig config = new ConnectorConfig();
					config.setUsername(SfdcCredentials.SFDC_USERNAME);
					config.setPassword(SfdcCredentials.SFDC_PASSWORD);
					connection = Connector.newConnection(config);
				}
			}

		} catch (CacheException e) {
			log.info("cache exception=" + e.getMessage());
		} catch (ConnectionException e) {
			log.info("connection exception=" + e.getMessage());
		}

		return connection;

	}

	private ConnectorConfig buildConnectionConfigFromCache(String sessionId) {
		ConnectorConfig config = new ConnectorConfig();
		String authEndpoint = (String) cache.get(KEY_AUTH_POINT);
		config.setAuthEndpoint(authEndpoint);
		String serviceEndPoint = (String) cache.get(KEY_END_POINT);
		config.setServiceEndpoint(serviceEndPoint);
		config.setSessionId(sessionId == null ? (String) cache.get(KEY_SESSION) : sessionId);
		config.setValidateSchema(true);
		return config;
	}

	// searches db to determine if app has been authrozied
	public Boolean hasBeenAuthorizeded() {
		return getStoredAccessCredentials() != null;
	}

	// saves the acces token and secret to the database
	public void saveTokens(String accessToken, String accessSecret) {
		// persist the access token and secret to bigtable
		AccessCredentials creds = new AccessCredentials();
		creds.setAccessToken(accessToken);
		creds.setAccessTokenSecret(accessSecret);
		storeAccessCredentials(creds);
	}

	private static AccessCredentials getStoredAccessCredentials() {
		// TODO: redo using CredentialsStorage
		return CredentialsStorage.getInstance().getCredentials(CredentialsStorage.KEY_OAUTH_CREDENTIALS);
	}

	private static void storeAccessCredentials(AccessCredentials creds) {
		CredentialsStorage.getInstance().putCredentials(CredentialsStorage.KEY_OAUTH_CREDENTIALS, creds);
	}

	// caches the auth endpoint, service endpoint and session id
	public void cacheSessionProps(String authEndpoint, String serviceEndpoint, String sessionId) {

		// use the partner api, not the enterprise
		serviceEndpoint = serviceEndpoint.replace("/c/", "/u/");

		log.info("Caching service end point=" + serviceEndpoint);
		log.info("Caching auth end point=" + authEndpoint);
		log.info("Caching session id=" + sessionId);

		Map<Object, Object> props = new HashMap<Object, Object>();
		props.put(GCacheFactory.EXPIRATION_DELTA, 5400); // cache for 90 minutes
		props.put(MemcacheService.SetPolicy.ADD_ONLY_IF_NOT_PRESENT, true);

		try {
			// cache the connection for appengine
			CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
			cache = cacheFactory.createCache(props);
			cache.put(KEY_AUTH_POINT, authEndpoint);
			cache.put(KEY_END_POINT, serviceEndpoint);
			cache.put(KEY_SESSION, sessionId);
		} catch (CacheException e) {
			log.info("cache exception=" + e.getMessage());
		}
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

}
