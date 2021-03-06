/*
 *
 * Copyright (c) 2000, 2002, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * -Redistributions of source code must retain the above copyright
 * notice, this  list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduct the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Oracle nor the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
 * DAMAGES OR LIABILITIES  SUFFERED BY LICENSEE AS A RESULT OF  OR
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that Software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */

package sample.module;

import java.util.*;
import java.io.IOException;
import javax.security.auth.*;
import javax.security.auth.callback.*;
import javax.security.auth.login.*;
import javax.security.auth.spi.*;
import sample.principal.SamplePrincipal;
// these new imports allow me to read from files 
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.security.*;
import java.io.FileWriter;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * <p> This sample LoginModule authenticates users with a password.
 *
 * <p> This LoginModule only recognizes one user:       testUser
 * <p> testUser's password is:  testPassword
 *
 * <p> If testUser successfully authenticates itself,
 * a <code>SamplePrincipal</code> with the testUser's user name
 * is added to the Subject.
 *
 * <p> This LoginModule recognizes the debug option.
 * If set to true in the login Configuration,
 * debug messages will be output to the output stream, System.out.
 *
 */
public class SampleLoginModule implements LoginModule {

    // initial state
    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map sharedState;
    private Map options;

    // database & message digest
    private HashMap<String, String> db;
    private HashMap<String, String> subscription;
    private MessageDigest md;
    private File file = new File ( "sample/db/data.txt" );
    private File filetemp = new File( "sample/db/tmp.txt" );
    private SecretKey key;
    private String checkSum;

    // configurable option
    private boolean debug = false;

    // the authentication status
    private boolean succeeded = false;
    private boolean commitSucceeded = false;

    // username and password
    private String username;
    private char[] password;

    // testUser's SamplePrincipal
    private SamplePrincipal userPrincipal;
    private SamplePrincipal basicPrincipal;
    private SamplePrincipal silverPrincipal;
    private SamplePrincipal goldPrincipal;

    /**
     * Initialize this <code>LoginModule</code>.
     *
     * <p>
     *
     * @param subject the <code>Subject</code> to be authenticated. <p>
     *
     * @param callbackHandler a <code>CallbackHandler</code> for communicating
     *                  with the end user (prompting for user names and
     *                  passwords, for example). <p>
     *
     * @param sharedState shared <code>LoginModule</code> state. <p>
     *
     * @param options options specified in the login
     *                  <code>Configuration</code> for this particular
     *                  <code>LoginModule</code>.
     */
    public void initialize(Subject subject,
                   CallbackHandler callbackHandler,
                         Map<java.lang.String, ?> sharedState,
                         Map<java.lang.String, ?> options){

        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
	this.db = new HashMap<String, String>();
	this.subscription = new HashMap<String, String>();
	this.key = generateKey();
	// read database and load it into an HashMap object
	try{
	    md = MessageDigest.getInstance("SHA-1");
	    BufferedReader br = new BufferedReader( new FileReader("sample/db/data.txt") );
	    String line;
	    while( (line = br.readLine()) != null ){
		String[] data = line.split(" ");
		db.put(data[0], new String( data[1].getBytes(), "utf8" ));
	    }
	    this.checkSum = db.remove("checksum");
	    br.close();
	    if( !checkIntegrity( "Initializing" ) ){
		throw new LoginException( "Password file may be corrupted ... Stopping" );
	    }
	    this.loadSubscriptions();
	}catch( Exception e ){
	    System.out.println( e.toString() );
	    System.exit(1);
	}
	// initialize any configured options
	debug = "true".equalsIgnoreCase((String)options.get("debug"));
    }

    private void loadSubscriptions(){
	try{
	    BufferedReader br = new BufferedReader( new FileReader("sample/db/subscriptions.txt") );
	    String line;
	    while( (line = br.readLine()) != null ){
		String[] data = line.split(" ");
		this.subscription.put(data[0], data[1]);
	    }
	}catch( Exception e ){
	    System.out.println( e.toString() );
	    System.exit(1);
	}
    }
    
