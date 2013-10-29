/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cryptography;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.*;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import sun.misc.IOUtils;


/**
 *
 * @author alex
 */
public class Cryptography {

    static byte[] perform_signature(Signature signature, byte[] digest, PrivateKey priv) throws Exception{
        signature.initSign(priv);
        signature.update(digest);
        return signature.sign();
    }
    
    static boolean perform_verification( Signature signature, byte[] data, PublicKey pub, byte[] sign ) throws Exception{
        signature.initVerify(pub);
        signature.update(data);
        return signature.verify(sign);
    }
    
    static byte[] encrypt_data( Cipher ciph, SecretKey key, byte[] data ) throws Exception{
        byte[] encrypted = ciph.doFinal(data);
        return encrypted;
    }
    
    static byte[] decrypt_data( Cipher ciph, byte[] encrypted, SecretKey key ) throws Exception{
        byte[] recieved = ciph.doFinal(encrypted);
        return recieved;
    }
    
    static boolean check_integrity(byte[] data, byte[] decrypted){
        for( int i=0; i<data.length; i++ ){
            if ( data[i] != decrypted[i] ){
                return false;
            }
        }
        return true;
    }
    
    static byte[] encrypt_file( String filename, SecretKey key, Cipher ciph ) throws Exception {
        FileInputStream fis = new FileInputStream(filename);
        FileOutputStream fos = new FileOutputStream("/Users/alex/ciphertext.txt");
        byte[] buffer;
        ciph.init(Cipher.ENCRYPT_MODE,key);
        File file = new File(filename);
        buffer = new byte[(int)file.length()];
        fis.read(buffer);
        byte[] encrypted_data = encrypt_data( ciph, key, buffer );
        fos.write(encrypted_data);
        fis.close();
        fos.close();
        return buffer;
    }
    static byte[] decrypt_file(SecretKey key, Cipher ciph) throws Exception {
        FileInputStream fis = new FileInputStream("/Users/alex/ciphertext.txt");
        FileOutputStream fos = new FileOutputStream("/Users/alex/decrypted.txt");
        ciph.init(Cipher.DECRYPT_MODE, key);
        File file = new File("/Users/alex/ciphertext.txt");
        byte[] buffer = new byte[(int)file.length()];
        fis.read(buffer);
        
        byte[] decrypted_data = decrypt_data( ciph, buffer, key );
        fos.write(decrypted_data);
        fis.close();
        fos.close();
        return decrypted_data;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        KeyGenerator keygenerator = KeyGenerator.getInstance("AES");
        keygenerator.init(128);
        SecretKey aesKey = keygenerator.generateKey();
        Cipher ciph = Cipher.getInstance("AES/ECB/PKCS5Padding");
        
        Thread.sleep(200);
        Signature signature = Signature.getInstance("SHA1withRSA"); // Initializes a signature object to mode sign
        
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA"); // Initializes a keygen using DSA algorithm
        keygen.initialize(2048);
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG"); // Secure random number generator
        keygen.initialize(1024, random); // "feeds" the keygen with the random generator
        KeyPair pair = keygen.genKeyPair(); // generates a pair of keys
        
        PrivateKey priv = pair.getPrivate();
        PublicKey pub = pair.getPublic();
        
        MessageDigest md = MessageDigest.getInstance("MD5");        
        byte[] digest = md.digest(encrypt_file("/Users/alex/plaintext.txt", aesKey, ciph));
        byte[] sign = perform_signature(signature,digest,priv);
        byte[] dec = decrypt_file(aesKey, ciph);
        byte[] second_digest = md.digest(dec);
        System.out.println(perform_verification(signature,second_digest,pub,sign));
    }
}
