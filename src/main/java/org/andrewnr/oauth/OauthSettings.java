package org.andrewnr.oauth;

/**
 * Class containing static members/settings used by the application
 * 
 * @author Jeff Douglas
 */

public class OauthSettings {
  
  // Consumer settings copied from Salesforce.com Remote Access application
  public static String CONSUMER_KEY = "3MVG9A2kN3Bn17hszKpKA8xWHsPmGWw6gSsTwftwvAZUgrU8XqN3LjRdPwrAM5D2Mep_C9Q4LqH8Ux5BXpz3T";
  public static String CONSUMER_SECRET = "8542237700479489045";
  
  // URLs used by the application - change to test.salesforce.com for sandboxes
  public static String HOST = "https://login.salesforce.com";
  
  // URLs used by the application
  public static String URL_CALLBACK = "https://andrewnr-oauth.herokuapp.com/callback";
  public static String URL_API_LOGIN = HOST+"/services/OAuth/c/17.0";
  public static String URL_AUTHORIZATION = HOST+"/setup/secur/RemoteAccessAuthorizationPage.apexp";  
  public static String URL_AUTH_ENDPOINT = HOST+"/services/Soap/u/17.0";
  public static String URL_REQUEST_TOKEN = HOST+"/_nc_external/system/security/oauth/RequestTokenHandler";
  public static String URL_ACCESS_TOKEN = HOST+"/_nc_external/system/security/oauth/AccessTokenHandler";

}
