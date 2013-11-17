/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package readfile;


import com.sun.crypto.provider.AESCipher;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author alex
 */
public class ReadFile {
    
    /**
     * @param args the command line arguments
     */
    
    private static void write_to_file( byte[] data, byte[] test_digest ) throws FileNotFoundException, IOException{
        FileOutputStream fos = new FileOutputStream("/Users/alex/dtu/data-security/authentication_lab/sample/db/data.txt", true);
        fos.write(data);
        
        fos.close();
        BufferedReader br = new BufferedReader( new FileReader("/Users/alex/dtu/data-security/authentication_lab/sample/db/test.txt") );
        String line = br.readLine();
        byte[] b = line.getBytes("utf-8");
        for( int i = 0; i<test_digest.length; i++ ){
            System.out.println( b[i]+"  "+test_digest[i] );
        }
    }
    
    private static byte[] get_digest( String filename ) throws NoSuchAlgorithmException, FileNotFoundException, IOException{
        MessageDigest md = MessageDigest.getInstance("MD5");
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[(int)file.length()];
        fis.read(buffer);
        fis.close();
        return md.digest(buffer);
    }
    
    private static byte[] generateCheckSum( SecretKey key, byte[] digest, Cipher c ) throws FileNotFoundException, IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        
        c.init(Cipher.ENCRYPT_MODE, key);
        return c.doFinal( digest );
    }
    
    private static boolean checkIntegrity( SecretKey key, byte[] test_digest, String real_check, Cipher c ) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, InvalidAlgorithmParameterException, FileNotFoundException, IOException{
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] result = c.doFinal(test_digest);
        //write_to_file( result, test_digest );
        System.out.println( new String( result, "UTF-8" ) );
        return (new String( result, "UTF-8" ).equals( real_check ));
    }
    
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException{
        KeyGenerator keygenerator = KeyGenerator.getInstance("AES");
        String filename = "/Users/alex/dtu/data-security/authentication_lab/sample/db/data.txt";
        String filetemp = "/Users/alex/dtu/data-security/authentication_lab/sample/db/data_tmp.txt";
        keygenerator.init(128);
        SecretKey aesKey = keygenerator.generateKey();
        //String keystring = Base64.encode(aesKey.getEncoded());
        String keystring = "oHY3XjuN8XuG+gOxvSL4ww==";
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        System.out.println(keystring);
        byte[] encodedKey     = Base64.decode(keystring);
        SecretKey originalKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
        //System.out.println(aesKey.equals(originalKey));
        HashMap<String, String> db = new HashMap<String,String>();
        BufferedReader br = new BufferedReader( new FileReader(filename) );
        BufferedWriter bw = new BufferedWriter( new FileWriter(filetemp));
        MessageDigest m = MessageDigest.getInstance("SHA-1");
        String line;
        while( (line = br.readLine()) != null ){
            String[] data = line.split(" ");
            db.put(data[0], new String( data[1].getBytes(), "UTF-8" ));
            if( !data[0].equals("checksum") ){
                bw.write(line+"\n");
            }
            else{
                System.out.println(db.get("checksum"));
            }
        }
        br.close();
        bw.close();
        System.out.println(db.get("exodus"));
        System.out.println( new String( m.digest("thrash".getBytes()) ));
        byte[] real_check;
        byte[] digest;
        try {
            real_check = generateCheckSum(originalKey, get_digest(filetemp), c);
            System.out.println((new String( real_check, "utf8" )));
            //bw = new BufferedWriter( new FileWriter("/Users/alex/dtu/data-security/authentication_lab/sample/db/test.txt") );
            //bw.write((new String( real_check, "utf8" )));
            //bw.close();
            digest = get_digest(filetemp);
            System.out.println( checkIntegrity( originalKey, digest, db.get("checksum"), c ) );
        } catch (Exception ex) {
            Logger.getLogger(ReadFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