    private byte[] getDigest(){
	try{
	    BufferedWriter bw = new BufferedWriter( new FileWriter(filetemp.getAbsolutePath()));
	    BufferedReader br = new BufferedReader( new FileReader(file.getAbsolutePath()) );
	    String line;
	    while( (line = br.readLine()) != null ){
		String[] data = line.split(" ");
		if( !data[0].equals("checksum") ){
		    bw.write(line+"\n");
		}
	    }
	    br.close();
	    bw.close();
	    MessageDigest digestor = MessageDigest.getInstance("MD5");
	    File f = new File(filetemp.getAbsolutePath());
	    FileInputStream fis = new FileInputStream(f);
	    byte[] buffer = new byte[(int)f.length()];
	    fis.read(buffer);
	    fis.close();
	    return digestor.digest(buffer);
	}catch( Exception e ){
	    System.out.println( e.toString() );
	    System.exit(1);
	}
	return new byte[0];
    }

    private boolean checkIntegrity( String msg ){
	System.out.println( msg + " - Checking file integrity ... " );
	try{
	    Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
	    c.init( Cipher.ENCRYPT_MODE, this.key );
	    byte[] digest = getDigest();
	    digest = c.doFinal(digest);
	    return (new String(digest, "utf8")).equals(this.checkSum);
	}catch( Exception e ){
	    System.out.println( e.toString() );
	    System.exit(1);
	}
	return false;
    }

    private SecretKey generateKey(){
	String keystring = "oHY3XjuN8XuG+gOxvSL4ww==";
	byte[] encodedKey     = Base64.decode(keystring);
	return new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
    }


    /**
     * <p> This methods checks mathching of the true and given passwords
     *
     * @return true if the two passwords match in length and value terms
     * false otherwise.</p>
     */
    private boolean checkpw( char[] pwTrue ){
	byte[] real = ( new String( pwTrue ) ).getBytes();
	byte[] to_test = ( new String( password ) ).getBytes();
	if( real.length != to_test.length )
	    return false;
	for( int i = 0; i < real.length; i++){
	    if( real[i] != to_test[i] )
		return false;
	}
	return true;
    }


    /**
     * Authenticate the user by prompting for a user name and password.
     *
     * <p>
     *
     * @return true in all cases since this <code>LoginModule</code>
     *          should not be ignored.
     *
     * @exception FailedLoginException if the authentication fails. <p>
     *
     * @exception LoginException if this <code>LoginModule</code>
     *          is unable to perform the authentication.
     */
    public boolean login() throws LoginException {
	// at every login try check the integrity of the file containing passwords
	// because it could be modified or corrupted in the meantime
	if( !checkIntegrity( "Login" ) ){
	    throw new LoginException( "Corrputed passwords file" );
	}
        // prompt for a user name and password
        if (callbackHandler == null)
            throw new LoginException("Error: no CallbackHandler available " +
                        "to garner authentication information from the user");

        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("user name: ");
        callbacks[1] = new PasswordCallback("password: ", false);

        try {
            callbackHandler.handle(callbacks);
            username = ((NameCallback)callbacks[0]).getName();
            char[] tmpPassword = ((PasswordCallback)callbacks[1]).getPassword();
            if (tmpPassword == null) {
                // treat a NULL password as an empty password
                tmpPassword = new char[0];
            }
            password = new char[tmpPassword.length];
            System.arraycopy(tmpPassword, 0,
                        password, 0, tmpPassword.length);
            ((PasswordCallback)callbacks[1]).clearPassword();

        } catch (java.io.IOException ioe) {
            throw new LoginException(ioe.toString());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException("Error: " + uce.getCallback().toString() +
                " not available to garner authentication information " +
                "from the user");
        }

        // print debugging information
        if (debug) {
            System.out.println("\t\t[SampleLoginModule] " +
                                "user entered user name: " +
                                username);
            System.out.print("\t\t[SampleLoginModule] " +
                                "user entered password: ");
            for (int i = 0; i < password.length; i++)
                System.out.print(password[i]);
            System.out.println();
        }

        // verify the username/password
        boolean usernameCorrect = false;
        boolean passwordCorrect = false;
	String pwTrue = new String();
        if ( db.get(username) != null ){
            usernameCorrect = true;
	    pwTrue = db.get(username);
	    try{
		byte[] bytes = md.digest( (new String(password)).getBytes() );
		String temp = new String( bytes, "utf8" );
		password = temp.toCharArray();
	    }catch( Exception e ){
		System.out.println( e.toString() );
		System.exit(1);
	    }
	}
        if ( usernameCorrect && checkpw(pwTrue.toCharArray()) ) {
            // authentication succeeded!!!
            passwordCorrect = true;
            if (debug)
                System.out.println("\t\t[SampleLoginModule] " +
                                "authentication succeeded");
            succeeded = true;
            return true;
        } else {

            // authentication failed -- clean out state
            if (debug)
                System.out.println("\t\t[SampleLoginModule] " +
                                "authentication failed");
            succeeded = false;
            username = null;
            for (int i = 0; i < password.length; i++)
                password[i] = ' ';
            password = null;
            if (!usernameCorrect) {
                throw new FailedLoginException("User Name Incorrect");
            } else {
                throw new FailedLoginException("Password Incorrect");
            }
        }
    }

    /**
     * <p> This method is called if the LoginContext's
     * overall authentication succeeded
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules
     * succeeded).
     *
     * <p> If this LoginModule's own authentication attempt
     * succeeded (checked by retrieving the private state saved by the
     * <code>login</code> method), then this method associates a
     * <code>SamplePrincipal</code>
     * with the <code>Subject</code> located in the
     * <code>LoginModule</code>.  If this LoginModule's own
     * authentication attempted failed, then this method removes
     * any state that was originally saved.
     *
     * <p>
     *
     * @exception LoginException if the commit fails.
     *
     * @return true if this LoginModule's own login and commit
     *          attempts succeeded, or false otherwise.
     */
    public boolean commit() throws LoginException {
        if (succeeded == false) {
            return false;
        } else {
            // add a Principal (authenticated identity)
            // to the Subject

            // assume the user we authenticated is the SamplePrincipal
	    switch( this.subscription.get(username) ){
	    case "Basic":
		addPrincipals("Basic");
		break;
	    case "Silver":
		addPrincipals("Basic Silver");
		break;
	    case "Gold":
		addPrincipals("Basic Silver Gold");
		break;
	    case "Admin":
		addPrincipals("Basic Silver Gold Admin");
		break;
	    }

            if (debug) {
                System.out.println("\t\t[SampleLoginModule] " +
                                "added SamplePrincipal to Subject");
            }

            // in any case, clean out state
            username = null;
            for (int i = 0; i < password.length; i++)
                password[i] = ' ';
            password = null;

            commitSucceeded = true;
            return true;
        }
    }


    private void addPrincipals( String principals ){
	String princ[] = principals.split(" ");
	for( int i = 0; i < princ.length; i++ ){
	    userPrincipal = new SamplePrincipal(princ[i]);
	    if (!subject.getPrincipals().contains(userPrincipal))
                subject.getPrincipals().add(userPrincipal);
	}
    }

    /**
     * <p> This method is called if the LoginContext's
     * overall authentication failed.
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules
     * did not succeed).
     *
     * <p> If this LoginModule's own authentication attempt
     * succeeded (checked by retrieving the private state saved by the
     * <code>login</code> and <code>commit</code> methods),
     * then this method cleans up any state that was originally saved.
     *
     * <p>
     *
     * @exception LoginException if the abort fails.
     *
     * @return false if this LoginModule's own login and/or commit attempts
     *          failed, and true otherwise.
     */
    public boolean abort() throws LoginException {
        if (succeeded == false) {
            return false;
        } else if (succeeded == true && commitSucceeded == false) {
            // login succeeded but overall authentication failed
            succeeded = false;
            username = null;
            if (password != null) {
                for (int i = 0; i < password.length; i++)
                    password[i] = ' ';
                password = null;
            }
            userPrincipal = null;
        } else {
            // overall authentication succeeded and commit succeeded,
            // but someone else's commit failed
            logout();
        }
        return true;
    }

    /**
     * Logout the user.
     *
     * <p> This method removes the <code>SamplePrincipal</code>
     * that was added by the <code>commit</code> method.
     *
     * <p>
     *
     * @exception LoginException if the logout fails.
     *
     * @return true in all cases since this <code>LoginModule</code>
     *          should not be ignored.
     */
    public boolean logout() throws LoginException {

        subject.getPrincipals().remove(userPrincipal);
        succeeded = false;
        succeeded = commitSucceeded;
        username = null;
        if (password != null) {
            for (int i = 0; i < password.length; i++)
                password[i] = ' ';
            password = null;
        }
        userPrincipal = null;
        return true;
    }
}
